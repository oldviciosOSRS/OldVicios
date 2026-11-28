/*
 * Adapted from Dink (https://github.com/pajlads/DinkPlugin): KillCountService.java,
 * KillCountNotifier.parseBoss and ClueNotifier.parse.
 *
 * BSD 2-Clause License
 *
 * Copyright (c) 2022, Jake Barter
 * All rights reserved.
 *
 * Copyright (c) 2022, pajlads
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oldvicios.loot;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.plugins.chatcommands.ChatCommandsPlugin;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.plugins.loottracker.LootTrackerConfig;
import net.runelite.client.plugins.loottracker.LootTrackerPlugin;
import net.runelite.http.api.loottracker.LootRecordType;

/**
 * KC de cada fonte de drop, como o Dink faz.
 * <ul>
 * <li>A mensagem "Your X kill count is: N" grava N - 1 para aquele boss; o evento de drop soma 1.</li>
 * <li>Sem mensagem na sessão, usa a KC salva pelo Chat Commands do RuneLite ou as kills do Loot Tracker.</li>
 * <li>Por isso a KC está pronta no drop mesmo quando a mensagem veio minutos antes (raid: completa e abre o baú depois).</li>
 * </ul>
 * Os eventos chegam na client thread; o cache aceita a correção agendada no executor.
 */
@Slf4j
@Singleton
public class KillCountService
{
	public static final String GAUNTLET_NAME = "Gauntlet", GAUNTLET_BOSS = "Crystalline Hunllef", THE_GAUNTLET = "The Gauntlet";
	public static final String CG_NAME = "Corrupted Gauntlet", CG_BOSS = "Corrupted Hunllef";
	public static final String TOA = "Tombs of Amascut";
	public static final String TOB = "Theatre of Blood";
	public static final String COX = "Chambers of Xeric";
	public static final String TFA = "Fractured Archive";

	private static final String RL_CHAT_CMD_PLUGIN_NAME = ChatCommandsPlugin.class.getSimpleName().toLowerCase();
	private static final String RL_LOOT_PLUGIN_NAME = LootTrackerPlugin.class.getSimpleName().toLowerCase();

	/** NPCs para os quais o RuneLite manda LootReceived em vez de NpcLootReceived. */
	public static final Set<Integer> SPECIAL_LOOT_NPC_IDS = Set.of(
		NpcID.WHISPERER, NpcID.WHISPERER_MELEE, NpcID.WHISPERER_QUEST, NpcID.WHISPERER_MELEE_QUEST,
		NpcID.ARAXXOR, NpcID.ARAXXOR_DEAD, NpcID.RT_FIRE_QUEEN_INACTIVE, NpcID.RT_ICE_KING_INACTIVE
	);
	public static final Set<String> SPECIAL_LOOT_NPC_NAMES = Set.of("The Whisperer", "Araxxor",
		"Branda the Fire Queen", "Eldric the Ice King", GAUNTLET_BOSS, CG_BOSS);

	/** NPCs cujo loot vem pelo ServerNpcLoot (o servidor avisa o drop): ids a partir deste número e os da lista. */
	public static final int SERVER_LOOT_NPC_ID_THRESHOLD = 15000;
	private static final Set<Integer> SERVER_LOOT_NPC_IDS = Set.of(
		NpcID.YAMA,
		NpcID.HESPORI,
		NpcID.SAILING_BULL_SHARK_DEAD,
		NpcID.SAILING_HAMMERHEAD_SHARK_DEAD,
		NpcID.SAILING_TIGER_SHARK_DEAD,
		NpcID.SAILING_GREAT_WHITE_SHARK_DEAD,
		NpcID.SAILING_NARWHAL_DEAD,
		NpcID.SAILING_ORCA_DEAD,
		NpcID.SAILING_PYGMY_KRAKEN_DEAD,
		NpcID.SAILING_SPINED_KRAKEN_DEAD,
		NpcID.SAILING_ARMOURED_KRAKEN_DEAD,
		NpcID.SAILING_VAMPYRE_KRAKEN_DEAD,
		NpcID.SAILING_EAGLE_RAY_DEAD,
		NpcID.SAILING_BUTTERFLY_RAY_DEAD,
		NpcID.SAILING_STINGRAY_DEAD,
		NpcID.SAILING_MANTA_RAY_DEAD,
		NpcID.SAILING_OSPREY_DEAD,
		NpcID.SAILING_ALBATROSS_DEAD,
		NpcID.SAILING_FRIGATEBIRD_DEAD,
		NpcID.SAILING_TERN_DEAD,
		NpcID.SAILING_SEA_MOGRE_DEAD,
		NpcID.SAILING_DOLPHIN_DEAD,
		NpcID.SAILING_VEILED_KRAKEN_DEAD,
		NpcID.MAGGOT_KING,
		NpcID.MAGGOT_KING_CORPSE
	);

