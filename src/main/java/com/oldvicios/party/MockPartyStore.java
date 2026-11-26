package com.oldvicios.party;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Estado da aba Grupos, todo em memória e mockado. Depois vira chamadas ao backend
 * (/v1/plugin/parties) e leituras do cliente (combate) e do hiscore (KC).
 */
final class MockPartyStore
{
	enum Application
	{
		NONE, PENDING, ACCEPTED
	}

	/** Resultado de um requisito para o jogador local. */
	static final class Check
	{
		final String label;
		final String value;
		final boolean met;

		Check(String label, String value, boolean met)
		{
			this.label = label;
			this.value = value;
			this.met = met;
		}
	}

	// Jogador local (mock). No plugin real: client.getLocalPlayer().getCombatLevel() e o hiscore para o KC.
	static final int MY_COMBAT_LEVEL = 118;
	static final int MY_WORLD = 416;
	private static final Map<Activity, Integer> MY_KILL_COUNT = new EnumMap<>(Activity.class);

	static
	{
		MY_KILL_COUNT.put(Activity.CHAMBERS_OF_XERIC, 87);
		MY_KILL_COUNT.put(Activity.THEATRE_OF_BLOOD, 64);
		MY_KILL_COUNT.put(Activity.TOMBS_OF_AMASCUT, 41);
		MY_KILL_COUNT.put(Activity.THE_FRACTURED_ARCHIVE, 3);
		MY_KILL_COUNT.put(Activity.YAMA, 12);
		MY_KILL_COUNT.put(Activity.NEX, 20);
	}

	private static final Color[] COLORS = {
		new Color(127, 162, 255), new Color(232, 160, 216), new Color(240, 200, 100),
		new Color(130, 210, 230), new Color(200, 170, 255),
	};
	private static final Color MY_COLOR = new Color(143, 211, 160);

	private final List<Party> parties = new ArrayList<>();
	private PartyMember me = new PartyMember("Você", MY_COMBAT_LEVEL, 0, MY_WORLD, MY_COLOR, false);

	/** Grupo em que sou o líder. */
	Party ownParty;
	/** Grupo em que me candidatei (ou fui aceito). */
	Party appliedParty;
	Application application = Application.NONE;

	MockPartyStore()
	{
		Party cox = new Party("CoX tranquilo", Activity.CHAMBERS_OF_XERIC, "Normal", 302, 3, true, LootMode.SPLIT, 100, 25,
			"Trio tranquilo, sem pressa. Discord opcional.", member("Lola Vicia", 123, 140, 302, 0, true));
		cox.members.add(member("Tia Neide", 117, 51, 302, 1, false));

		Party toa = new Party("ToA 300 rápida", Activity.TOMBS_OF_AMASCUT, "Expert", 330, 4, false, LootMode.SPLIT_DUPES, 110, 100,
			"Raid 300 rápida, wardens no esquema.", member("Kiko Tbow", 126, 300, 330, 2, true));
		toa.members.add(member("Lola Vicia", 123, 140, 330, 0, true));
		toa.members.add(member("Baiano Rng", 121, 64, 330, 3, true));

		Party titans = new Party("Pet hunt", Activity.ROYAL_TITANS, null, 416, 2, true, LootMode.SPLIT, 0, 0,
			"Atrás do pet, loot dividido.", member("Baiano Rng", 121, 64, 416, 3, true));

		Party tfa = new Party("TFA semanal", Activity.THE_FRACTURED_ARCHIVE, null, 420, 8, true, LootMode.FFA, 120, 0,
			"Primeira run da semana.", member("Tia Neide", 117, 51, 420, 1, false));
		tfa.members.add(member("Juninho PvM", 116, 50, 420, 4, false));
		tfa.members.add(member("Kiko Tbow", 126, 300, 420, 2, true));

		Party yama = new Party("", Activity.YAMA, null, 444, 2, false, LootMode.SPLIT, 0, 0,
			"", member("Juninho PvM", 116, 50, 444, 4, false));

		Party tob = new Party("HM fixo", Activity.THEATRE_OF_BLOOD, "Hard Mode", 416, 5, false, LootMode.FFA, 120, 100,
			"HM com time fixo.", member("Kiko Tbow", 126, 300, 416, 2, true));
		for (int i = 0; i < 4; i++)
		{
			tob.members.add(member("Membro " + (i + 1), 120, 150, 416, i, true));
		}

		parties.add(cox);
		parties.add(toa);
		parties.add(titans);
		parties.add(tfa);
		parties.add(yama);
		parties.add(tob);
	}

	/** Modo de jogo de cada jogador de mentira (quem não está aqui é conta normal). */
	private static final Map<String, AccountType> ACCOUNT_TYPES = new HashMap<>();

	static
	{
		ACCOUNT_TYPES.put("Lola Vicia", AccountType.GROUP_IRONMAN);
		ACCOUNT_TYPES.put("Tia Neide", AccountType.HARDCORE_IRONMAN);
		ACCOUNT_TYPES.put("Baiano Rng", AccountType.ULTIMATE_IRONMAN);
		ACCOUNT_TYPES.put("Juninho PvM", AccountType.IRONMAN);
		ACCOUNT_TYPES.put("Membro 2", AccountType.HARDCORE_GROUP_IRONMAN);
	}

	/** Loot que cada jogador de mentira prefere (quem não está aqui fica com Split). */
	private static final Map<String, LootMode> LOOT_PREFERENCES = new HashMap<>();

