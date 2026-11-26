package com.oldvicios.party;

import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.ui.components.PluginErrorPanel;

/**
 * Aba "Grupos" do painel do OldVicios, ligada ao backend (/v1/plugin/party).
 *
 * - Enquanto o jogador está conectado, consulta GET /me a cada POLL_MS: papel, grupo e avisos.
 *   A lista e o detalhe só são consultados enquanto essas telas estão abertas.
 * - Combate, modo de jogo e mundo vêm do próprio cliente; o KC vem do hiscore (HiscoreClient).
 * - Quem está num grupo (líder ou aceito) só vê o próprio grupo; candidatos e quem não tem grupo veem a lista.
 * Todos os métodos rodam na Swing EDT.
 */
public class PartyFinderPanel extends JPanel
{
	private static final int POLL_MS = 15_000;
	private static final long HISCORE_TTL_MS = 10 * 60_000;
	/** Hiscore que falhou (fora do ar, nick novo): tenta de novo depois disso. */
	private static final long HISCORE_RETRY_MS = 60_000;

	private enum Screen
	{
		DISCONNECTED, LIST, CREATE, DETAILS, MANAGE, LOBBY
	}

	/** Requisito do grupo comparado com os dados do jogador. */
	private static final class Check
	{
		final String label;
		final String value;
		final boolean met;

		Check(String label, String value, boolean met)
		{
			this.label = label;
			this.value = value;
			this.met = met;
		}
	}

	private final PartyApi api;
	private final PartyContext context;
	private final Timer poll;
	/** Ícones de modo de jogo (iron, HCIM, UIM, GIM...), carregados do cache do jogo pelo SpriteManager. */
	private final Map<AccountType, BufferedImage> accountIcons = new EnumMap<>(AccountType.class);

	/** Nick conectado; null = desconectado (não consulta nada). */
	private String playerName;
	private PartyContext.LocalPlayer me;
	private HiscoreResult hiscores;
	private long hiscoresRequestedAt;
	/** Último GET /me. */
	private MyStatus status;
	private List<Party> openParties = Collections.emptyList();
	private boolean listLoaded;
	/** Grupo aberto na tela de detalhe. */
	private Party detail;
	/** null = todas as atividades. */
	private Activity filter;
	/** Uma ação por vez (evita clique duplo enquanto o servidor responde). */
	private boolean busy;

	private Screen screen = Screen.DISCONNECTED;
	/** Tela atual, para redesenhar quando o estado muda. */
	private Runnable currentScreen = this::showDisconnected;
	/** false na tela de criar grupo: redesenhar apagaria o que o jogador está digitando. */
	private boolean currentScreenRefreshable = true;

	public PartyFinderPanel(SpriteManager spriteManager, PartyApi api, PartyContext context)
	{
		super(new BorderLayout());
		setOpaque(false);
		this.api = api;
		this.context = context;
		poll = new Timer(POLL_MS, e -> poll());
		loadAccountIcons(spriteManager);
		showDisconnected();
	}

	/** Conectou (nick confirmado pelo backend): começa a consultar. */
	public void setPlayerName(String name)
	{
		if (!name.equals(playerName))
		{
			playerName = name;
			hiscores = null;
			hiscoresRequestedAt = 0;
			status = null;
			listLoaded = false;
			detail = null;
			showList();
		}
		poll.restart();
		poll();
	}

	/** Desconectou (deslogou ou perdeu o acesso): para de consultar e limpa a aba. */
	public void disconnect()
	{
		poll.stop();
		playerName = null;
		me = null;
		hiscores = null;
		status = null;
		openParties = Collections.emptyList();
		listLoaded = false;
		detail = null;
		showDisconnected();
	}

	public void shutdown()
	{
		poll.stop();
	}

	/** O SpriteManager entrega na client thread; guardamos e redesenhamos na EDT. */
	private void loadAccountIcons(SpriteManager spriteManager)
	{
		for (AccountType type : AccountType.values())
		{
			if (type.spriteId < 0)
			{
				continue;
			}
			spriteManager.getSpriteAsync(type.spriteId, 0, image -> SwingUtilities.invokeLater(() ->
			{
				accountIcons.put(type, image);
				refresh();
			}));
		}
	}