	private static final Pattern PRIMARY_REGEX = Pattern.compile("Your (?<key>.+)\\s(?<type>kill|chest|completion|harvest|success|opened|lap|Total Ticket)\\s?count is: ?(?<value>[\\d,]+)\\b", Pattern.CASE_INSENSITIVE);
	private static final Pattern SECONDARY_REGEX = Pattern.compile("Your (?<type>completed|subdued) (?<key>.+) count is: (?<value>[\\d,]+)\\b");
	private static final Pattern CLUE_SCROLL_REGEX = Pattern.compile("You have completed (?<scrollCount>\\d+) (?<scrollType>\\w+) Treasure Trails?\\.");

	@Inject
	private ConfigManager configManager;

	@Inject
	private Gson gson;

	@Inject
	private ScheduledExecutorService executor;

	private final Cache<String, Integer> killCounts = CacheBuilder.newBuilder()
		.expireAfterAccess(10, TimeUnit.MINUTES)
		.maximumSize(64L)
		.build();

	/** Fonte do último drop ou da última mensagem de raid (separa os modos: Expert, Challenge Mode...). */
	@Nullable
	private volatile String lastDropSource;

	/** Boss e KC lidos de uma mensagem do jogo. */
	static final class BossCount
	{
		final String boss;
		final int count;

		BossCount(String boss, int count)
		{
			this.boss = boss;
			this.count = count;
		}
	}

	/** O loot deste NPC vem pelo ServerNpcLoot, não pelo NpcLootReceived. */
	public static boolean isServerLootNpc(int npcId)
	{
		return npcId >= SERVER_LOOT_NPC_ID_THRESHOLD || SERVER_LOOT_NPC_IDS.contains(npcId);
	}

	/** Trocou de conta: as KCs guardadas eram da outra. */
	public void reset()
	{
		lastDropSource = null;
		killCounts.invalidateAll();
	}

	public void onServerNpcLoot(String npcName)
	{
		if (npcName != null)
		{
			incrementKills(LootRecordType.NPC, npcName);
		}
	}

	public void onNpcKill(String npcName, int npcId)
	{
		if (SPECIAL_LOOT_NPC_IDS.contains(npcId))
		{
			// O RuneLite manda LootReceived para estes; tratado em onLoot
			return;
		}
		if (GAUNTLET_BOSS.equals(npcName) || CG_BOSS.equals(npcName))
		{
			// Já tratado pela mensagem de completion count
			return;
		}
		if (npcName != null)
		{
			incrementKills(LootRecordType.NPC, npcName);
		}
	}

	public void onLoot(LootReceived event)
	{
		boolean increment;
		switch (event.getType())
		{
			case NPC:
				// O RuneLite manda LootReceived (e não NpcLootReceived) para alguns NPCs
				increment = SPECIAL_LOOT_NPC_NAMES.contains(event.getName());
				break;
			case PLAYER:
				increment = false;
				break;
			default:
				increment = true;
				break;
		}
		if (increment)
		{
			incrementKills(event.getType(), getStandardizedSource(event));
		}
	}

	public void onGameMessage(String message)
	{
		// Contagem de clue (casket), para o drop do clue
		Matcher clue = CLUE_SCROLL_REGEX.matcher(message);
		if (clue.find())
		{
			String tier = clue.group("scrollType");
			String tierName = tier.isEmpty() ? tier : Character.toUpperCase(tier.charAt(0)) + tier.substring(1).toLowerCase();
			// N - 1 porque o onLoot soma 1
			killCounts.put("Clue Scroll (" + tierName + ")", Integer.parseInt(clue.group("scrollCount")) - 1);
			return;
		}

		BossCount parsed = parseBoss(message);
		if (parsed == null)
		{
			return;
		}
		String boss = parsed.boss;
		int kc = parsed.count;

		// Guarda N - 1, porque o evento de drop soma 1: N - 1 + 1 == N
		String cacheKey = getCacheKey(LootRecordType.UNKNOWN, boss);
		killCounts.asMap().merge(cacheKey, kc - 1, Math::max);

		if (boss.equals("Araxxor") || boss.equals(GAUNTLET_BOSS) || boss.equals(CG_BOSS) || boss.startsWith(TOA)
			|| boss.startsWith(TOB) || boss.startsWith(COX) || boss.equals(TFA))
		{
			// Guarda o nome da mensagem para separar os modos da raid no drop
			lastDropSource = boss;
			if (!isPluginDisabled(RL_LOOT_PLUGIN_NAME))
			{
				// O onLoot já vai somar 1
				return;
			}
		}

		// Não dá para saber se a mensagem veio antes ou depois do drop. Depois que o drop passou, grava N.
		executor.schedule(() -> killCounts.asMap().merge(cacheKey, kc, Math::max), 15, TimeUnit.SECONDS);
	}

