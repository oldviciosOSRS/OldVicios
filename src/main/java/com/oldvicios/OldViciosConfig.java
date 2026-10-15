package com.oldvicios;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(OldViciosConfig.GROUP)
public interface OldViciosConfig extends Config
{
	String GROUP = "oldvicios";

	String THIRD_PARTY_WARNING = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers";

	@ConfigSection(
		name = "Drops e pets",
		description = "Envio de drops e pets para o Discord do clã",
		position = 1
	)
	String dropsSection = "drops";

	@ConfigItem(
		keyName = "connect",
		name = "Conectar ao Old Vicios",
		description = "Conecta sua conta ao servidor do Old Vicios. Necessário para todas as funções online.",
		warning = THIRD_PARTY_WARNING,
		position = 0
	)
	default boolean connect()
	{
		return false;
	}

	@ConfigItem(
		keyName = "sendDrops",
		name = "Enviar drops",
		description = "Publica seus drops no Discord do clã",
		section = dropsSection,
		position = 0
	)
	default boolean sendDrops()
	{
		return false;
	}

	@ConfigItem(
		keyName = "minDropValue",
		name = "Valor mínimo do drop",
		description = "Só envia drops com valor total (GE) igual ou maior que este",
		section = dropsSection,
		position = 1
	)
	default int minDropValue()
	{
		return 1_000_000;
	}

	@ConfigItem(
		keyName = "sendPets",
		name = "Enviar pets",
		description = "Publica seus pets no Discord do clã",
		section = dropsSection,
		position = 2
	)
	default boolean sendPets()
	{
		return false;
	}
}
