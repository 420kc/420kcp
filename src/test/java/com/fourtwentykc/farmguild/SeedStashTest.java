package com.fourtwentykc.farmguild;

import org.junit.Assert;
import org.junit.Test;

public class SeedStashTest
{
	@Test
	public void theSeedVaultIsTheSeedStashInEachCaseTheGameWritesIt()
	{
		Assert.assertEquals("<col=00ffff>Seed stash", SeedStash.rename("<col=00ffff>Seed vault"));
		Assert.assertEquals("Seed Stash", SeedStash.rename("Seed Vault"));
		Assert.assertEquals("Open the seed stash.", SeedStash.rename("Open the seed vault."));
		Assert.assertEquals("Seed pack", SeedStash.rename("Seed pack"));
		Assert.assertNull(SeedStash.rename(null));
	}
}
