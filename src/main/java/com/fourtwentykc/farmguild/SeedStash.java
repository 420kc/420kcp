package com.fourtwentykc.farmguild;

/**
 * The guild's seed vault is the Seed Stash (Dylan, 10/23): in its right-click options, in the game's sentence case
 * as "Seed vault" is, and over its window, in the title's own case.
 */
final class SeedStash
{
	private SeedStash()
	{
	}

	/** The text with the vault named the Seed Stash, or the same text when it doesn't name it. */
	static String rename(String text)
	{
		if (text == null || text.indexOf("eed ") < 0)
		{
			return text;
		}
		return text.replace("Seed Vault", "Seed Stash").replace("Seed vault", "Seed stash").replace("seed vault", "seed stash");
	}
}
