package com.borderlessfullscreen;

import net.runelite.client.util.HotkeyListener;

import javax.inject.Inject;

class BorderlessFullscreenHotkeyListener extends HotkeyListener
{
	private final BorderlessFullscreenPlugin plugin;
	private final BorderlessFullscreenConfig config;

	@Inject
	public BorderlessFullscreenHotkeyListener(BorderlessFullscreenPlugin plugin, BorderlessFullscreenConfig config)
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
			plugin.DisableFullScreen();
		} else
		{
			plugin.EnableFullScreen();
		}
	}
}