	private BufferedImage accountIcon(PartyMember m)
	{
		return accountIcons.get(m.accountType());
	}

	// ---------- Consulta ao servidor ----------

	private void poll()
	{
		if (playerName == null)
		{
			return;
		}
		context.localPlayer(info ->
		{
			boolean first = me == null && info != null;
			me = info;
			if (first)
			{
				refresh();
			}
		});
		loadHiscores();

		api.status(result -> SwingUtilities.invokeLater(() -> onStatus(result)));
		if (screen == Screen.LIST)
		{
			loadList();
		}
		else if (screen == Screen.DETAILS && detail != null)
		{
			loadDetail(detail.id);
		}
	}

	/** KC do hiscore, guardado por HISCORE_TTL_MS (o hiscore oficial não atualiza em tempo real mesmo). */
	private void loadHiscores()
	{
		long now = System.currentTimeMillis();
		long wait = hiscores == null ? HISCORE_RETRY_MS : HISCORE_TTL_MS;
		if (now - hiscoresRequestedAt < wait)
		{
			return;
		}
		hiscoresRequestedAt = now;
		String name = playerName;
		context.hiscores(name, result ->
		{
			if (result != null && name.equals(playerName))
			{
				hiscores = result;
				refresh();
			}
		});
	}

	private void onStatus(PartyApi.Result<MyStatus> result)
	{
		if (playerName == null || !result.ok() || result.data == null)
		{
			return;
		}
		MyStatus.Role before = status != null ? status.role() : null;
		status = result.data;
		for (MyStatus.Notice notice : status.notices())
		{
			String message = notice.message();
			if (message != null)
			{
				context.notify(message);
			}
		}

		// Mudou o papel (aceito, removido, grupo encerrado...): a raiz da aba muda junto.
		// Na tela de criar, não: apagaria o formulário (o Publicar volta para a raiz).
		if (before != status.role() && screen != Screen.CREATE)
		{
			showList();
		}
		else
		{
			refresh();
		}
	}

	private void loadList()
	{
		Activity requested = filter;
		api.list(requested, result -> SwingUtilities.invokeLater(() ->
		{
			if (!result.ok() || result.data == null || requested != filter)
			{
				return;
			}
			List<Party> parties = new ArrayList<>();
			for (Party p : result.data)
			{
				// Atividade que esta versão não conhece: não dá para mostrar direito
				if (p.activity != null)
				{
					parties.add(p);
				}
			}
			openParties = parties;
			listLoaded = true;
			if (screen == Screen.LIST)
			{
				refresh();
			}
		}));
	}

	private void loadDetail(String partyId)
	{
		api.get(partyId, result -> SwingUtilities.invokeLater(() ->
		{
			if (screen != Screen.DETAILS || detail == null || !detail.id.equals(partyId))
			{
				return;
			}
			if (result.ok() && result.data != null && result.data.activity != null)
			{
				detail = result.data;
				refresh();
			}
			else if (result.status == 404)
			{
				context.notify("O grupo \"" + detail.name + "\" não existe mais");
				detail = null;
				showList();
			}
		}));
	}

	/**
	 * Faz uma ação no servidor: {@code act(cb -> api.leave(cb::accept))}. Uma por vez (evita clique duplo); erro vira
	 * aviso. Depois de qualquer resposta, consulta o /me de novo para a tela refletir o estado do servidor.
	 */
	private void act(Consumer<Consumer<PartyApi.Result<?>>> call, Runnable onOk)
	{
		if (busy)
		{
			return;
		}
		busy = true;
		call.accept(result -> SwingUtilities.invokeLater(() ->
		{
			busy = false;
			if (result.ok())
			{
				onOk.run();
			}
			else
			{
				JOptionPane.showMessageDialog(this, errorMessage(result), "OldVicios", JOptionPane.WARNING_MESSAGE);
			}
			poll();
		}));
	}

