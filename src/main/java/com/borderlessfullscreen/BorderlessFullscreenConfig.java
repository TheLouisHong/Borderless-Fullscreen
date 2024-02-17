package com.borderlessfullscreen;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup("BorderlessFullScreen")
public interface BorderlessFullscreenConfig extends Config
{

	@ConfigItem(
		keyName = "BorderlessFullscreenHotkey",
		name = "Fullscreen Hotkey",
		description = "Hotkey to toggle fullscreen mode",
		position = 1,
		section = "Hotkeys"
	)

	default Keybind FullScreenHotKey()
	{
		return Keybind.NOT_SET;
	}

}
