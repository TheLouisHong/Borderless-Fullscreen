package com.fullscreen;

import net.runelite.client.util.HotkeyListener;

import javax.inject.Inject;

class FullscreenHotkeyListener extends HotkeyListener
{
	private final FullscreenPlugin plugin;
	private final FullscreenConfig config;

	@Inject
	public FullscreenHotkeyListener(FullscreenPlugin plugin, FullscreenConfig config)
	{
		super(config::FullScreenHotKey);

		this.plugin = plugin;
		this.config = config;

	}

	@Override
	public void hotkeyReleased()
	{
		if (plugin.isFullScreen())
		{
			plugin.disableFullScreen();
		} else
		{
			plugin.enableFullScreen();
		}
	}
}
