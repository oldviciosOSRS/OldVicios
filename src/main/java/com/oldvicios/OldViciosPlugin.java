package com.oldvicios;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "OldVicios",
	description = "Plugin oficial do clã OldVicios",
	tags = {"clan", "oldvicios", "discord", "drops", "pets", "pk"}
)
public class OldViciosPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private OldViciosConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	private OldViciosPanel panel;
	private NavigationButton navButton;

	@Override
	protected void startUp()
	{
		panel = new OldViciosPanel();
		navButton = NavigationButton.builder()
			.tooltip("OldVicios")
			.icon(icon())
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		updateStatus();
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		navButton = null;
		panel = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN || event.getGameState() == GameState.LOGIN_SCREEN)
		{
			updateStatus();
		}
	}

	private void updateStatus()
	{
		// TODO: autenticar no backend (accountHash) — próximo passo
		String text = client.getGameState() == GameState.LOGGED_IN
			? "Conectado (ainda sem servidor)"
			: "Entre no jogo para conectar";

		OldViciosPanel p = panel;
		if (p != null)
		{
			SwingUtilities.invokeLater(() -> p.setStatus(text));
		}
	}

	private static BufferedImage icon()
	{
		// TODO: trocar pelo ícone do OldVicios
		return ImageUtil.loadImageResource(OldViciosPlugin.class, "icon.png");
	}

	@Provides
	OldViciosConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(OldViciosConfig.class);
	}
}
