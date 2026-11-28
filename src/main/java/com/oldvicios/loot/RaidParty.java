/*
 * Adapted from Dink (https://github.com/pajlads/DinkPlugin): Utils.getBossParty and the party readers.
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

import javax.annotation.Nullable;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;

/**
 * Tamanho da party numa raid, lido do cliente como o Dink faz. Só o número sai do plugin: os nomes dos
 * outros jogadores nunca são enviados (regra do Plugin Hub). Roda na client thread.
 */
public final class RaidParty
{
	private static final int TOA_PARTY_MAX_SIZE = 8, TOB_PARTY_MAX_SIZE = 5, TFA_PARTY_MAX_SIZE = 8;

	private RaidParty()
	{
	}

	/** Quantos jogadores estão na raid de onde veio o drop; null fora de raid ou se não deu para ler. */
	@Nullable
	public static Integer size(Client client, String source)
	{
		int size;
		switch (source)
		{
			case "Chambers of Xeric":
			case "Chambers of Xeric Challenge Mode":
				size = xericChambersParty(client);
				break;
			case "Tombs of Amascut":
			case "Tombs of Amascut: Entry Mode":
			case "Tombs of Amascut: Expert Mode":
				size = varcStrings(client, VarClientID.TOA_CLIENT_NAME0, TOA_PARTY_MAX_SIZE);
				break;
			case "Theatre of Blood":
			case "Theatre of Blood: Entry Mode":
			case "Theatre of Blood: Hard Mode":
				size = varcStrings(client, VarClientID.TOB_CLIENT_NAME0, TOB_PARTY_MAX_SIZE);
				break;
			case "Fractured Archive":
			case "The Fractured Archive":
				size = varcStrings(client, VarClientID.TFA_CLIENT_NAME0, TFA_PARTY_MAX_SIZE);
				break;
			default:
				return null;
		}
		return size > 0 ? size : null;
	}

	/** CoX: nomes no painel lateral da raid. */
	private static int xericChambersParty(Client client)
	{
		Widget widget = client.getWidget(InterfaceID.RaidsSidepanel.LIST);
		Widget[] children = widget != null ? widget.getChildren() : null;
		if (children == null)
		{
			return 0;
		}
		int count = 0;
		for (Widget child : children)
		{
			if (child != null && !sanitize(child.getName()).isEmpty())
			{
				count++;
			}
		}
		return count;
	}

	/** ToB, ToA e Fractured Archive: o jogo guarda o nome de cada membro numa varc seguida da outra. */
	private static int varcStrings(Client client, int initialVarcId, int maxSize)
	{
		int count = 0;
		for (int i = 0; i < maxSize; i++)
		{
			String name = client.getVarcStrValue(initialVarcId + i);
			if (name != null && !name.isEmpty())
			{
				count++;
			}
		}
		return count;
	}

	private static String sanitize(String str)
	{
		if (str == null || str.isEmpty())
		{
			return "";
		}
		return Text.removeTags(str.replace("<br>", "\n")).replace(' ', ' ').trim();
	}
}
