package com.fullscreen.implementations;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.ContainableFrame;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.util.Collection;

@Slf4j
public class BorderlessFullscreen implements Fullscreen
{
	private final PluginManager pluginManager;
	private final ClientUI clientUI;

	private ContainableFrame clientFrame = null;
	private Rectangle prevClientFrameBounds = null;

	public BorderlessFullscreen(PluginManager pluginManager, ClientUI clientUI)
	{
		this.pluginManager = pluginManager;
		this.clientUI = clientUI;
	}


	@Override
	public void enableFullscreen()
	{

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

		clientUI.requestFocus();
	}

	public void disableFullscreen()
	{
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

		clientUI.requestFocus();

	}

	private void setClientFrameSize(Rectangle frameBounds)
	{
		clientFrame.setBounds(frameBounds);
		clientFrame.revalidate();
		clientFrame.pack();
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


}