	private void act(Consumer<Consumer<PartyApi.Result<?>>> call)
	{
		act(call, () -> { });
	}

	private static String errorMessage(PartyApi.Result<?> result)
	{
		if (result.status == 0)
		{
			return "Servidor indisponível. Tente de novo em instantes.";
		}
		String error = result.error != null ? result.error : "";
		switch (error)
		{
			case "already_in_party":
				return "Você já está em um grupo.";
			case "party_full":
				return "O grupo está cheio.";
			case "applications_closed":
				return "As inscrições deste grupo estão fechadas.";
			case "requirements_not_met":
				return "Você não cumpre os requisitos do grupo.";
			case "not_found":
				return "O grupo não existe mais.";
			case "not_applicant":
				return "Essa candidatura não existe mais.";
			case "not_member":
				return "Esse jogador não está mais no grupo.";
			case "not_leader":
				return "Só o líder pode fazer isso.";
			case "too_many_parties":
				return "Muitos grupos abertos agora. Tente mais tarde.";
			case "too_many_applicants":
				return "O grupo já tem candidatos demais.";
			case "wom_unavailable":
				return "Não deu para confirmar o clã no Wise Old Man agora. Tente de novo em instantes.";
			case "not_connected":
			case "not_a_member":
			case "under_review":
			case "blocked":
			case "beta_only":
				return "Seu acesso ao conteúdo do clã não foi confirmado.";
			default:
				return error.startsWith("invalid_")
					? "O servidor recusou os dados (" + error + "). Atualize o plugin."
					: "Não deu certo (" + error + ").";
		}
	}

	// ---------- Navegação ----------

	private void show(Screen screen, Runnable render, JComponent content, boolean refreshable)
	{
		this.screen = screen;
		currentScreen = render;
		currentScreenRefreshable = refreshable;
		removeAll();
		add(content, BorderLayout.NORTH);
		revalidate();
		repaint();
	}

	/** Redesenha a tela atual com o estado novo, menos formulários (ver currentScreenRefreshable). */
	private void refresh()
	{
		if (currentScreenRefreshable)
		{
			currentScreen.run();
		}
	}

	private void showDisconnected()
	{
		JPanel screen = PartyUi.column();
		PluginErrorPanel info = new PluginErrorPanel();
		info.setContent("Grupos", "Entre no jogo com a sua conta do clã para ver e criar grupos.");
		screen.add(info);
		show(Screen.DISCONNECTED, this::showDisconnected, screen, true);
	}

	// ---------- Lista ----------

	/**
	 * Raiz da aba. Quem está num grupo (líder ou aceito) só vê o próprio grupo: para ver os outros,
	 * precisa encerrar ou sair. Só quem não tem grupo (ou ainda aguarda resposta) vê a lista.
	 */
	private void showList()
	{
		if (playerName == null)
		{
			showDisconnected();
			return;
		}
		MyStatus.Role role = status != null ? status.role() : MyStatus.Role.NONE;
		Party myParty = status != null ? status.party : null;
		if (role == MyStatus.Role.LEADER && myParty != null)
		{
			showManage();
			return;
		}
		if (role == MyStatus.Role.MEMBER && myParty != null)
		{
			showLobby();
			return;
		}

		JPanel screen = PartyUi.column();

		if (role == MyStatus.Role.APPLICANT && myParty != null)
		{
			// Grupo cheio: o candidato continua na fila (o líder ainda pode aceitar se abrir vaga)
			String waiting = myParty.isFull() ? "grupo cheio: você está na fila" : "aguardando " + myParty.leaderName;
			screen.add(PartyUi.banner(myParty.name, waiting, PartyUi.GRAY,
				PartyUi.button("Cancelar", () -> act(cb -> api.cancelApplication(cb::accept)))));
		}

		Object[] options = new Object[Activity.values().length + 1];
		options[0] = "Todas";
		System.arraycopy(Activity.values(), 0, options, 1, Activity.values().length);
		JComboBox<Object> filterBox = PartyUi.combo(options);
		filterBox.setSelectedItem(filter != null ? filter : "Todas");
		filterBox.addActionListener(e ->
		{
			Object item = filterBox.getSelectedItem();
			filter = item instanceof Activity ? (Activity) item : null;
			listLoaded = false;
			showList();
			loadList();
		});
		screen.add(PartyUi.row("Atividade", filterBox));

		boolean inParty = role != MyStatus.Role.NONE;
		JButton create = PartyUi.button("Criar grupo", this::showCreate);
		create.setEnabled(!inParty);
		if (inParty)
		{
			create.setToolTipText("Você já está em um grupo");
		}
		screen.add(create);

		if (!listLoaded)
		{
			screen.add(PartyUi.gray("Carregando grupos..."));
		}
		else if (openParties.isEmpty())
		{
			PluginErrorPanel empty = new PluginErrorPanel();
			empty.setContent("Nenhum grupo aberto", "Crie um grupo para chamar o clã. Quando alguém abrir um, ele aparece aqui.");
			screen.add(empty);
		}
		else
		{
			screen.add(PartyUi.section("Grupos abertos (" + openParties.size() + ")", true, partyItems(openParties)));
		}
		show(Screen.LIST, this::showList, screen, true);
	}