	/** KC no momento do drop, ou null se não houver como saber. */
	@Nullable
	public Integer getKillCount(LootRecordType type, String sourceName)
	{
		if (sourceName == null)
		{
			return null;
		}
		Integer stored = getStoredKillCount(type, sourceName);
		if (stored != null)
		{
			return killCounts.asMap().merge(getCacheKey(type, sourceName), stored, Math::max);
		}
		return killCounts.getIfPresent(getCacheKey(type, sourceName));
	}

	/** Nome da fonte do drop, com o modo da raid e o nome certo do Gauntlet. */
	public String getStandardizedSource(LootReceived event)
	{
		if (GAUNTLET_BOSS.equals(event.getName()))
		{
			return THE_GAUNTLET;
		}
		if (CG_BOSS.equals(event.getName()))
		{
			return CG_NAME;
		}
		if (lastDropSource != null && shouldUseChatName(event))
		{
			return lastDropSource;
		}
		return event.getName();
	}

	private boolean shouldUseChatName(LootReceived event)
	{
		if (event.getType() != LootRecordType.EVENT)
		{
			return false;
		}
		String lastSource = lastDropSource;
		String name = event.getName();
		return lastSource != null && (TOA.equals(name) || TOB.equals(name) || COX.equals(name)) && lastSource.startsWith(name);
	}

	private void incrementKills(LootRecordType type, String sourceName)
	{
		killCounts.asMap().compute(getCacheKey(type, sourceName), (key, cachedKc) ->
		{
			if (cachedKc != null)
			{
				return cachedKc + 1;
			}
			// Primeira kill na sessão: pega a KC salva pelo RuneLite
			Integer kc = getStoredKillCount(type, sourceName);
			return kc != null ? kc + 1 : null;
		});
		lastDropSource = sourceName;
	}

	/** KC salva pelos plugins do RuneLite (Chat Commands ou Loot Tracker), ou null. */
	@Nullable
	private Integer getStoredKillCount(LootRecordType type, String sourceName)
	{
		if (!isPluginDisabled(RL_CHAT_CMD_PLUGIN_NAME))
		{
			Integer kc = configManager.getRSProfileConfiguration("killcount", cleanBossName(sourceName), int.class);
			if (kc != null)
			{
				// Menos 1 porque a mensagem de KC costuma chegar antes do drop
				return kc - 1;
			}
		}
		return getLootTrackerKills(type, sourceName);
	}

	@Nullable
	private Integer getLootTrackerKills(LootRecordType type, String sourceName)
	{
		if (isPluginDisabled(RL_LOOT_PLUGIN_NAME))
		{
			// Com o Loot Tracker desligado, a contagem salva não vale
			return null;
		}
		String json = configManager.getConfiguration(LootTrackerConfig.GROUP, configManager.getRSProfileKey(),
			"drops_" + type + "_" + sourceName);
		if (json == null)
		{
			// Nada salvo: primeira kill
			return 0;
		}
		try
		{
			LootTrackerRecord record = gson.fromJson(json, LootTrackerRecord.class);
			return record != null ? record.kills : null;
		}
		catch (JsonSyntaxException e)
		{
			log.debug("OldVicios: failed to read kills from the loot tracker config", e);
			return null;
		}
	}

	/** Só o que interessa do registro salvo pelo Loot Tracker do RuneLite (ConfigLoot). */
	private static final class LootTrackerRecord
	{
		int kills;
	}

