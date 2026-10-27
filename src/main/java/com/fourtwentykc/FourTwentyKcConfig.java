package com.fourtwentykc;

import com.fourtwentykc.farmguild.HempStage;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(FourTwentyKcConfig.GROUP)
public interface FourTwentyKcConfig extends Config
{
	String GROUP = "fourtwentykc";

	@ConfigSection(
		name = "Farm Guild",
		description = "The Farming Guild grows hemp, and its guildmaster is Mary Jane",
		position = 4
	)
	String farmGuildSection = "farmGuild";

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

	@ConfigItem(
		keyName = "hempPlants",
		name = "Hemp plants",
		description = "The Farming Guild's trees and bushes grow hemp",
		section = farmGuildSection,
		position = 0
	)
	default boolean hempPlants()
	{
		return true;
	}

	@ConfigItem(
		keyName = "moreHemp",
		name = "More hemp",
		description = "The guild's plain crates grow hemp too; the ones you can search stay crates",
		section = farmGuildSection,
		position = 1
	)
	default boolean moreHemp()
	{
		return true;
	}

	@ConfigItem(
		keyName = "growthStage",
		name = "Growth stage",
		description = "How grown the hemp is: a stage, or Random, each plant its own stage whenever the guild loads."
			+ " Earlier stages draw faster",
		section = farmGuildSection,
		position = 2
	)
	default HempStage growthStage()
	{
		return HempStage.RANDOM;
	}

	@ConfigItem(
		keyName = "growAtFourTwenty",
		name = "Grow at 4:20",
		description = "At 4:20 pm your time, every hemp plant grows a stage",
		section = farmGuildSection,
		position = 3
	)
	default boolean growAtFourTwenty()
	{
		return true;
	}

	@ConfigItem(
		keyName = "maryJane",
		name = "Mary Jane",
		description = "Guildmaster Jane is Mary Jane, in her name and her lines",
		section = farmGuildSection,
		position = 4
	)
	default boolean maryJane()
	{
		return true;
	}

	// World 420 alone needs 1,500 total level; 421 and 422 are open to every members account (Dylan, 10/23).
	@ConfigItem(
		keyName = "only420Worlds",
		name = "Only show on w420-422",
		description = "The Farm Guild shows on worlds 420, 421 and 422 alone; other worlds keep the game's own guild",
		section = farmGuildSection,
		position = 5
	)
	default boolean only420Worlds()
	{
		return true;
	}
}
