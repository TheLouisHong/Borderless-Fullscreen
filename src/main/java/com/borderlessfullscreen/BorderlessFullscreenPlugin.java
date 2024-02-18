
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
package com.borderlessfullscreen;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collection;
import javax.inject.Inject;
import javax.swing.JOptionPane;
import javax.swing.*;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.ContainableFrame;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
	name = "Borderless Fullscreen",
	description = "True Borderless Windowed Fullscreen",
	tags = {"fullscreen", "borderless", "windowed"}
)
@Slf4j
public class BorderlessFullscreenPlugin extends Plugin
{

	@Inject
	private BorderlessFullscreenHotkeyListener hotkeyListener;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ClientUI clientUI;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	private ContainableFrame clientFrame = null;

	private Rectangle prevClientFrameBounds = null;

	@Getter
	private boolean fullScreen = false;
	private NavigationButton enableFullscreenNavButton = null;
	private NavigationButton disableFullscreenNavButton = null;

	private final BufferedImage enable_icon = ImageUtil.loadImageResource(getClass(), "fullscreen.png");
	private final BufferedImage disable_icon = ImageUtil.loadImageResource(getClass(), "fullscreen_off.png");

	@Provides
	BorderlessFullscreenConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BorderlessFullscreenConfig.class);
	}

	@Override
	protected void startUp() throws Exception
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
			DisableFullScreen();
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
			.onClick(this::EnableFullScreen)
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
			.onClick(this::DisableFullScreen)
			.build();
		return disableFullscreenNavButton;
	}

	protected void EnableFullScreen()
	{
		Frame showError = Frame.getFrames()[0];

		if (configManager.getConfig(RuneLiteConfig.class).enableCustomChrome())
		{
			log.info("You must disable custom chrome to enable fullscreen");
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(showError,
					"Please uncheck 'Enable custom window chrome' via the 'Runelite' plugin.",
					"Disable Custom Chrome",
					JOptionPane.ERROR_MESSAGE));
			return;
		}

		if (fullScreen)
		{
			log.error("Tried to enable fullscreen, but already in fullscreen mode.");
			return;

		}

		// Steal clientUI ContainableFrame through java.awt
		Frame[] frames = Frame.getFrames();
		clientFrame = null;
		for (Frame frame : frames)
		{
			if (frame instanceof ContainableFrame)
			{
				clientFrame = (ContainableFrame) frame;
			}
		}

		if (clientFrame == null)
		{
			log.error("Could not find clientUI Containable frame. Very weird! This plugin must be way out-of-date.");
			return;
		}

		// The two plugins that rely on the client frame being displayable are 117HD plugin and GPU plugin

		// We need to pause them before we can dispose of the frame
		// We will start them again after we have packed the frame and made it visible again
		Plugin hdPlugin = stopPlugin("HdPlugin");
		Plugin gpuPlugin = stopPlugin("GpuPlugin");

		// get the screen insets
		Insets insets = clientFrame.getInsets();

		prevClientFrameBounds = clientFrame.getBounds();

		// get the screen bounds
		Rectangle fullscreenBounds = clientFrame.getBounds();
		// remove the insets from the bounds
		fullscreenBounds = new Rectangle(
			fullscreenBounds.x + insets.left,
			fullscreenBounds.y + insets.top,
			fullscreenBounds.width - (insets.left + insets.right),
			fullscreenBounds.height - (insets.top + insets.bottom)
		);

		// find the display that the client bounds are on
		GraphicsConfiguration gc = findDisplayFromBounds(fullscreenBounds);

		// dispose of the frame if it's already visible
		// this is non-destructive to the frame, it can be re-shown later
		if (clientFrame.isDisplayable())
		{
			clientFrame.dispose();
		}

		// set the frame state to maximized
		clientFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
		// remove the window decorations
		clientFrame.setUndecorated(true);

		// set the frame to be always on top
		clientFrame.setAlwaysOnTop(true);

		// set the frame to be full screen
		clientFrame.setSize(gc.getBounds().getSize());
		// set the frame location to the display location
		clientFrame.setLocation(gc.getBounds().getLocation());

		// pack the frame
		clientFrame.pack();

		// make the frame visible
		clientFrame.setVisible(true);

		// start the plugins again
		if (hdPlugin != null)
		{
			startPlugin(hdPlugin);
		}
		if (gpuPlugin != null)
		{
			startPlugin(gpuPlugin);
		}

		fullScreen = true;

		clientToolbar.removeNavigation(enableFullscreenNavButton);
		clientToolbar.addNavigation(buildDisableButton());

		clientUI.requestFocus();
	}

	protected void DisableFullScreen()
	{
		if (!fullScreen)
		{
			log.error("Tried to disable fullscreen, but already in windowed mode.");
			return;
		}
		if (clientFrame == null)
		{
			log.error("Tried to disable fullscreen, but clientFrame is null. This should never happen.");
			return;
		}

		// The two plugins that rely on the client frame being displayable are 117HD plugin and GPU plugin
		// We need to pause them before we can dispose of the frame
		// We will start them again after we have packed the frame and made it visible again
		Plugin HdPlugin = stopPlugin("HdPlugin");
		Plugin GpuPlugin = stopPlugin("GpuPlugin");

		clientFrame.dispose();
		clientFrame.setExtendedState(Frame.NORMAL);
		clientFrame.setUndecorated(false);
		clientFrame.setAlwaysOnTop(false);


		clientFrame.pack();
		clientFrame.setVisible(true);

		setClientFrameSize(prevClientFrameBounds);

		if (HdPlugin != null)
		{
			startPlugin(HdPlugin);
		}
		if (GpuPlugin != null)
		{
			startPlugin(GpuPlugin);
		}

		fullScreen = false;

		clientToolbar.removeNavigation(disableFullscreenNavButton);
		clientToolbar.addNavigation(buildEnableButton());

		clientUI.requestFocus();

	}

	private void setClientFrameSize(Rectangle frameBounds)
	{
		clientFrame.setBounds(frameBounds);
		clientFrame.revalidate();
		clientFrame.pack();
	}

	private GraphicsConfiguration findDisplayFromBounds(final Rectangle bounds)
	{
		GraphicsDevice[] gds = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

		for (GraphicsDevice gd : gds)
		{
			GraphicsConfiguration gc = gd.getDefaultConfiguration();

			final Rectangle displayBounds = gc.getBounds();
			if (displayBounds.contains(new Point((int) bounds.getCenterX(), (int) bounds.getCenterY())))
			{
				return gc;
			}
		}

		// If we can't find the display, just return the default display
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
	}


	// Privileged access to other plugins through plugin manager
	private Plugin stopPlugin(String pluginClassName)
	{
		Plugin resultPlugin = null;
		Collection<Plugin> plugins = pluginManager.getPlugins();
		for (Plugin plugin : plugins)
		{
			if (plugin.getClass().getSimpleName().equals(pluginClassName))
			{
				if (pluginManager.isPluginEnabled(plugin))
				{
					pluginManager.setPluginEnabled(plugin, false);
					try
					{
						pluginManager.stopPlugin(plugin);
					}
					catch (PluginInstantiationException e)
					{
						throw new RuntimeException(e);
					}
					resultPlugin = plugin;
				}
			}
		}
		return resultPlugin;
	}

	// Privileged access to other plugins through plugin manager
	private void startPlugin(Plugin plugin)
	{
		pluginManager.setPluginEnabled(plugin, true);
		try
		{
			pluginManager.startPlugin(plugin);
		}
		catch (PluginInstantiationException e)
		{
			throw new RuntimeException(e);
		}
	}


}
