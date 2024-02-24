package com.fullscreen;

import com.fullscreen.implementations.FullscreenMode;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup("FullScreen")
public interface FullscreenConfig extends Config
{
	// fullscreen mode
	@ConfigItem(
		keyName = "FullscreenMode",
		name = "Fullscreen Mode",
		description = "Choose between borderless fullscreen and exclusive fullscreen",
		position = 0,
		section = "Fullscreen"
	)
	default FullscreenMode FullscreenMode()
	{
		return FullscreenMode.BORDERLESS;
	}

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
