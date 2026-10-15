package com.oldvicios;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;

@ConfigGroup(OldViciosConfig.GROUP)
public interface OldViciosConfig extends Config
{
	String GROUP = "oldvicios";

	String DEFAULT_LOOT_MESSAGE = "%USERNAME% has looted: \n\n%LOOT%\nFrom: %SOURCE%";

	@ConfigSection(
		name = "Drops",
		description = "Envio de drops para o Discord do OldVicios",
		position = 0
	)
	String dropsSection = "drops";

	@ConfigSection(
		name = "Notificações",
		description = "Avisos de novos eventos, avisos da staff e mensagens pra você",
		position = 1
	)
	String notificationsSection = "notifications";

	@ConfigItem(
		keyName = "sendDrops",
		name = "Enviar drops",
		description = "Publica seus drops no Discord do OldVicios",
		section = dropsSection,
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
		section = dropsSection,
		position = 1
	)
	default boolean sendPkDrops()
	{
		return false;
	}

	@ConfigItem(
		keyName = "lootMessage",
		name = "Mensagem do drop",
		description = "Texto da mensagem no Discord. Use %USERNAME% (seu nome), %LOOT% (itens), %SOURCE% (boss/origem), "
			+ "%COUNT% (kill count), %TOTAL_VALUE% (valor total) e %WORLD% (mundo)",
		section = dropsSection,
		position = 2
	)
	default String lootMessage()
	{
		return DEFAULT_LOOT_MESSAGE;
	}

	@ConfigItem(
		keyName = "chatNotifications",
		name = "Mensagem no chat",
		description = "Mostra no chat do jogo quando sai um evento, um aviso ou uma mensagem pra você",
		section = notificationsSection,
		position = 0
	)
	default boolean chatNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notification",
		name = "Notificação do RuneLite",
		description = "Notificação do RuneLite (sistema, som, piscar a tela...). Clique na engrenagem pra escolher como receber",
		section = notificationsSection,
		position = 1
	)
	default Notification notification()
	{
		return Notification.ON;
	}
}