	private JPanel partyItems(List<Party> parties)
	{
		// Primeiro os que posso entrar; os que falta requisito vão para o fim (ordem do servidor dentro de cada bloco)
		List<Party> eligible = new ArrayList<>();
		List<Party> ineligible = new ArrayList<>();
		for (Party p : parties)
		{
			(allMet(checks(p)) ? eligible : ineligible).add(p);
		}

		JPanel column = PartyUi.column();
		for (Party p : eligible)
		{
			column.add(partyItem(p, true));
		}
		for (Party p : ineligible)
		{
			column.add(partyItem(p, false));
		}
		return column;
	}

	private JPanel partyItem(Party p, boolean eligible)
	{
		return PartyUi.partyItem(p.name, p.slotsText(), p.summary(), p.lootText(), p.learners, eligible, () -> showDetails(p));
	}

	// ---------- Requisitos (dados do próprio cliente + hiscore) ----------

	/** Desconhecido (cliente ou hiscore ainda sem dado) conta como não cumprido; o servidor confere de novo. */
	private List<Check> checks(Party p)
	{
		List<Check> checks = new ArrayList<>();
		if (p.minCombat > 0)
		{
			Integer combat = me != null ? me.combatLevel : null;
			checks.add(new Check("Combate " + p.minCombat + "+", combat != null ? combat.toString() : "?",
				combat != null && combat >= p.minCombat));
		}
		if (p.minKillCount > 0)
		{
			Integer kc = p.activity.killCount(hiscores, p.mode);
			checks.add(new Check("KC " + p.minKillCount + "+", kc != null ? kc.toString() : "?",
				kc != null && kc >= p.minKillCount));
		}
		return checks;
	}

	private static boolean allMet(List<Check> checks)
	{
		for (Check c : checks)
		{
			if (!c.met)
			{
				return false;
			}
		}
		return true;
	}

	// ---------- Criar ----------

	private void showCreate()
	{
		JPanel screen = PartyUi.column();

		PartyForm form = new PartyForm(me != null ? me.world : 301);
		form.addSections(screen, "Novo grupo");

		screen.add(PartyUi.spacer(6));
		screen.add(PartyUi.button("Publicar grupo", () ->
		{
			if (me == null)
			{
				JOptionPane.showMessageDialog(this, "Entre no jogo para criar um grupo.", "OldVicios", JOptionPane.WARNING_MESSAGE);
				return;
			}
			if (!confirm("Publicar o grupo? Depois de criado, ele não pode ser alterado."))
			{
				return;
			}
			JsonObject body = new JsonObject();
			body.addProperty("name", form.name());
			body.addProperty("activity", form.activity().name());
			if (form.mode() != null)
			{
				body.addProperty("mode", form.mode());
			}
			body.addProperty("world", form.world());
			body.addProperty("slots", form.slots());
			body.addProperty("learners", form.learners());
			body.addProperty("lootMode", form.partyLoot().name());
			body.addProperty("minCombat", form.minCombat());
			body.addProperty("minKillCount", form.minKillCount());
			body.addProperty("description", form.description());
			body.add("player", playerInfo(form.activity(), form.mode(), form.ownLoot()));
			act(cb -> api.create(body, cb::accept), this::showList);
		}));
		screen.add(PartyUi.spacer(10));
		screen.add(PartyUi.backButton(this::showList));
		// Formulário: atualizações (ícones, /me em loop) não podem redesenhar e apagar o que foi digitado
		show(Screen.CREATE, this::showCreate, screen, false);
	}

