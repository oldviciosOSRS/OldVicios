package com.oldvicios;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class OldViciosPluginTest
{
	public static void main(String[] args) throws Exception
	{
		// OV_MOCK_USER=Nick ./gradlew run: abre o painel do clã com esse nick, sem login no jogo nem no backend
		String mockUser = System.getenv("OV_MOCK_USER");
		if (mockUser != null && !mockUser.isBlank())
		{
			OldViciosPlugin.previewName = mockUser.trim();
		}
		ExternalPluginManager.loadBuiltin(OldViciosPlugin.class);
		RuneLite.main(args);
	}
}
