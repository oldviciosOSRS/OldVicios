package com.oldvicios.party;

import java.util.function.Consumer;
import net.runelite.client.hiscore.HiscoreResult;

/**
 * O que a aba Grupos precisa do plugin: dados do jogador logado (lidos na client thread), o hiscore
 * (para o KC) e os avisos no chat. Os callbacks são entregues na Swing EDT.
 */
public interface PartyContext
{
	/** Combate, modo de jogo e mundo do jogador logado; null se não estiver no jogo. */
	void localPlayer(Consumer<LocalPlayer> callback);

	/** Hiscore oficial do jogador (null se falhar ou se ele não aparecer no hiscore). */
	void hiscores(String playerName, Consumer<HiscoreResult> callback);

	/** Aviso no chat do jogo / notificação do RuneLite. */
	void notify(String message);

	/** Dados do próprio cliente, sem o jogador digitar nada. */
	final class LocalPlayer
	{
		final int combatLevel;
		final AccountType accountType;
		final int world;

		public LocalPlayer(int combatLevel, net.runelite.api.vars.AccountType accountType, int world)
		{
			this.combatLevel = combatLevel;
			// Mesmos 6 valores do RuneLite: mapeamento direto pelo nome
			this.accountType = accountType != null ? AccountType.valueOf(accountType.name()) : AccountType.NORMAL;
			this.world = world;
		}
	}
}
