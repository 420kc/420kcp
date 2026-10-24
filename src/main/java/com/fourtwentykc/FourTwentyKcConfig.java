package com.fourtwentykc;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup("fourtwentykc")
public interface FourTwentyKcConfig extends Config
{
	@ConfigSection(
		name = "Smoke",
		description = "Smoke rolls over the game screen",
		position = 2
	)
	String smokeSection = "smoke";

	@ConfigSection(
		name = "Confetti",
		description = "Green confetti falls over the game screen",
		position = 3
	)
	String confettiSection = "confetti";

	@ConfigItem(
		keyName = "chatMessages",
		name = "Chat messages",
		description = "Show a message in chat when a tracked count reaches 420",
		position = 0
	)
	default boolean chatMessages()
	{
		return true;
	}

	@ConfigItem(
		keyName = "popup",
		name = "Popup",
		description = "Show the game's collection log popup when a tracked count reaches 420",
		position = 1
	)
	default boolean popup()
	{
		return true;
	}

	@ConfigItem(
		keyName = "smoke",
		name = "Smoke",
		description = "Roll smoke over the game screen when a tracked count reaches 420. Test in chat with ::420",
		section = smokeSection,
		position = 0
	)
	default boolean smoke()
	{
		return true;
	}

	@Range(min = 1, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "smokeDensity",
		name = "Density",
		description = "How thick the smoke is",
		section = smokeSection,
		position = 1
	)
	default int smokeDensity()
	{
		return 50;
	}

	@Range(min = 1, max = 30)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "smokeDuration",
		name = "Duration",
		description = "How long the smoke lasts",
		section = smokeSection,
		position = 2
	)
	default int smokeDuration()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "confetti",
		name = "Confetti",
		description = "Drop green confetti over the game screen when a tracked count reaches 420. Test in chat with ::420",
		section = confettiSection,
		position = 0
	)
	default boolean confetti()
	{
		return true;
	}

	@Range(min = 1, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "confettiDensity",
		name = "Density",
		description = "How much confetti falls",
		section = confettiSection,
		position = 1
	)
	default int confettiDensity()
	{
		return 50;
	}

	@Range(min = 1, max = 30)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "confettiDuration",
		name = "Duration",
		description = "How long the confetti falls",
		section = confettiSection,
		position = 2
	)
	default int confettiDuration()
	{
		return 3;
	}
}