	/** O que o próprio cliente informa: combate, modo de jogo, KC na atividade (hiscore) e o loot desejado. */
	private JsonObject playerInfo(Activity activity, String mode, LootMode loot)
	{
		JsonObject player = new JsonObject();
		player.addProperty("combatLevel", me.combatLevel);
		player.addProperty("accountType", me.accountType.name());
		Integer kc = activity.killCount(hiscores, mode);
		if (kc != null)
		{
			player.addProperty("killCount", kc);
		}
		player.addProperty("lootPreference", loot.name());
		return player;
	}

	// ---------- Ver e candidatar-se ----------

	private void showDetails(Party p)
	{
		detail = p;
		renderDetails();
		loadDetail(p.id);
	}

	private void renderDetails()
	{
		Party p = detail;
		if (p == null)
		{
			showList();
			return;
		}
		JPanel screen = PartyUi.column();

		JPanel info = PartyUi.column();
		info.add(PartyUi.row("Atividade", p.activity.displayName));
		if (p.mode != null)
		{
			info.add(PartyUi.row("Modo", p.mode));
		}
		info.add(PartyUi.row("Mundo", String.valueOf(p.world)));
		info.add(PartyUi.row("Vagas", p.slotsText()));
		info.add(PartyUi.row("Loot do grupo", p.lootText()));
		info.add(PartyUi.row("Learners", PartyUi.yesNo(p.learners)));
		if (!p.descriptionText().isEmpty())
		{
			info.add(PartyUi.paragraph(p.descriptionText(), PartyUi.GRAY));
		}
		screen.add(PartyUi.section(p.name, true, info));

		List<Check> checks = checks(p);
		JPanel requirements = PartyUi.column();
		if (checks.isEmpty())
		{
			requirements.add(PartyUi.gray("Sem requisitos"));
		}
		for (Check c : checks)
		{
			requirements.add(PartyUi.row(c.label, PartyUi.checkResult(c.value, c.met)));
		}
		screen.add(PartyUi.section("Requisitos", true, requirements));

		JPanel members;
		if (p.members == null)
		{
			// Item da lista ainda sem os membros: o GET /parties/:id completa
			members = PartyUi.column();
			members.add(PartyUi.gray("Carregando membros..."));
		}
		else
		{
			members = PartyUi.tileGrid();
			for (PartyMember m : p.members())
			{
				members.add(PartyUi.memberTile(m, m.leader, accountIcon(m), tileCaptions("CB " + m.combatLevel, m),
					actions(PartyUi.womButton(m.name))));
			}
		}
		screen.add(PartyUi.section("Membros (" + p.slotsText() + ")", true, members));

		// O loot do grupo é só a regra: o candidato diz qual quer, e o líder vê na candidatura
		JComboBox<LootMode> myLoot = PartyUi.combo(LootMode.values());
		myLoot.setSelectedItem(p.lootMode);
		JPanel candidacy = PartyUi.column();
		candidacy.add(PartyUi.row("Seu loot", myLoot));
		screen.add(PartyUi.section("Sua candidatura", true, candidacy));

		String blockReason = blockReason(p, checks);
		JButton apply = PartyUi.button("Candidatar-se", () ->
		{
			JsonObject body = playerInfo(p.activity, p.mode, (LootMode) myLoot.getSelectedItem());
			act(cb -> api.apply(p.id, body, cb::accept), this::showList);
		});
		apply.setEnabled(blockReason == null);
		myLoot.setEnabled(blockReason == null);
		screen.add(PartyUi.spacer(6));
		screen.add(apply);
		if (blockReason != null)
		{
			screen.add(PartyUi.paragraph(blockReason, PartyUi.ORANGE));
		}
		screen.add(PartyUi.spacer(10));
		screen.add(PartyUi.backButton(() ->
		{
			detail = null;
			showList();
		}));
		show(Screen.DETAILS, this::renderDetails, screen, true);
	}

