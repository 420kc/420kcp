package com.fourtwentykc;

import com.fourtwentykc.farmguild.FarmGuild;
import com.google.inject.Provides;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.GameEventManager;

@Slf4j
@PluginDescriptor(
	name = "420 kc",
	description = "Celebrate when boss, activity, or clue counts reach 420, and grow hemp in the Farming Guild on worlds 420 to 422",
	tags = {"420", "kc", "boss", "kill count", "activity", "clue", "clue scroll", "popup", "smoke", "confetti",
		"farming", "guild", "hemp"}
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

	// The count-bearing messages that do NOT say "count is:" at all, from the
	// same ChatCommandsPlugin catalog (2026-07-30 audit - the full sweep after
	// the Gauntlet taught us Jagex never uses one wording when five will do).
	// One capturing group each: the count.
	static final Pattern SEPULCHRE_PATTERN = Pattern.compile(
		"You have completed Floor \\d of the Hallowed Sepulchre! Total completions: "
			+ "<col=ff0000>([0-9,]+)</col>\\.");
	static final Pattern GRAND_HALLOWED_COFFIN_PATTERN = Pattern.compile(
		"You have opened the Grand Hallowed Coffin <col=ff0000>([0-9,]+)</col> times?!");
	static final Pattern RIFTS_CLOSED_PATTERN = Pattern.compile(
		"Amount of Rifts you have closed: <col=ff0000>([0-9,]+)</col>\\.",
		Pattern.CASE_INSENSITIVE);
	static final Pattern HUNTER_RUMOUR_PATTERN = Pattern.compile(
		"You have completed <col=[0-9a-f]{6}>([0-9,]+)</col> rumours? for the Hunter Guild\\.");
	static final Pattern BIRD_EGG_PATTERN = Pattern.compile(
		"You have made <col=ff0000>([0-9,]+)</col> offerings?\\.");
	static final Pattern CHEST_OPENING_PATTERN = Pattern.compile(
		"You have opened (?:the )?(?:crystal chest|Larran's big chest|Larran's small chest"
			+ "|Brimstone chest) ([0-9,]+) times\\.");

	// RuneLite parses clue totals separately in LootTrackerPlugin. The count has
	// no dedicated col wrapper and precedes the tier; the optional plural matches
	// both first and later completions.
	static final Pattern TREASURE_TRAIL_PATTERN = Pattern.compile(
		"You have completed ([0-9,]+) [a-z]+ Treasure Trails?\\.");

	// Duel Arena wins/losses are in RuneLite's catalog but the content left the
	// game; those messages can no longer occur, so they are deliberately absent.
	static final Pattern[] KC_PATTERNS = {
		KC_PATTERN, SEPULCHRE_PATTERN, GRAND_HALLOWED_COFFIN_PATTERN,
		RIFTS_CLOSED_PATTERN, HUNTER_RUMOUR_PATTERN, BIRD_EGG_PATTERN,
		CHEST_OPENING_PATTERN, TREASURE_TRAIL_PATTERN,
	};

	/** The count carried by a game message, or null if it carries none. */
	static String countFrom(String message)
	{
		for (Pattern pattern : KC_PATTERNS)
		{
			Matcher matcher = pattern.matcher(message);
			if (matcher.find())
			{
				return matcher.group(1);
			}
		}
		return null;
	}

	@Inject
	private FourTwentyKcConfig config;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private EventBus eventBus;

	@Inject
	private GameEventManager gameEventManager;

	// The Farming Guild's hemp and Mary Jane (2.0.0).
	@Inject
	private FarmGuild farmGuild;

	// Each start of the plugin draws with its own overlay and popup, so an earlier stop's cleanup can never end a
	// celebration that began after it.
	private BlazeOverlay overlay;
	private BlazePopup popup;

	@Provides
	FourTwentyKcConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FourTwentyKcConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlay = new BlazeOverlay(client);
		popup = new BlazePopup(client);
		overlayManager.add(overlay);
		// RuneLite replays the loaded scene to the plugin alone, so the guild asks for its own: started in game, the
		// guild already standing grows its hemp.
		eventBus.register(farmGuild);
		gameEventManager.simulateGameEvents(farmGuild);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		eventBus.unregister(farmGuild);
		farmGuild.stop();
		// The overlay draws and the popup lives on the client thread: they end there too.
		BlazeOverlay stopping = overlay;
		BlazePopup closing = popup;
		clientThread.invoke(() ->
		{
			stopping.stop();
			closing.clear();
		});
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		popup.tick();
	}

	/** {@code ::420} shows the celebration as it is set, without waiting for a real 420. */
	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		if ("420".equals(event.getCommand()))
		{
			celebrate();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		// Match the raw message: the <col> tags around the number are part of
		// the patterns' strictness, so they must survive to the matcher.
		String count = countFrom(event.getMessage());
		if (count == null)
		{
			return;
		}

		String kcStr = count.replace(",", "");
		int kc;
		try
		{
			kc = Integer.parseInt(kcStr);
		}
		catch (NumberFormatException e)
		{
			return;
		}

		if (kc != 420)
		{
			return;
		}
		if (config.chatMessages())
		{
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.GAMEMESSAGE)
				.runeLiteFormattedMessage("<col=00ff00>420 kc</col>: blaze it!")
				.build());
		}
		celebrate();
	}

	/** The popup, smoke and confetti, each as its setting says. */
	private void celebrate()
	{
		if (config.popup())
		{
			popup.show();
		}
		if (config.smoke())
		{
			overlay.smoke(config.smokeDuration(), config.smokeDensity());
		}
		if (config.confetti())
		{
			overlay.confetti(config.confettiDuration(), config.confettiDensity());
		}
	}
}
