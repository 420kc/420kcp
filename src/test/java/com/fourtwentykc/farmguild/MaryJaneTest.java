package com.fourtwentykc.farmguild;

import org.junit.Assert;
import org.junit.Test;

public class MaryJaneTest
{
	@Test
	public void everyPlaceTheGameNamesHerSaysMaryJane()
	{
		Assert.assertEquals("<col=ffff00>Mary Jane", MaryJane.rename("<col=ffff00>Guildmaster Jane"));
		Assert.assertEquals("Mary Jane has given you a seed pack.",
			MaryJane.rename("Guildmaster Jane has given you a seed pack."));
		Assert.assertTrue(MaryJane.isHer("Guildmaster Jane"));
		Assert.assertTrue(MaryJane.isHer("Mary Jane"));
		Assert.assertFalse(MaryJane.isHer("Jane"));
	}

	@Test
	public void herLinesAreDylansPicksWhateverTheGameWrapsThemIn()
	{
		Assert.assertEquals("Peace, friend. You made it. Most people rush right past. What brings you in?",
			MaryJane.say("Welcome to the farming guild. How may<br>I help you?", n -> 0));
		Assert.assertEquals("I'm Mary Jane. I help things grow. Plants, people, whatever shows up.",
			MaryJane.say("My name is Jane, and I am the founder of the Farming Guild.", n -> n - 1));
		Assert.assertEquals("Hey, not yet. 85 Farming for up there. The top tier is a lot. Grow into it.",
			MaryJane.say("Hey! You need a Farming level of at least 85 to enter that area of the Farming Guild.",
				n -> 0));
	}

	@Test
	public void aLineWithSeveralPicksSaysOneOfThem()
	{
		String game = "The heat of the volcano allows us to grow things here that would never survive in the climate"
			+ " of Kourend. The discoveries we have made are worth the extra risk.";
		Assert.assertNotEquals(MaryJane.say(game, n -> 0), MaryJane.say(game, n -> 2));
	}

	@Test
	public void herCountAndHerEmptyTradesKeepWhatTheGameSays()
	{
		Assert.assertEquals("You've grown 12 contracts with us. That's 12 little acts of love for the soil.",
			MaryJane.say("You've completed 12 farming contracts.", n -> 0));
		Assert.assertEquals("You've grown 1 contract with us. That's 1 little act of love for the soil.",
			MaryJane.say("You've completed 1 farming contract.", n -> 0));
		Assert.assertEquals("Nothing to trade right now, and that's alright. Come back when the seeds find you.",
			MaryJane.say("I'm afraid you have no Spirit seedling(w)s to trade in, come back when you do.", n -> 0));
		Assert.assertEquals("Nothing to trade right now, and that's alright. Come back when the seeds find you.",
			MaryJane.say("I'm afraid you have nothing to trade in, come back when you do.", n -> 0));
	}

	@Test
	public void aLineNotPickedStaysTheGames()
	{
		Assert.assertNull(MaryJane.say("What kind of contract would you like?", n -> 0));
		Assert.assertNull(MaryJane.say(null, n -> 0));
	}
}
