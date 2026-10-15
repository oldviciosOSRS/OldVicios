package com.oldvicios;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WorldType;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.http.api.loottracker.LootRecordType;

@Slf4j
@PluginDescriptor(
	name = "OldVicios",
	description = "Plugin oficial do clã OldVicios",
	tags = {"clan", "oldvicios", "discord", "drops", "pets", "pk"}
)
public class OldViciosPlugin extends Plugin
{
	// Mundos que não contam para o clã (contas temporárias / sem progresso real)
	private static final Set<WorldType> IGNORED_WORLDS = EnumSet.of(
		WorldType.SEASONAL,
		WorldType.DEADMAN,
		WorldType.TOURNAMENT_WORLD,
		WorldType.BETA_WORLD,
		WorldType.NOSAVE_MODE,
		WorldType.FRESH_START_WORLD,
		WorldType.LAST_MAN_STANDING,
		WorldType.PVP_ARENA
	);

	private static final String PET_FOLLOWING = "You have a funny feeling like you're being followed.";
	private static final String PET_BACKPACK = "You feel something weird sneaking into your backpack.";

	private static final long DEFAULT_MIN_LOOT_VALUE = 3_000_000L;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OldViciosConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ItemManager itemManager;

	@Inject
	private DrawManager drawManager;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private OldViciosApi api;

	private OldViciosPanel panel;
	private NavigationButton navButton;

	private boolean authPending;
	private String accountId;
	private volatile long minLootValue = DEFAULT_MIN_LOOT_VALUE;

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

		if (client.getGameState() == GameState.LOGGED_IN)
		{
			authPending = true;
		}
		else
		{
			setStatus("Entre no jogo para conectar");
		}
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		navButton = null;
		panel = null;
		authPending = false;
		accountId = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			authPending = true;
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			authPending = false;
			accountId = null;
			setStatus("Entre no jogo para conectar");
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		// O accountHash e o nome só ficam disponíveis alguns ticks depois do login
		if (!authPending || client.getAccountHash() == -1 || client.getLocalPlayer() == null
			|| client.getLocalPlayer().getName() == null)
		{
			return;
		}
		authPending = false;
		accountId = OldViciosApi.accountId(client.getAccountHash());
		authenticate(client.getLocalPlayer().getName());
	}

	private void authenticate(String playerName)
	{
		setStatus("Conectando...");
		api.authenticate(accountId, playerName, result ->
		{
			if (result.ok())
			{
				if (result.data.has("minLootValue"))
				{
					minLootValue = result.data.get("minLootValue").getAsLong();
				}
				setStatus("Conectado como " + result.data.get("playerName").getAsString());
				return;
			}
			setStatus(authErrorMessage(result));
		});
	}

	private static String authErrorMessage(OldViciosApi.Result result)
	{
		if (result.status == 403)
		{
			return "Personagem não encontrado no grupo do OldVicios (WOM)";
		}
		if (result.status == 409)
		{
			return "Conta em revisão pela staff";
		}
		return "Servidor indisponível, tente relogar mais tarde";
	}

	@Subscribe
	public void onNpcLootReceived(NpcLootReceived event)
	{
		if (config.sendDrops())
		{
			handleLoot(event.getNpc().getName(), event.getItems(), false);
		}
	}

	@Subscribe
	public void onPlayerLootReceived(PlayerLootReceived event)
	{
		if (config.sendPkDrops())
		{
			handleLoot(event.getPlayer().getName(), event.getItems(), true);
		}
	}

	@Subscribe
	public void onLootReceived(LootReceived event)
	{
		// Baús e raids (CoX, ToB, ToA, Barrows...). NPC e PK já chegam pelos eventos acima.
		if (event.getType() == LootRecordType.EVENT && config.sendDrops())
		{
			handleLoot(event.getName(), event.getItems(), false);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE || !config.sendDrops() || !canSubmit())
		{
			return;
		}
		String message = event.getMessage();
		if (PET_FOLLOWING.equals(message) || PET_BACKPACK.equals(message))
		{
			JsonObject payload = new JsonObject();
			payload.addProperty("type", "PET");
			payload.addProperty("playerName", client.getLocalPlayer().getName());
			submitWithScreenshot(payload, "Pet enviado ao Discord do OldVicios!");
		}
	}

	private boolean canSubmit()
	{
		if (accountId == null || client.getLocalPlayer() == null)
		{
			return false;
		}
		for (WorldType type : client.getWorldType())
		{
			if (IGNORED_WORLDS.contains(type))
			{
				return false;
			}
		}
		return true;
	}

	private void handleLoot(String source, Collection<ItemStack> stacks, boolean pk)
	{
		if (!canSubmit())
		{
			return;
		}

		// Agrupa itens repetidos e soma o valor (GE)
		Map<Integer, int[]> quantities = new LinkedHashMap<>();
		for (ItemStack stack : stacks)
		{
			int id = itemManager.canonicalize(stack.getId());
			quantities.computeIfAbsent(id, k -> new int[1])[0] += stack.getQuantity();
		}

		long total = 0;
		JsonArray items = new JsonArray();
		for (Map.Entry<Integer, int[]> entry : quantities.entrySet())
		{
			int id = entry.getKey();
			int quantity = entry.getValue()[0];
			long price = itemManager.getItemPrice(id);
			total += price * quantity;

			JsonObject item = new JsonObject();
			item.addProperty("id", id);
			item.addProperty("name", itemManager.getItemComposition(id).getName());
			item.addProperty("quantity", quantity);
			item.addProperty("priceEach", price);
			items.add(item);
		}

		if (total < minLootValue)
		{
			return;
		}

		JsonObject payload = new JsonObject();
		payload.addProperty("type", "LOOT");
		payload.addProperty("playerName", client.getLocalPlayer().getName());
		payload.addProperty("pk", pk);
		if (source != null)
		{
			payload.addProperty("source", source);
		}
		payload.add("items", items);
		submitWithScreenshot(payload, pk ? "Loot PK enviado ao Discord do OldVicios!" : "Drop enviado ao Discord do OldVicios!");
	}

	private void submitWithScreenshot(JsonObject payload, String successMessage)
	{
		String id = accountId;
		drawManager.requestNextFrameListener(image ->
			executor.execute(() ->
			{
				byte[] png = toPng(ImageUtil.bufferedImageFromImage(image));
				api.submitLoot(id, payload, png, result ->
				{
					if (result.ok() && result.data.has("sent") && result.data.get("sent").getAsBoolean())
					{
						clientThread.invokeLater(() ->
							client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", successMessage, null));
					}
					else if (!result.ok())
					{
						log.debug("OldVicios loot submit failed: status={} error={}", result.status, result.error);
					}
				});
			}));
	}

	private static byte[] toPng(BufferedImage image)
	{
		try (ByteArrayOutputStream out = new ByteArrayOutputStream())
		{
			ImageIO.write(image, "png", out);
			return out.toByteArray();
		}
		catch (IOException e)
		{
			log.debug("OldVicios screenshot encode failed", e);
			return null;
		}
	}

	private void setStatus(String text)
	{
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
