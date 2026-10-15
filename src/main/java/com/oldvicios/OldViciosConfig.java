package com.oldvicios;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(OldViciosConfig.GROUP)
public interface OldViciosConfig extends Config
{
	String GROUP = "oldvicios";

	@ConfigItem(
		keyName = "sendPkDrops",
		name = "Enviar drops PK",
		description = "Publica seus drops de PvP no canal de PK do Discord do OldVicios"
	)
	default boolean sendPkDrops()
	{
		return false;
	}
}