	private boolean isPluginDisabled(String simpleLowerClassName)
	{
		return "false".equals(configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, simpleLowerClassName));
	}

	/** Nome do boss como o Chat Commands do RuneLite salva (minúsculo, com algumas exceções). */
	static String cleanBossName(String boss)
	{
		if (THE_GAUNTLET.equalsIgnoreCase(boss) || GAUNTLET_BOSS.equals(boss))
		{
			return "gauntlet";
		}
		if (CG_BOSS.equals(boss))
		{
			return "corrupted gauntlet";
		}
		if ("The Leviathan".equalsIgnoreCase(boss))
		{
			return "leviathan";
		}
		if ("The Whisperer".equalsIgnoreCase(boss))
		{
			return "whisperer";
		}
		if ("The Hueycoatl".equalsIgnoreCase(boss))
		{
			return "hueycoatl";
		}
		if (boss.startsWith("Barrows"))
		{
			return "barrows chests";
		}
		if (boss.endsWith("Hallowed Sepulchre)"))
		{
			return "hallowed sepulchre";
		}
		if (boss.endsWith("Tempoross)"))
		{
			return "tempoross";
		}
		if (boss.endsWith("Wintertodt)"))
		{
			return "wintertodt";
		}
		return boss.toLowerCase().replace(":", "");
	}

	private static String getCacheKey(LootRecordType type, String sourceName)
	{
		switch (type)
		{
			case PICKPOCKET:
				return "pickpocket_" + sourceName;
			case PLAYER:
				return "player_" + sourceName;
			default:
				// Nome do boss na mensagem diferente do nome do NPC
				if ("Whisperer".equals(sourceName))
				{
					return "The Whisperer";
				}
				if (THE_GAUNTLET.equals(sourceName))
				{
					return GAUNTLET_BOSS;
				}
				if (CG_NAME.equals(sourceName))
				{
					return CG_BOSS;
				}
				return sourceName;
		}
	}

	/** Lê "Your Vorkath kill count is: 150." ou "Your completed Chambers of Xeric count is: 50."; null se não for KC. */
	@Nullable
	static BossCount parseBoss(String message)
	{
		Matcher primary = PRIMARY_REGEX.matcher(message);
		if (primary.find())
		{
			return result(parsePrimaryBoss(primary.group("key"), primary.group("type")), primary.group("value"));
		}
		Matcher secondary = SECONDARY_REGEX.matcher(message);
		if (secondary.find())
		{
			return result(parseSecondary(secondary.group("key")), secondary.group("value"));
		}
		return null;
	}

	@Nullable
	private static BossCount result(@Nullable String boss, String count)
	{
		if (boss == null)
		{
			return null;
		}
		try
		{
			return new BossCount(boss, Integer.parseInt(count.replace(",", "")));
		}
		catch (NumberFormatException e)
		{
			log.debug("OldVicios: failed to parse kill count [{}] for boss [{}]", count, boss);
			return null;
		}
	}

	@Nullable
	private static String parsePrimaryBoss(String boss, String type)
	{
		switch (type.toLowerCase())
		{
			case "chest":
				if ("Barrows".equalsIgnoreCase(boss))
				{
					return boss;
				}
				if ("Lunar".equals(boss))
				{
					return boss + " " + type;
				}
				return null;

			case "completion":
				if (GAUNTLET_NAME.equalsIgnoreCase(boss))
				{
					return GAUNTLET_BOSS;
				}
				if (CG_NAME.equalsIgnoreCase(boss))
				{
					return CG_BOSS;
				}
				return null;

			case "harvest":
				if ("Herbiboar".equalsIgnoreCase(boss))
				{
					return "Herbiboar";
				}
				// fall through, como no Dink
			case "kill":
			case "success":
			case "opened":
			case "lap": // cursos de agility
			case "total ticket": // Brimhaven Agility Arena
				return boss;

			default:
				return null;
		}
	}

	@Nullable
	private static String parseSecondary(String boss)
	{
		if (boss == null || "Wintertodt".equalsIgnoreCase(boss) || TFA.equalsIgnoreCase(boss))
		{
			return boss;
		}
		int modeSeparator = boss.lastIndexOf(':');
		String raid = modeSeparator > 0 ? boss.substring(0, modeSeparator) : boss;
		if (raid.equalsIgnoreCase(TOB) || raid.equalsIgnoreCase(TOA) || raid.equalsIgnoreCase(COX)
			|| raid.equalsIgnoreCase(COX + " Challenge Mode"))
		{
			return boss;
		}
		return null;
	}
}
