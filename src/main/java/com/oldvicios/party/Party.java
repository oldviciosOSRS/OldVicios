package com.oldvicios.party;

import java.util.ArrayList;
import java.util.List;

/** Um grupo aberto por um membro do clã. Sem expiração: fica até o líder encerrar. */
final class Party
{
	/** Nome dado pelo líder (padrão: o nome da atividade). */
	final String name;
	final Activity activity;
	/** null = atividade sem modos. */
	final String mode;
	final int world;
	final int slots;
	final boolean learners;
	/** Regra de loot do grupo (cada membro tem também o loot que ele quer, em PartyMember). */
	final LootMode lootMode;
	final int minCombat;
	final int minKillCount;
	final String description;
	final PartyMember leader;
	final List<PartyMember> members = new ArrayList<>();
	final List<PartyMember> applicants = new ArrayList<>();
	boolean applicationsOpen = true;

	Party(String name, Activity activity, String mode, int world, int slots, boolean learners, LootMode lootMode,
		int minCombat, int minKillCount, String description, PartyMember leader)
	{
		this.name = name == null || name.trim().isEmpty() ? activity.displayName : name.trim();
		this.activity = activity;
		this.mode = mode;
		this.world = world;
		this.slots = slots;
		this.learners = learners;
		this.lootMode = lootMode;
		this.minCombat = minCombat;
		this.minKillCount = minKillCount;
		this.description = description;
		this.leader = leader;
		members.add(leader);
	}

	boolean isFull()
	{
		return members.size() >= slots;
	}

	String slotsText()
	{
		return members.size() + "/" + slots;
	}

	/** "Chambers of Xeric, Normal, W302" (sem o modo quando a atividade não tem). */
	String summary()
	{
		return activity.displayName + (mode != null ? ", " + mode : "") + ", W" + world;
	}
}
