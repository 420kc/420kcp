package com.fourtwentykc;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class FourTwentyKcPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(FourTwentyKcPlugin.class);
		RuneLite.main(args);
	}
}
