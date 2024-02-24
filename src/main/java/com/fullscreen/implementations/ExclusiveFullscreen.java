/*
 * Copyright (c) 2020, dekvall <https://github.com/dekvall>
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
package com.fullscreen.implementations;

import java.awt.Frame;
import java.awt.GraphicsDevice;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.ContainableFrame;

@Slf4j
public class ExclusiveFullscreen implements Fullscreen
{

	private final ClientUI clientUI;

	private GraphicsDevice gd;

	public ExclusiveFullscreen(ClientUI clientUI)
	{
		this.clientUI = clientUI;
	}


	public void enableFullscreen() throws RuntimeException
	{
		log.info("Fullscreen started!");
		gd = clientUI.getGraphicsConfiguration().getDevice();
		Frame tempParent = Frame.getFrames()[0];

		if (!gd.isFullScreenSupported())
		{
			log.info("Exclusive Fullscreen Mode is not supported on your device, please try switching to borderless in settings.");
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(tempParent,
				"Exclusive Fullscreen Mode is not supported on your device, please try switching to borderless in settings.",
				"Could not enter fullscreen mode",
				JOptionPane.ERROR_MESSAGE));
			return;
		}

		// throw exception with message on enable on mac
		if (System.getProperty("os.name").toLowerCase().contains("mac"))
		{
			throw new RuntimeException("Exclusive Fullscreen Mode is not supported on Mac OS, please try switching to borderless in settings.");
		}

		//Dirty hack
		Frame[] frames = Frame.getFrames();
		for (Frame frame : frames)
		{
			if (frame instanceof ContainableFrame)
			{
				gd.setFullScreenWindow(frame);
				return;
			}
		}
	}

	public void disableFullscreen()
	{
		gd.setFullScreenWindow(null);
		log.info("Fullscreen stopped!");
	}
}
