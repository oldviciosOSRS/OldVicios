package com.oldvicios.party;

import java.util.Collections;
import java.util.List;

/**
 * Um grupo como o backend devolve (/v1/plugin/party). Na lista vem sem members/applicants; no detalhe
 * vem com os membros, e com os candidatos só para o líder. Depois de criado, o grupo não muda.
 */
final class Party
{
	String id;
	String name;
	/** null quando o backend manda uma atividade que esta versão do plugin não conhece. */
	Activity activity;
	/** null = atividade sem modos. */
	String mode;
	int world;
	int slots;
	int memberCount;
	boolean learners;
	/** Regra de loot do grupo (cada membro tem também o loot que ele quer). */
	LootMode lootMode;
	int minCombat;
	int minKillCount;
	String leaderName;
	boolean applicationsOpen;
	String description;
	List<PartyMember> members;
	List<PartyMember> applicants;

	boolean isFull()
	{
		return memberCount >= slots;
	}

	String slotsText()
	{
		return memberCount + "/" + slots;
	}

	/** "Chambers of Xeric, Normal, W302" (sem o modo quando a atividade não tem). */
	String summary()
	{
		return activity.displayName + (mode != null ? ", " + mode : "") + ", W" + world;
	}

	String lootText()
	{
		return lootMode != null ? lootMode.displayName : "—";
	}

	String descriptionText()
	{
		return description != null ? description : "";
	}

	List<PartyMember> members()
	{
		return members != null ? members : Collections.emptyList();
	}

	List<PartyMember> applicants()
	{
		return applicants != null ? applicants : Collections.emptyList();
	}
}
