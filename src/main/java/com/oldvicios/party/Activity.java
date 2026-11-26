package com.oldvicios.party;

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

	@Override
	public String toString()
	{
		return displayName;
	}
}
