package com.oldvicios.party;

import java.awt.Color;

/**
 * Jogador num grupo (membro ou candidato), como o backend devolve (/v1/plugin/party).
 * Campos preenchidos pelo Gson a partir do JSON.
 */
final class PartyMember
{
	private static final Color[] COLORS = {
		new Color(127, 162, 255), new Color(232, 160, 216), new Color(240, 200, 100),
		new Color(130, 210, 230), new Color(200, 170, 255), new Color(143, 211, 160),
	};

	/** Id da conta no backend (usado para aceitar, recusar e remover). */
	int id;
	String name;
	int combatLevel;
	AccountType accountType;
	/** null = sem KC (atividade sem KC no hiscore, ou não informado). */
	Integer killCount;
	/** O loot que o jogador quer. */
	LootMode lootPreference;
	boolean leader;

	/** Cor do nome no quadradinho, fixa por jogador. */
	Color color()
	{
		return COLORS[Math.floorMod(id, COLORS.length)];
	}

	/** NORMAL quando o backend manda um modo que esta versão não conhece. */
	AccountType accountType()
	{
		return accountType != null ? accountType : AccountType.NORMAL;
	}

	String lootText()
	{
		return lootPreference != null ? lootPreference.displayName : "—";
	}
}
