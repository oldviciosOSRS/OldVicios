package com.oldvicios;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WorldType;
import net.runelite.api.clan.ClanMember;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
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
import net.runelite.client.util.Text;
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

	// "Your Maggot King kill count is: 149." / "Your completed Chambers of Xeric count is: 50."
	static final Pattern KILL_COUNT = Pattern.compile(
		"^Your (?:completed )?(?<name>.+?) (?:kill |chest |completion |harvest |lap |success )?count is: (?<kc>[\\d,]+)");
	// A mensagem de KC chega logo antes do drop
	private static final int KILL_COUNT_MAX_AGE_TICKS = 5;

	private static final long DEFAULT_MIN_LOOT_VALUE = 3_000_000L;
	private static final long CONTENT_REFRESH_MINUTES = 5;
	private static final long UPDATES_POLL_SECONDS = 45;

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

	@Inject
	private Notifier notifier;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ChatIconManager chatIconManager;

	private OldViciosPanel panel;
	private NavigationButton navButton;
	private ScheduledFuture<?> contentRefresh;
	private ScheduledFuture<?> updatesPoll;
	// Última notificação já vista (null = ainda não sabemos; a 1ª consulta só pega o cursor)
	private volatile Integer updatesCursor;
	// Imagens dos cards: baixadas uma vez por sessão
	private final Map<String, BufferedImage> imageCache = new ConcurrentHashMap<>();

	private boolean authPending;
	// Nick conectado; o cabeçalho mostra "[cargo no CC] nick" assim que o clã carregar
	private volatile String connectedName;
	private boolean headerPending;
	private String accountId;
	private volatile long minLootValue = DEFAULT_MIN_LOOT_VALUE;
	private int lastKillCount;
	private int lastKillCountTick = -1;

	@Override
	protected void startUp()
	{
		panel = new OldViciosPanel(this::loadImage);
		addNavigationButton();
		contentRefresh = executor.scheduleWithFixedDelay(this::refreshContent, 0, CONTENT_REFRESH_MINUTES, TimeUnit.MINUTES);
		updatesPoll = executor.scheduleWithFixedDelay(this::pollUpdates, 0, UPDATES_POLL_SECONDS, TimeUnit.SECONDS);

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
		if (contentRefresh != null)
		{
			contentRefresh.cancel(false);
			contentRefresh = null;
		}
		if (updatesPoll != null)
		{
			updatesPoll.cancel(false);
			updatesPoll = null;
		}
		updatesCursor = null;
		clientToolbar.removeNavigation(navButton);
		navButton = null;
		panel = null;
		imageCache.clear();
		authPending = false;
		accountId = null;
	}

	private void addNavigationButton()
	{
		navButton = NavigationButton.builder()
			.tooltip("OldVicios")
			.icon(icon())
			.priority(config.sidebarPriority())
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		// Move o ícone na barra lateral na hora, sem reiniciar o plugin
		if (OldViciosConfig.GROUP.equals(event.getGroup()) && "sidebarPriority".equals(event.getKey()) && navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			addNavigationButton();
		}
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
			connectedName = null;
			headerPending = false;
			authPending = false;
			accountId = null;
			setStatus("Entre no jogo para conectar");
		}
	}

	@Subscribe
	public void onClanChannelChanged(ClanChannelChanged event)
	{
		if (connectedName != null)
		{
			updateHeader();
		}
	}

	/** Cabeçalho "[ícone do cargo no CC] nick". Roda na client thread; tenta de novo se o clã não carregou. */
	private void updateHeader()
	{
		String name = connectedName;
		if (name == null)
		{
			return;
		}
		ClanSettings settings = client.getClanSettings();
		ClanMember member = settings != null ? settings.findMember(name) : null;
		ClanTitle title = member != null ? settings.titleForRank(member.getRank()) : null;
		if (title == null)
		{
			headerPending = true;
			onPanel(p -> p.setPlayer(null, null, name));
			return;
		}
		BufferedImage icon = chatIconManager.getRankImage(title);
		headerPending = icon == null;
		onPanel(p -> p.setPlayer(icon, title.getName(), name));
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (headerPending && client.getTickCount() % 5 == 0)
		{
			updateHeader();
		}

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
				connectedName = result.data.get("playerName").getAsString();
				clientThread.invokeLater(this::updateHeader);
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
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}
		String message = Text.removeTags(event.getMessage());

		Matcher kc = KILL_COUNT.matcher(message);
		if (kc.find())
		{
			lastKillCount = Integer.parseInt(kc.group("kc").replace(",", ""));
			lastKillCountTick = client.getTickCount();
			return;
		}

		if (!config.sendDrops() || !canSubmit())
		{
			return;
		}
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

		// Igual ao Dink (e ao backend): só vale se algum item, sozinho, atingir o mínimo
		long highestStack = 0;
		JsonArray items = new JsonArray();
		for (Map.Entry<Integer, int[]> entry : quantities.entrySet())
		{
			int id = entry.getKey();
			int quantity = entry.getValue()[0];
			long price = itemManager.getItemPrice(id);
			highestStack = Math.max(highestStack, price * quantity);

			JsonObject item = new JsonObject();
			item.addProperty("id", id);
			item.addProperty("name", itemManager.getItemComposition(id).getName());
			item.addProperty("quantity", quantity);
			item.addProperty("priceEach", price);
			items.add(item);
		}

		// Loot PK não tem mínimo
		if (!pk && highestStack < minLootValue)
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
		payload.addProperty("world", client.getWorld());
		payload.addProperty("template", config.lootMessage());
		if (!pk && lastKillCountTick >= 0 && client.getTickCount() - lastKillCountTick <= KILL_COUNT_MAX_AGE_TICKS)
		{
			payload.addProperty("killCount", lastKillCount);
		}
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

	private void refreshContent()
	{
		api.fetchCards("notices", cards -> onPanel(p -> p.setNotices(cards)));
		api.fetchCards("events", cards -> onPanel(p -> p.setEvents(cards)));
	}

	private void pollUpdates()
	{
		Integer since = updatesCursor;
		api.fetchUpdates(accountId, since, result ->
		{
			if (!result.ok())
			{
				return;
			}
			updatesCursor = result.data.get("cursor").getAsInt();
			if (since == null || !result.data.has("messages"))
			{
				return;
			}
			boolean refresh = false;
			for (JsonElement element : result.data.getAsJsonArray("messages"))
			{
				JsonObject message = element.getAsJsonObject();
				String kind = message.get("kind").getAsString();
				String title = message.get("title").getAsString();
				String text = message.has("text") && !message.get("text").isJsonNull() ? message.get("text").getAsString() : null;
				notify(title, text);
				refresh |= "EVENT".equals(kind) || "NOTICE".equals(kind);
			}
			if (refresh)
			{
				refreshContent();
			}
		});
	}

	private void notify(String title, String text)
	{
		String line = text != null ? title + " - " + text : title;
		clientThread.invokeLater(() ->
		{
			if (config.chatNotifications() && client.getGameState() == GameState.LOGGED_IN)
			{
				chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.CONSOLE)
					.runeLiteFormattedMessage(new ChatMessageBuilder()
						.append(ChatColorType.HIGHLIGHT)
						.append("[OldVicios] ")
						.append(ChatColorType.NORMAL)
						.append(line)
						.build())
					.build());
			}
			notifier.notify(config.notification(), "OldVicios: " + line);
		});
	}

	/** Entrega a imagem na EDT, do cache ou baixando. */
	private void loadImage(String url, Consumer<BufferedImage> callback)
	{
		BufferedImage cached = imageCache.get(url);
		if (cached != null)
		{
			callback.accept(cached);
			return;
		}
		api.fetchImage(url, image ->
		{
			if (image != null)
			{
				imageCache.put(url, image);
				SwingUtilities.invokeLater(() -> callback.accept(image));
			}
		});
	}

	private void onPanel(Consumer<OldViciosPanel> action)
	{
		OldViciosPanel p = panel;
		if (p != null)
		{
			SwingUtilities.invokeLater(() -> action.accept(p));
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
