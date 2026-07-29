package com.fourtwentykc;

import com.google.inject.Provides;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "420 kc",
	description = "Celebrate 420 kill count milestones with chat messages",
	tags = {"420", "kc", "boss", "kill count"}
)
public class FourTwentyKcPlugin extends Plugin
{
	// Mirrors RuneLite's ChatCommandsPlugin KILLCOUNT_PATTERN: the game says
	// "kill count" for bosses but "completion count" for the Gauntlet, "completed"
	// for raids, "subdued" for Wintertodt, plus harvest/lap/success variants.
	// Matching only "kill count" missed a 420 Gauntlet completion (2026-07-28).
	// The number keeps its <col> wrapper as the discriminator, so this stays as
	// strict as RuneLite's own parser.
	static final Pattern KC_PATTERN = Pattern.compile(
		"Your (?:completion count for |subdued |completed )?(?:<col=[0-9a-f]{6}>)?.+?(?:</col>)? "
			+ "(?:(?:kill|harvest|lap|completion|success|Total Ticket) )?(?:count )?"
			+ "is: ?<col=[0-9a-f]{6}>([0-9,]+)</col>");

	@Inject
	private FourTwentyKcConfig config;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Provides
	FourTwentyKcConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FourTwentyKcConfig.class);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		// Match the raw message: the <col> tags around the number are part of
		// the pattern's strictness, so they must survive to the matcher.
		Matcher matcher = KC_PATTERN.matcher(event.getMessage());
		if (!matcher.find())
		{
			return;
		}

		String kcStr = matcher.group(1).replace(",", "");
		int kc;
		try
		{
			kc = Integer.parseInt(kcStr);
		}
		catch (NumberFormatException e)
		{
			return;
		}

		if (kc == 420 && config.chatMessages())
		{
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.GAMEMESSAGE)
				.runeLiteFormattedMessage("<col=00ff00>420 kc</col>: blaze it!")
				.build());
		}
	}
}
