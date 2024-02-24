
/* Based on the work of dekvall <https://github.com/dekvall/runelite-external-plugins/blob/fullscreen/src/main/java/dekvall/fullscreen/FullscreenPlugin.java> */
/*
 * Copyright (c) 2023, Louis Hong <https://github.com/TheLouisHong/Borderless-Fullscreen>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.fullscreen;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.JOptionPane;
import javax.swing.*;

import com.fullscreen.implementations.BorderlessFullscreen;
import com.fullscreen.implementations.ExclusiveFullscreen;
import com.fullscreen.implementations.Fullscreen;
import com.fullscreen.implementations.FullscreenMode;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
	name = "Fullscreen",
	description = "Fullscreen for RuneLite.",
	tags = {"fullscreen","borderless","windowed","quality of life","QOL,monitor","full","screen","all"}
)
@Slf4j
public class FullscreenPlugin extends Plugin
{

	@Inject
	private FullscreenHotkeyListener hotkeyListener;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private ClientUI clientUI;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private FullscreenConfig config;

	@Getter
	private boolean fullScreen = false;

	private Fullscreen currentFullscreen = null;

	private NavigationButton enableFullscreenNavButton = null;
	private NavigationButton disableFullscreenNavButton = null;


	private final BufferedImage enable_icon = ImageUtil.loadImageResource(getClass(), "fullscreen.png");
	private final BufferedImage disable_icon = ImageUtil.loadImageResource(getClass(), "fullscreen_off.png");

	@Provides
	FullscreenConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FullscreenConfig.class);
	}

	@Override
	protected void startUp()
	{
		clientToolbar.addNavigation(buildEnableButton());
		hotkeyListener.setEnabledOnLoginScreen(true);
		keyManager.registerKeyListener(hotkeyListener);

		log.info("Fullscreen started!");
	}

	@Override
	protected void shutDown()
	{
		if (enableFullscreenNavButton != null)
		{
			clientToolbar.removeNavigation(enableFullscreenNavButton);
		}
		if (disableFullscreenNavButton != null)
		{
			clientToolbar.removeNavigation(disableFullscreenNavButton);
		}
		keyManager.unregisterKeyListener(hotkeyListener);

		if (fullScreen)
		{
			disableFullScreen();
		}

		log.info("FullScreen stopped!");
	}

	// Java passes NavButtons by reference, and add/removeNavigation() consumes the reference
	// So we rebuild the button to update the icon
	private NavigationButton buildEnableButton()
	{
		enableFullscreenNavButton = NavigationButton.builder()
			.tooltip("Fullscreen")
			.icon(enable_icon)
			.priority(10)
			.onClick(this::enableFullScreen)
			.build();
		return enableFullscreenNavButton;
	}

	// Java passes NavButtons by reference, and add/removeNavigation() consumes the reference
	// So we rebuild the button to update the icon
	private NavigationButton buildDisableButton()
	{
		disableFullscreenNavButton = NavigationButton.builder()
			.tooltip("Windowed")
			.icon(disable_icon)
			.priority(10)
			.onClick(this::disableFullScreen)
			.build();
		return disableFullscreenNavButton;
	}

	protected void enableFullScreen()
	{
		Frame showError = Frame.getFrames()[0];

		if (configManager.getConfig(RuneLiteConfig.class).enableCustomChrome())
		{
			log.info("You must disable custom chrome to enable fullscreen");
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(showError,
					"Please uncheck 'Enable custom window chrome' via the 'Runelite' plugin. You must disable custom chrome to enable fullscreen.",
					"Borderless Fullscreen",
					JOptionPane.ERROR_MESSAGE));
			return;
		}

		if (fullScreen)
		{
			log.error("Tried to enable fullscreen, but already in fullscreen mode.");
			return;

		}

		switch (config.FullscreenMode())
		{
			case BORDERLESS:
				currentFullscreen = new BorderlessFullscreen(pluginManager, clientUI);
				break;
			case EXCLUSIVE:
				currentFullscreen = new ExclusiveFullscreen(clientUI);
				break;
		}

		try
		{
			currentFullscreen.enableFullscreen();
		}
		catch (Exception e)
		{
			log.error("Failed to enable fullscreen.", e);
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(showError,
				"Fullscreen could not be enabled due to:\n" + e,
				"Fullscreen",
				JOptionPane.ERROR_MESSAGE));
			return;
		}


		fullScreen = true;

		clientToolbar.removeNavigation(enableFullscreenNavButton);
		clientToolbar.addNavigation(buildDisableButton());

	}

	protected void disableFullScreen()
	{
		if (!fullScreen)
		{
			log.error("Tried to disable fullscreen, but already in windowed mode.");
			return;
		}

		try
		{
			currentFullscreen.disableFullscreen();
		}
		catch (Exception e)
		{
			log.error("Failed to disable fullscreen.", e);
			Frame showError = Frame.getFrames()[0];
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(showError,
				"Fullscreen could not be enabled due to:\n" + e.toString() + ":" + e.getCause(),
				"Fullscreen",
				JOptionPane.ERROR_MESSAGE));
			return;
		}

		currentFullscreen = null;
		fullScreen = false;

		clientToolbar.removeNavigation(disableFullscreenNavButton);
		clientToolbar.addNavigation(buildEnableButton());

	}


//	@Subscribe
//	public void onConfigChanged(ConfigChanged ev)
//	{
//		if (config.FullscreenMode() == FullscreenMode.EXCLUSIVE)
//		{
//			// check if operating system is running macos, if so, it is not supported.
//			if (System.getProperty("os.name").toLowerCase().contains("mac"))
//			{
//				log.info("Exclusive Fullscreen Mode is not supported on MacOS, please try switching to borderless in settings.");
//				Frame showError = Frame.getFrames()[0];
//				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(showError,
//					"Exclusive Fullscreen Mode is not supported on MacOS, please try switching to borderless in settings.",
//					"Fullscreen",
//					JOptionPane.ERROR_MESSAGE));
//				configManager.setConfiguration("Fullscreen", "FullscreenMode", FullscreenMode.BORDERLESS);
//			}
//		}
//	}
//
}