	private String blockReason(Party p, List<Check> checks)
	{
		if (me == null)
		{
			return "Entre no jogo para se candidatar.";
		}
		if (status != null && status.role() != MyStatus.Role.NONE)
		{
			return "Você já está em um grupo.";
		}
		if (p.isFull())
		{
			return "O grupo está cheio.";
		}
		if (!p.applicationsOpen)
		{
			return "As inscrições estão fechadas.";
		}
		if (!allMet(checks))
		{
			return hiscores == null && p.minKillCount > 0
				? "Carregando seu KC do hiscore..."
				: "Você não cumpre os requisitos.";
		}
		return null;
	}

	// ---------- Meu grupo (líder) ----------

	/** Depois de criado, o grupo não muda: o líder só administra membros e candidatos (e pode encerrar). */
	private void showManage()
	{
		Party p = status.party;
		JPanel screen = PartyUi.column();

		JPanel info = PartyUi.column();
		info.add(PartyUi.row("Atividade", p.activity != null ? p.activity.displayName : "?"));
		if (p.mode != null)
		{
			info.add(PartyUi.row("Modo", p.mode));
		}
		info.add(PartyUi.row("Mundo", String.valueOf(p.world)));
		info.add(PartyUi.row("Loot do grupo", p.lootText()));
		info.add(PartyUi.row("Learners", PartyUi.yesNo(p.learners)));
		if (p.minCombat > 0)
		{
			info.add(PartyUi.row("Combate mínimo", String.valueOf(p.minCombat)));
		}
		if (p.minKillCount > 0)
		{
			info.add(PartyUi.row("KC mínimo", String.valueOf(p.minKillCount)));
		}
		JCheckBox applicationsOpen = new JCheckBox();
		applicationsOpen.setSelected(p.applicationsOpen);
		applicationsOpen.addActionListener(e ->
			act(cb -> api.setApplicationsOpen(p.id, applicationsOpen.isSelected(), cb::accept)));
		info.add(PartyUi.row("Inscrições abertas", applicationsOpen));
		if (!p.descriptionText().isEmpty())
		{
			info.add(PartyUi.paragraph(p.descriptionText(), PartyUi.GRAY));
		}
		screen.add(PartyUi.section(p.name, true, info));

		JPanel members = PartyUi.tileGrid();
		for (PartyMember m : p.members())
		{
			List<JButton> memberActions = actions(PartyUi.womButton(m.name));
			if (!m.leader)
			{
				memberActions.add(PartyUi.iconButton(PartyUi.TRASH, PartyUi.TRASH_HOVER, "Remover do grupo", () ->
				{
					if (confirm("Remover " + m.name + " do grupo?"))
					{
						act(cb -> api.kick(p.id, m.id, cb::accept));
					}
				}));
			}
			members.add(PartyUi.memberTile(m, m.leader, accountIcon(m), tileCaptions("CB " + m.combatLevel, m), memberActions));
		}
		screen.add(PartyUi.section("Membros (" + p.slotsText() + ")", true, members));

		JPanel applicants = PartyUi.column();
		if (p.applicants().isEmpty())
		{
			applicants.add(PartyUi.gray("Nenhum candidato"));
		}
		for (PartyMember a : p.applicants())
		{
			JButton accept = PartyUi.iconButton(PartyUi.CHECK, PartyUi.CHECK_HOVER, p.isFull() ? "Grupo cheio" : "Aceitar",
				() -> act(cb -> api.accept(p.id, a.id, cb::accept)));
			accept.setEnabled(!p.isFull());
			JButton decline = PartyUi.iconButton(PartyUi.CROSS, PartyUi.CROSS_HOVER, "Recusar",
				() -> act(cb -> api.decline(p.id, a.id, cb::accept)));
			// Mesmo bloco com fundo dos membros, mas um candidato por linha
			applicants.add(PartyUi.memberTile(a, false, accountIcon(a),
				Arrays.asList(combatAndKc(a), "Loot: " + a.lootText()),
				actions(PartyUi.womButton(a.name), accept, decline)));
		}
		screen.add(PartyUi.section("Candidatos (" + p.applicants().size() + ")", true, applicants));

		JButton close = PartyUi.button("Encerrar grupo", () ->
		{
			if (confirm("Encerrar o grupo \"" + p.name + "\"?"))
			{
				act(cb -> api.close(p.id, cb::accept));
			}
		});
		close.setForeground(PartyUi.ORANGE);
		screen.add(PartyUi.spacer(6));
		screen.add(close);
		show(Screen.MANAGE, this::showList, screen, true);
	}