	static
	{
		LOOT_PREFERENCES.put("Tia Neide", LootMode.FFA);
		LOOT_PREFERENCES.put("Baiano Rng", LootMode.SPLIT_DUPES);
		LOOT_PREFERENCES.put("Juninho PvM", LootMode.FFA);
	}

	private static PartyMember member(String name, int combatLevel, int killCount, int world, int color,
		boolean inRuneLiteParty)
	{
		PartyMember m = new PartyMember(name, combatLevel, killCount, world, COLORS[color % COLORS.length], inRuneLiteParty);
		m.accountType = ACCOUNT_TYPES.getOrDefault(name, AccountType.NORMAL);
		m.lootPreference = LOOT_PREFERENCES.getOrDefault(name, LootMode.SPLIT);
		return m;
	}

	void setLocalPlayerName(String name)
	{
		PartyMember renamed = new PartyMember(name, MY_COMBAT_LEVEL, 0, me.world, MY_COLOR, me.inRuneLiteParty);
		renamed.lootPreference = me.lootPreference;
		for (Party p : parties)
		{
			p.members.replaceAll(m -> m == me ? renamed : m);
		}
		me = renamed;
	}

	PartyMember localPlayer()
	{
		return me;
	}

	/** Já lidera ou está candidato/aceito em algum grupo (um grupo por vez). */
	boolean isBusy()
	{
		return ownParty != null || application != Application.NONE;
	}

	List<Party> list(Activity filter, boolean full)
	{
		List<Party> result = new ArrayList<>();
		for (Party p : parties)
		{
			if ((filter == null || p.activity == filter) && p.isFull() == full)
			{
				result.add(p);
			}
		}
		return result;
	}

	List<Check> check(Party p)
	{
		List<Check> checks = new ArrayList<>();
		if (p.minCombat > 0)
		{
			checks.add(new Check("Combate " + p.minCombat + "+", String.valueOf(MY_COMBAT_LEVEL),
				MY_COMBAT_LEVEL >= p.minCombat));
		}
		if (p.minKillCount > 0)
		{
			int kc = MY_KILL_COUNT.getOrDefault(p.activity, 0);
			checks.add(new Check("KC " + p.minKillCount + "+", String.valueOf(kc), kc >= p.minKillCount));
		}
		return checks;
	}

	static boolean allMet(List<Check> checks)
	{
		for (Check c : checks)
		{
			if (!c.met)
			{
				return false;
			}
		}
		return true;
	}

	/** partyLoot = regra de loot do grupo; ownLoot = o loot que o criador quer (aparece no quadradinho dele). */
	Party create(String name, Activity activity, String mode, int world, int slots, boolean learners, LootMode partyLoot,
		LootMode ownLoot, int minCombat, int minKillCount, String description)
	{
		me.world = world;
		me.lootPreference = ownLoot;
		ownParty = new Party(name, activity, mode, world, slots, learners, partyLoot, minCombat, minKillCount, description, me);
		// Candidatos de mentira para dar o que testar no Aceitar/Recusar
		ownParty.applicants.add(member("Baiano Rng", 121, 64, 416, 3, true));
		ownParty.applicants.add(member("Tia Neide", 117, 51, 302, 1, false));
		parties.add(0, ownParty);
		return ownParty;
	}

	void close(Party p)
	{
		parties.remove(p);
		ownParty = null;
	}

	void accept(Party p, PartyMember m)
	{
		if (!p.isFull())
		{
			p.applicants.remove(m);
			p.members.add(m);
		}
	}

	void decline(Party p, PartyMember m)
	{
		p.applicants.remove(m);
	}

	void kick(Party p, PartyMember m)
	{
		p.members.remove(m);
	}

	/** lootPreference = o loot que o candidato quer; aparece para o líder junto com a candidatura. */
	void apply(Party p, LootMode lootPreference)
	{
		me.lootPreference = lootPreference;
		appliedParty = p;
		application = Application.PENDING;
	}

	/** Mock do líder aceitando: no plugin real chega pelo /updates. */
	void mockLeaderAccepts()
	{
		if (application == Application.PENDING && appliedParty != null && !appliedParty.isFull())
		{
			appliedParty.members.add(me);
			application = Application.ACCEPTED;
		}
	}

	void cancelApplication()
	{
		appliedParty = null;
		application = Application.NONE;
	}

	void leave()
	{
		if (appliedParty != null)
		{
			appliedParty.members.remove(me);
		}
		me.inRuneLiteParty = false;
		cancelApplication();
	}

	/** Mock de PartyService.changeParty(senha que o backend entrega ao aceitar). Sempre por clique do jogador. */
	void joinRuneLiteParty()
	{
		me.inRuneLiteParty = true;
	}

	// ---------- Debug (só com usuário mockado) ----------

	/** Vira dono de um grupo de ToB com membros e candidatos. */
	void debugBecomeLeader()
	{
		leave();
		if (ownParty == null)
		{
			Party p = create("ToB learners", Activity.THEATRE_OF_BLOOD, "Normal", MY_WORLD, 5, true, LootMode.SPLIT, LootMode.FFA,
				115, 50,
				"Eu explico as salas, sem pressa.");
			p.members.add(member("Kiko Tbow", 126, 300, MY_WORLD, 2, true));
			p.members.add(member("Lola Vicia", 123, 140, 302, 0, false));
		}
	}

	/** Volta a ser só um membro sem grupo, pronto para se candidatar. */
	void debugBecomeApplicant()
	{
		if (ownParty != null)
		{
			close(ownParty);
		}
		leave();
	}
}
