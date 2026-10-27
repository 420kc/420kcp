package com.fourtwentykc.farmguild;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Guildmaster Jane is Mary Jane: every place the game names her (her right-click options, the name over her
 * dialogue, the seed pack she hands over), and her lines, Dylan's picks of 10/23. A line with more than one pick
 * says one of them each time. The game's lines are matched as her wiki transcript gives them
 * (Transcript:Guildmaster_Jane), ignoring case, tags and line breaks; a line not listed stays the game's. The other
 * two NPCs named plain Jane (12951, 12952) are someone else and keep their name.
 */
final class MaryJane
{
	static final String NAME = "Mary Jane";

	private static final String GAME_NAME = "Guildmaster Jane";

	/** Her lines: the game's (normalized, see {@link #key}) and what Mary Jane says instead. */
	private static final Map<String, String[]> LINES = new LinkedHashMap<>();

	/** Her lines that carry a number or an item: a pattern over the normalized line, and the line with its groups. */
	private static final Map<Pattern, String[]> PATTERNS = new LinkedHashMap<>();

	static
	{
		line("Welcome to the farming guild. How may I help you?",
			"Peace, friend. You made it. Most people rush right past. What brings you in?");
		line("After the Shayzien Army cleared out the lizardmen in this area, I founded this guild with some fellow"
				+ " Hosidius farmers.",
			"This plant brought us together. Hosidius farmers, a few wanderers, one very confused Shayzien soldier."
				+ " That's how a guild starts.",
			"I didn't found this guild to own anything. After the lizardmen left, the land lent itself to us. We grow"
				+ " what it offers and pass it around.");
		line("The heat of the volcano allows us to grow things here that would never survive in the climate of"
				+ " Kourend. The discoveries we have made are worth the extra risk.",
			"The volcano is the earth breathing out. We just breathe in. Things grow here that Kourend's cold would"
				+ " never let live.",
			"Kourend is lovely, but it's tense. Here the ground is warm, and so are the people. The plants feel it.",
			"The heat, mostly. And the quiet. A plant grows best where nobody's in a hurry.");
		line("The guild is split into three tiers, with only advanced farmers being allowed access to the upper"
				+ " echelons.",
			"Three tiers, like a journey. You start at the roots and work your way up. No shortcuts. The plants"
				+ " always know.",
			"Three tiers. Everyone begins at the bottom, and honestly, that's the beautiful part.");
		line("My name is Jane, and I am the founder of the Farming Guild.",
			"Mary Jane. Founder, gardener, listener. Mostly listener. The plants have a lot to say.",
			"I'm Mary Jane. I help things grow. Plants, people, whatever shows up.");
		line("Well we always need to rotate the crops we're growing to stop the soil from tiring due to lack of"
				+ " nutrients. It would be great if you could help out with that.",
			"The soil gives us so much. We rotate the crops so it can rest and give again. It's a relationship,"
				+ " really.",
			"Nothing should have to do the same thing forever. Not soil, not people. Help me rotate the crops?");
		line("If you're interested, I can give you a farming contract which tells you what to plant within the guild."
				+ " Complete the contract, and I'll give you some seeds as a thank you.",
			"I'll give you a contract: what to plant, and where. Grow it with good intentions, and I'll send you off"
				+ " with a seed pack. Seeds want to travel.");
		line("You'll be wanting a reward then. Come back when you have some room to take it.",
			"Your bag's holding a lot right now. Make a little room, and come back for your reward.");
		line("You'll be wanting a reward then. Here's a seed pack.",
			"You did it. Can you feel that? The soil's thanking you. Here's a seed pack. Pass the good on.");
		line("Of course, I will also take potted spirit seeds and saplings. What would you like to trade in?",
			"Spirit seeds carry a lot of energy. I'll take them, potted ones and saplings too, and give you something"
				+ " to plant in return.");
		line("Sure, let me know if there's anything I can do for you.",
			"That's beautiful. Some of the best things in life are just passing by. Take a little peace with you.",
			"Of course. Walk slow on your way out. Notice something green.");
		line("Hey! You need a Farming level of at least 65 to enter that area of the Farming Guild.",
			"Oh, friend. That tier needs 65 Farming. You're not ready yet, and that's okay. You will be.",
			"Gently, gently. You need 65 Farming for that tier. Everything in its season.");
		line("Hey! You need a Farming level of at least 85 to enter that area of the Farming Guild.",
			"Hey, not yet. 85 Farming for up there. The top tier is a lot. Grow into it.");
		line("While I am letting you pick your own contracts, I won't allow you to pick the same one again. We must"
				+ " ensure that the patches can be rotated to stop the soil tiring from lack of nutrients.",
			"Same one again? I hear you, but the soil needs variety, just like us. Try something new.");

		pattern("you've completed 1 farming contract\\.",
			"You've grown 1 contract with us. That's 1 little act of love for the soil.");
		pattern("you've completed (\\d+) farming contracts?\\.",
			"You've grown $1 contracts with us. That's $1 little acts of love for the soil.");
		pattern("i'm afraid you have (?:no .+|nothing) to trade in, come back when you do\\.",
			"Nothing to trade right now, and that's alright. Come back when the seeds find you.");
	}

	private MaryJane()
	{
	}

	private static void line(String game, String... hers)
	{
		LINES.put(key(game), hers);
	}

	private static void pattern(String game, String... hers)
	{
		PATTERNS.put(Pattern.compile(game), hers);
	}

	/** The text with her name as Mary Jane's, or the same text when it doesn't name her. */
	static String rename(String text)
	{
		if (text == null || text.indexOf("Jane") < 0)
		{
			return text;
		}
		return text.replace(GAME_NAME, NAME);
	}

	/** Whether a dialogue's name line is hers, before or after the rename. */
	static boolean isHer(String name)
	{
		return name != null && (name.contains(GAME_NAME) || name.contains(NAME));
	}

	/**
	 * What Mary Jane says for this line of Guildmaster Jane's, {@code choose} picking among several (given how many,
	 * it returns an index), or null for a line she says as the game does.
	 */
	static String say(String line, IntUnaryOperator choose)
	{
		if (line == null)
		{
			return null;
		}
		String key = key(line);
		String[] hers = LINES.get(key);
		if (hers != null)
		{
			return hers[choose.applyAsInt(hers.length)];
		}
		for (Map.Entry<Pattern, String[]> entry : PATTERNS.entrySet())
		{
			Matcher m = entry.getKey().matcher(key);
			if (m.matches())
			{
				String[] options = entry.getValue();
				return m.replaceFirst(options[choose.applyAsInt(options.length)]);
			}
		}
		return null;
	}

	/** A line as compared: tags dropped, line breaks as spaces, spaces collapsed, lower case. */
	static String key(String text)
	{
		return text.replaceAll("(?i)<br>", " ").replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").trim()
			.toLowerCase();
	}
}
