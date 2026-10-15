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
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "OldVicios",
	description = "Plugin oficial do clã Old Vicios",
	tags = {"clan", "old vicios", "discord", "drops", "pets"}
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
			.tooltip("Old Vicios")
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

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (OldViciosConfig.GROUP.equals(event.getGroup()))
		{
			updateStatus();
		}
	}

	private void updateStatus()
	{
		String text;
		if (!config.connect())
		{
			text = "Desconectado";
		}
		else if (client.getGameState() != GameState.LOGGED_IN)
		{
			text = "Entre no jogo para conectar";
		}
		else
		{
			// TODO: autenticar no backend (accountHash) — próximo passo
			text = "Conectado (ainda sem servidor)";
		}

		OldViciosPanel p = panel;
		if (p != null)
		{
			SwingUtilities.invokeLater(() -> p.setStatus(text));
		}
	}

	private static BufferedImage icon()
	{
		// TODO: trocar pelo ícone do Old Vicios
		return ImageUtil.loadImageResource(OldViciosPlugin.class, "icon.png");
	}

	@Provides
	OldViciosConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(OldViciosConfig.class);
	}
}
