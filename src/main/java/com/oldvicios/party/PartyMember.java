package com.oldvicios.party;

import java.awt.Color;

/** Jogador num grupo (membro ou candidato). Por enquanto, dados mockados. */
final class PartyMember
{
	final String name;
	final int combatLevel;
	final int killCount;
	final Color color;
	AccountType accountType = AccountType.NORMAL;
	/** Modo de loot que o jogador quer (escolhido ao se candidatar; o do grupo é só a preferência do líder). */
	LootMode lootPreference = LootMode.SPLIT;
	int world;
	/** Está na mesma party do RuneLite (entrou pelo botão "Entrar"). */
	boolean inRuneLiteParty;

	PartyMember(String name, int combatLevel, int killCount, int world, Color color, boolean inRuneLiteParty)
	{
		this.name = name;
		this.combatLevel = combatLevel;
		this.killCount = killCount;
		this.world = world;
		this.color = color;
		this.inRuneLiteParty = inRuneLiteParty;
	}
}
