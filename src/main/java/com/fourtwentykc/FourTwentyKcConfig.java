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
        description = "Show 'blaze it' in chat when you hit 420 kc on a boss"
    )
    default boolean chatMessages()
    {
        return true;
    }
}
