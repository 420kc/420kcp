package com.fourtwentykc.farmguild;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;

/**
 * Which of the Farming Guild's objects grow hemp, read from the game cache's placements in region 4922 (10/23): the
 * guild's own trees and bushes, which arrived with it (ids next to its door's, 34463). The hemp
 * itself is {@link HempStage}.
 */
final class FarmGuildScenery
{
	/** The Farming Guild's map square. */
	static final int REGION = 4922;

	/** The guild's trees (34486 to 34488 one tile, 34489 two by two) and its bush, by the cache's count in 4922. */
	static final Set<Integer> REPLACED = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
		34486, // Tree, 69
		34487, // Tree, 21
		34488, // Tree, 19
		34489, // Tree, 2x2, 15
		33638  // Bush, 12
	)));

	/**
	 * More hemp (Dylan, 10/23): the guild's plain crates, the ones with nothing to click (their definitions have no
	 * options), the stacked one beside the seed vault among them. The crates that can be searched (34504, 34508 to
	 * 34510) stay crates.
	 */
	static final Set<Integer> MORE = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
		7513, // Crate, stacked, 1 (beside the seed vault)
		7514  // Crate, 5
	)));

	private FarmGuildScenery()
	{
	}

	/** Whether the object with this id at this tile grows hemp instead, with More hemp on or off. */
	static boolean replaces(int id, WorldPoint at, boolean more)
	{
		return at != null && at.getRegionID() == REGION && (REPLACED.contains(id) || more && MORE.contains(id));
	}
}
