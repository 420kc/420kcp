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
	private static final Pattern KC_PATTERN =
		Pattern.compile("Your .+ kill count is: (\\d[\\d,]*)\\.");

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
