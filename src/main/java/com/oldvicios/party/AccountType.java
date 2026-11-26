package com.oldvicios.party;

import net.runelite.api.gameval.SpriteID;

/**
 * Modo de jogo da conta, com o ícone original do jogo (os mesmos do chat). O sprite
 * ModIconsInterface._N corresponde ao IconID N (2 = Ironman, 3 = UIM, 10 = HCIM, 41 = GIM, 42 = HCGIM).
 * No plugin real vem de client.getAccountType() (net.runelite.api.vars.AccountType tem estes mesmos
 * 6 valores, então o mapeamento é direto); cada cliente manda o seu.
 */
enum AccountType
{
	NORMAL("Conta normal", -1),
	IRONMAN("Ironman", SpriteID.ModIconsInterface._2),
	ULTIMATE_IRONMAN("Ultimate Ironman", SpriteID.ModIconsInterface._3),
	HARDCORE_IRONMAN("Hardcore Ironman", SpriteID.ModIconsInterface._10),
	GROUP_IRONMAN("Group Ironman", SpriteID.ModIconsInterface._41),
	HARDCORE_GROUP_IRONMAN("Hardcore Group Ironman", SpriteID.ModIconsInterface._42);

	final String displayName;
	/** Sprite do ícone; -1 = sem ícone (conta normal). */
	final int spriteId;

	AccountType(String displayName, int spriteId)
	{
		this.displayName = displayName;
		this.spriteId = spriteId;
	}
}
