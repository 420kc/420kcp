package com.fourtwentykc;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("fourtwentykc")
public interface FourTwentyKcConfig extends Config
{
	@ConfigItem(
		keyName = "chatMessages",
		name = "Chat messages",
		description = "Show a message in chat on your 420th kill"
	)
	default boolean chatMessages()
	{
		return true;
	}
}
