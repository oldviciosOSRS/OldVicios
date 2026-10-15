package com.oldvicios;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(OldViciosConfig.GROUP)
public interface OldViciosConfig extends Config
{
	String GROUP = "oldvicios";

	@ConfigItem(
		keyName = "sendDrops",
		name = "Enviar drops",
		description = "Publica seus drops no Discord do OldVicios",
		position = 0
	)
	default boolean sendDrops()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sendPkDrops",
		name = "Enviar drops PK",
		description = "Publica seus drops de PvP no canal de PK do Discord do OldVicios",
		position = 1
	)
	default boolean sendPkDrops()
	{
		return false;
	}
}
