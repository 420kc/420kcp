package com.fourtwentykc.farmguild;

import java.time.LocalTime;
import net.runelite.api.coords.WorldPoint;
import org.junit.Assert;
import org.junit.Test;

public class FarmGuildSceneryTest
{
	@Test
	public void theGuildsTreesAndBushGrowHemp()
	{
		// A 34486 tree and the 33638 bush inside the guild, from the cache's placements.
		Assert.assertTrue(FarmGuildScenery.replaces(34486, new WorldPoint(1240, 3740, 0), false));
		Assert.assertTrue(FarmGuildScenery.replaces(33638, new WorldPoint(1237, 3724, 0), false));
	}

	@Test
	public void nothingElseAndNowhereElseChanges()
	{
		// The guild's door, and the same tree id outside the guild's map square.
		Assert.assertFalse(FarmGuildScenery.replaces(34463, new WorldPoint(1248, 3723, 0), true));
		Assert.assertFalse(FarmGuildScenery.replaces(34486, new WorldPoint(3222, 3218, 0), true));
		Assert.assertFalse(FarmGuildScenery.replaces(34486, null, true));
	}

	@Test
	public void moreHempTakesThePlainCratesAndLeavesTheOnesYouCanSearch()
	{
		// The stacked crate beside the seed vault (1243, 3740), and a crate that can be searched.
		WorldPoint besideTheVault = new WorldPoint(1244, 3742, 0);
		Assert.assertTrue(FarmGuildScenery.replaces(7513, besideTheVault, true));
		Assert.assertFalse(FarmGuildScenery.replaces(7513, besideTheVault, false));
		Assert.assertFalse(FarmGuildScenery.replaces(34508, new WorldPoint(1235, 3738, 0), true));
	}

	@Test
	public void aStageIsItsPlantAndRandomIsAnyStage()
	{
		Assert.assertEquals(59774, HempStage.GROWN.forPlant(n -> 0).getModel());
		Assert.assertEquals(HempStage.STAGE_1, HempStage.STAGE_1.forPlant(n -> 4));
		Assert.assertEquals(HempStage.STAGE_1, HempStage.RANDOM.forPlant(n -> 0));
		Assert.assertEquals(HempStage.GROWN, HempStage.RANDOM.forPlant(n -> n - 1));
	}

	@Test
	public void atFourTwentyEveryPlantGrowsAStageAndGrownStaysGrown()
	{
		Assert.assertEquals(HempStage.STAGE_2, HempStage.STAGE_1.next());
		Assert.assertEquals(HempStage.GROWN, HempStage.STAGE_4.next());
		Assert.assertEquals(HempStage.GROWN, HempStage.GROWN.next());
		Assert.assertTrue(HempStage.isFourTwenty(LocalTime.of(16, 20, 59)));
		Assert.assertFalse(HempStage.isFourTwenty(LocalTime.of(4, 20)));
		Assert.assertFalse(HempStage.isFourTwenty(LocalTime.of(16, 21)));
	}
}
