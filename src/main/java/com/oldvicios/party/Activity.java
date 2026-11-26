package com.oldvicios.party;

import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.Skill;

/**
 * Atividades que aceitam grupo. Vagas: mínimo 2 (dono + 1), máximo = limite do jogo.
 * Onde o jogo não tem limite (Corp, GWD, skilling em massa) usamos 20 provisoriamente.
 */
enum Activity
{
	CHAMBERS_OF_XERIC("Chambers of Xeric", 2, 100, "Normal + CM", "Normal", "Challenge Mode"),
	THEATRE_OF_BLOOD("Theatre of Blood", 2, 5, "Normal + Hard", "Entry", "Normal", "Hard Mode"),
	TOMBS_OF_AMASCUT("Tombs of Amascut", 2, 8, "Normal + Expert", "Entry", "Normal", "Expert"),
	THE_FRACTURED_ARCHIVE("The Fractured Archive", 2, 8, "KC"),
	YAMA("Yama", 2, 2, "KC"),
	ROYAL_TITANS("Royal Titans", 2, 2, "KC"),
	NEX("Nex", 2, 60, "KC"),
	THE_NIGHTMARE("The Nightmare", 2, 80, "KC"),
	THE_HUEYCOATL("The Hueycoatl", 2, 20, "KC"),
	CORPOREAL_BEAST("Corporeal Beast", 2, 20, "KC"),
	GOD_WARS("God Wars Dungeon", 2, 20, "KC", "General Graardor", "Commander Zilyana", "Kree'arra", "K'ril Tsutsaroth"),
	ZALCANO("Zalcano", 2, 20, "KC"),
	WINTERTODT("Wintertodt", 2, 20, "KC"),
	TEMPOROSS("Tempoross", 2, 20, "KC"),
	GUARDIANS_OF_THE_RIFT("Guardians of the Rift", 2, 20, "Rifts closed"),
	BARBARIAN_ASSAULT("Barbarian Assault", 5, 5, null),
	VOLCANIC_MINE("Volcanic Mine", 2, 50, null);

	final String displayName;
	final int minSlots;
	final int maxSlots;
	/** O que o requisito de KC soma no hiscore. null = atividade sem KC no hiscore. */
	final String kcLabel;
	/** Modos que o líder escolhe. Vazio = atividade sem modos. */
	final String[] modes;

	Activity(String displayName, int minSlots, int maxSlots, String kcLabel, String... modes)
	{
		this.displayName = displayName;
		this.minSlots = minSlots;
		this.maxSlots = maxSlots;
		this.kcLabel = kcLabel;
		this.modes = modes;
	}

	/**
	 * Entradas do hiscore que somam o KC da atividade (o mesmo que o backend chama de "Normal + CM" etc.).
	 * Vazio = atividade sem KC no hiscore. Na GWD o modo escolhido é o boss.
	 */
	HiscoreSkill[] killCountSkills(String mode)
	{
		switch (this)
		{
			case CHAMBERS_OF_XERIC:
				return new HiscoreSkill[]{HiscoreSkill.CHAMBERS_OF_XERIC, HiscoreSkill.CHAMBERS_OF_XERIC_CHALLENGE_MODE};
			case THEATRE_OF_BLOOD:
				return new HiscoreSkill[]{HiscoreSkill.THEATRE_OF_BLOOD, HiscoreSkill.THEATRE_OF_BLOOD_HARD_MODE};
			case TOMBS_OF_AMASCUT:
				return new HiscoreSkill[]{HiscoreSkill.TOMBS_OF_AMASCUT, HiscoreSkill.TOMBS_OF_AMASCUT_EXPERT};
			case THE_FRACTURED_ARCHIVE:
				return new HiscoreSkill[]{HiscoreSkill.THE_FRACTURED_ARCHIVE};
			case YAMA:
				return new HiscoreSkill[]{HiscoreSkill.YAMA};
			case ROYAL_TITANS:
				return new HiscoreSkill[]{HiscoreSkill.THE_ROYAL_TITANS};
			case NEX:
				return new HiscoreSkill[]{HiscoreSkill.NEX};
			case THE_NIGHTMARE:
				return new HiscoreSkill[]{HiscoreSkill.NIGHTMARE};
			case THE_HUEYCOATL:
				return new HiscoreSkill[]{HiscoreSkill.THE_HUEYCOATL};
			case CORPOREAL_BEAST:
				return new HiscoreSkill[]{HiscoreSkill.CORPOREAL_BEAST};
			case GOD_WARS:
				if ("General Graardor".equals(mode))
				{
					return new HiscoreSkill[]{HiscoreSkill.GENERAL_GRAARDOR};
				}
				if ("Commander Zilyana".equals(mode))
				{
					return new HiscoreSkill[]{HiscoreSkill.COMMANDER_ZILYANA};
				}
				if ("Kree'arra".equals(mode))
				{
					return new HiscoreSkill[]{HiscoreSkill.KREEARRA};
				}
				if ("K'ril Tsutsaroth".equals(mode))
				{
					return new HiscoreSkill[]{HiscoreSkill.KRIL_TSUTSAROTH};
				}
				return new HiscoreSkill[]{HiscoreSkill.GENERAL_GRAARDOR, HiscoreSkill.COMMANDER_ZILYANA,
					HiscoreSkill.KREEARRA, HiscoreSkill.KRIL_TSUTSAROTH};
			case ZALCANO:
				return new HiscoreSkill[]{HiscoreSkill.ZALCANO};
			case WINTERTODT:
				return new HiscoreSkill[]{HiscoreSkill.WINTERTODT};
			case TEMPOROSS:
				return new HiscoreSkill[]{HiscoreSkill.TEMPOROSS};
			case GUARDIANS_OF_THE_RIFT:
				return new HiscoreSkill[]{HiscoreSkill.RIFTS_CLOSED};
			default:
				return new HiscoreSkill[0];
		}
	}

	/**
	 * KC do jogador nesta atividade, somando as entradas do hiscore. null = hiscore ainda não carregou,
	 * o jogador não está nele, ou a atividade não tem KC. Entrada sem valor (-1) conta como 0.
	 */
	Integer killCount(HiscoreResult hiscores, String mode)
	{
		HiscoreSkill[] skills = killCountSkills(mode);
		if (hiscores == null || skills.length == 0)
		{
			return null;
		}
		int total = 0;
		for (HiscoreSkill skill : skills)
		{
			Skill entry = hiscores.getSkill(skill);
			if (entry != null && entry.getLevel() > 0)
			{
				total += entry.getLevel();
			}
		}
		return total;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
