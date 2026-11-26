package com.oldvicios;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class OldViciosPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(OldViciosPlugin.class);
		RuneLite.main(args);
	}
}