	// ---------- Lobby (membro aceito) ----------

	private void showLobby()
	{
		Party p = status.party;
		JPanel screen = PartyUi.column();
		screen.add(PartyUi.banner("Você está neste grupo", "Saia do grupo para ver os outros.", PartyUi.GREEN, null));

		JPanel info = PartyUi.column();
		info.add(PartyUi.row("Atividade", p.activity != null ? p.activity.displayName : "?"));
		if (p.mode != null)
		{
			info.add(PartyUi.row("Modo", p.mode));
		}
		info.add(PartyUi.row("Vagas", p.slotsText()));
		String world = String.valueOf(p.world);
		if (me != null && me.world != p.world)
		{
			info.add(PartyUi.row(PartyUi.label("Mundo", PartyUi.TEXT, false),
				PartyUi.label(world + " (você: " + me.world + ")", PartyUi.ORANGE, false)));
		}
		else
		{
			info.add(PartyUi.row("Mundo", world));
		}
		info.add(PartyUi.row("Loot do grupo", p.lootText()));
		if (!p.descriptionText().isEmpty())
		{
			info.add(PartyUi.paragraph(p.descriptionText(), PartyUi.GRAY));
		}
		screen.add(PartyUi.section(p.name, true, info));

		JPanel members = PartyUi.tileGrid();
		for (PartyMember m : p.members())
		{
			String first = m.name.equalsIgnoreCase(playerName) ? "você" : "CB " + m.combatLevel;
			members.add(PartyUi.memberTile(m, m.leader, accountIcon(m), tileCaptions(first, m),
				actions(PartyUi.womButton(m.name))));
		}
		screen.add(PartyUi.section("Membros", true, members));

		JButton leave = PartyUi.button("Sair do grupo", () ->
		{
			if (confirm("Sair do grupo \"" + p.name + "\"?"))
			{
				act(cb -> api.leave(cb::accept));
			}
		});
		leave.setForeground(PartyUi.ORANGE);
		screen.add(PartyUi.spacer(6));
		screen.add(leave);
		show(Screen.LOBBY, this::showList, screen, true);
	}

	// ---------- Comum ----------

	/** "CB 121, 64 KC", ou só "CB 121" quando o KC não veio (atividade sem KC ou fora do hiscore). */
	private static String combatAndKc(PartyMember m)
	{
		return "CB " + m.combatLevel + (m.killCount != null ? ", " + m.killCount + " KC" : "");
	}

	/** Legendas do quadradinho: primeira linha (CB ou "você") e o loot que o jogador quer. */
	private static List<String> tileCaptions(String first, PartyMember m)
	{
		return Arrays.asList(first, "Loot: " + m.lootText());
	}

	private static List<JButton> actions(JButton... buttons)
	{
		return new ArrayList<>(Arrays.asList(buttons));
	}

	private boolean confirm(String question)
	{
		Object[] options = {"Sim", "Não"};
		return JOptionPane.showOptionDialog(this, question, "OldVicios", JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE, null, options, options[1]) == JOptionPane.YES_OPTION;
	}
}
