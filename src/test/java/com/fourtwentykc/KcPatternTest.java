package com.fourtwentykc;

import java.util.regex.Matcher;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The pattern must recognize every wording the game uses for a countable
 * completion, not just "kill count" - a 420 Gauntlet completion slipped
 * through the old boss-only pattern (2026-07-28). Message strings here are
 * the game's real formats with the number's col wrapper intact.
 */
public class KcPatternTest
{
	private static String kcOf(String message)
	{
		Matcher m = FourTwentyKcPlugin.KC_PATTERN.matcher(message);
		return m.find() ? m.group(1) : null;
	}

	@Test
	public void bossKillCountStillMatches()
	{
		assertEquals("420", kcOf("Your Zulrah kill count is: <col=ff0000>420</col>."));
	}

	@Test
	public void gauntletCompletionCountMatches()
	{
		// The message that started this: 420 Gauntlet, no blaze.
		assertEquals("420", kcOf("Your Gauntlet completion count is: <col=ff0000>420</col>."));
		assertEquals("420", kcOf("Your Corrupted Gauntlet completion count is: <col=ff0000>420</col>."));
	}

	@Test
	public void raidCompletedCountMatches()
	{
		assertEquals("420", kcOf("Your completed Theatre of Blood count is: <col=ff0000>420</col>."));
		assertEquals("420", kcOf("Your completed Chambers of Xeric count is: <col=ff0000>420</col>."));
		assertEquals("420", kcOf("Your completed Tombs of Amascut: Expert Mode count is: <col=ff0000>420</col>."));
	}

	@Test
	public void completionCountForPrefixMatches()
	{
		assertEquals("420", kcOf("Your completion count for Tempoross is: <col=ff0000>420</col>."));
	}

	@Test
	public void subduedWintertodtMatches()
	{
		assertEquals("420", kcOf("Your subdued Wintertodt count is: <col=ff0000>420</col>."));
	}

	@Test
	public void harvestAndLapCountsMatch()
	{
		assertEquals("420", kcOf("Your Herbiboar harvest count is: <col=ff0000>420</col>."));
		assertEquals("420", kcOf("Your Prifddinas Agility Course lap count is: <col=ff0000>420</col>."));
	}

	@Test
	public void commasSurviveExtraction()
	{
		assertEquals("1,420", kcOf("Your Barrows Chests kill count is: <col=ff0000>1,420</col>."));
	}

	@Test
	public void nearMissCountsStillMatchThePattern()
	{
		// The 420 gate is the plugin's job, not the pattern's.
		assertEquals("419", kcOf("Your Gauntlet completion count is: <col=ff0000>419</col>."));
	}

	@Test
	public void unwrappedNumbersDoNotMatch()
	{
		// The col wrapper is the discriminator against arbitrary "is: N" text,
		// same as RuneLite's own parser.
		assertFalse(FourTwentyKcPlugin.KC_PATTERN.matcher(
			"Your bank PIN is: 420.").find());
		assertFalse(FourTwentyKcPlugin.KC_PATTERN.matcher(
			"Your kill count is over 420, whatever that means.").find());
	}

	@Test
	public void colWrappedBossNameAlsoMatches()
	{
		// Some messages color the boss name too.
		assertEquals("420", kcOf(
			"Your <col=ff0000>TzTok-Jad</col> kill count is: <col=ff0000>420</col>."));
	}

	@Test
	public void plainChatDoesNotMatch()
	{
		assertTrue(kcOf("blaze it") == null);
	}
}
