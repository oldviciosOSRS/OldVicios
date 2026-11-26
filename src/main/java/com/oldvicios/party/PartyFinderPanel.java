package com.oldvicios.party;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.PluginErrorPanel;

/**
 * Aba "Grupos" do painel do OldVicios. Por enquanto só a casca: telas e navegação, com todos os
 * dados e ações mockados em {@link MockPartyStore}. Todos os métodos rodam na Swing EDT.
 */
public class PartyFinderPanel extends JPanel
{
	/** Tempo até o "líder" mockado aceitar a sua candidatura. */
	private static final int MOCK_ACCEPT_DELAY_MS = 3000;

	private final MockPartyStore store = new MockPartyStore();
	private final Timer mockAccept;
	/** Ícones de modo de jogo (iron, HCIM, UIM, GIM...), carregados do cache do jogo pelo SpriteManager. */
	private final Map<AccountType, BufferedImage> accountIcons = new EnumMap<>(AccountType.class);
	/** null = todas as atividades. */
	private Activity filter;
	/** Tela atual, para redesenhar quando o estado muda. */
	private Runnable currentScreen = this::showList;
	/** false na tela de criar grupo: redesenhar apagaria o que o jogador está digitando. */
	private boolean currentScreenRefreshable = true;

	public PartyFinderPanel(SpriteManager spriteManager)
	{
		super(new BorderLayout());
		setOpaque(false);
		mockAccept = new Timer(MOCK_ACCEPT_DELAY_MS, e ->
		{
			store.mockLeaderAccepts();
			// Aceito: a aba passa a mostrar só o grupo (showList() redireciona para o lobby)
			showList();
		});
		mockAccept.setRepeats(false);
		loadAccountIcons(spriteManager);
		showList();
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
		return accountIcons.get(m.accountType);
	}

	/** Nick do jogador conectado (vira o "você" dos mocks). */
	public void setPlayerName(String name)
	{
		store.setLocalPlayerName(name);
		refresh();
	}

	/** Redesenha a tela atual com o estado novo, menos formulários (ver currentScreenRefreshable). */
	private void refresh()
	{
		if (currentScreenRefreshable)
		{
			currentScreen.run();
		}
	}

	public void shutdown()
	{
		mockAccept.stop();
	}

	/**
	 * Faixa de debug (só com usuário mockado): alterna entre dono de um grupo e candidato.
	 * onSwitch é chamado depois, para o painel abrir a aba Grupos.
	 */
	public JComponent debugBar(Runnable onSwitch)
	{
		JToggleButton leader = new JToggleButton("Dono");
		JToggleButton applicant = new JToggleButton("Candidato", true);
		ButtonGroup toggle = new ButtonGroup();
		toggle.add(leader);
		toggle.add(applicant);
		leader.setFocusPainted(false);
		applicant.setFocusPainted(false);
		leader.addActionListener(e ->
		{
			mockAccept.stop();
			store.debugBecomeLeader();
			showManage(store.ownParty);
			onSwitch.run();
		});
		applicant.addActionListener(e ->
		{
			mockAccept.stop();
			store.debugBecomeApplicant();
			showList();
			onSwitch.run();
		});

		JPanel buttons = new JPanel(new GridLayout(1, 2, 4, 0));
		buttons.setOpaque(false);
		buttons.add(leader);
		buttons.add(applicant);

		JPanel bar = new JPanel(new BorderLayout(6, 0));
		bar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		bar.setBorder(new CompoundBorder(
			BorderFactory.createDashedBorder(ColorScheme.BRAND_ORANGE, 3, 2),
			new EmptyBorder(4, 6, 4, 4)));
		bar.add(PartyUi.label("DEBUG", ColorScheme.BRAND_ORANGE, true), BorderLayout.WEST);
		bar.add(buttons, BorderLayout.CENTER);
		return bar;
	}

	private void show(Runnable screen, JComponent content)
	{
		show(screen, content, true);
	}

	private void show(Runnable screen, JComponent content, boolean refreshable)
	{
		currentScreen = screen;
		currentScreenRefreshable = refreshable;
		removeAll();
		add(content, BorderLayout.NORTH);
		revalidate();
		repaint();
	}

	// ---------- Lista ----------

	/**
	 * Raiz da aba. Quem está num grupo (líder ou aceito) só vê o próprio grupo: para ver os outros,
	 * precisa encerrar ou sair. Só quem não tem grupo (ou ainda aguarda resposta) vê a lista.
	 */
	private void showList()
	{
		if (store.ownParty != null)
		{
			showManage(store.ownParty);
			return;
		}
		if (store.application == MockPartyStore.Application.ACCEPTED)
		{
			showLobby(store.appliedParty);
			return;
		}

		JPanel screen = PartyUi.column();

		if (store.application == MockPartyStore.Application.PENDING)
		{
			// Grupo cheio: o candidato continua na fila (o líder ainda pode aceitar se abrir vaga)
			String waiting = store.appliedParty.isFull()
				? "grupo cheio: você está na fila"
				: "aguardando " + store.appliedParty.leader.name;
			screen.add(PartyUi.banner(store.appliedParty.name, waiting, PartyUi.GRAY,
				PartyUi.button("Cancelar", () ->
				{
					mockAccept.stop();
					store.cancelApplication();
					showList();
				})));
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
			showList();
		});
		screen.add(PartyUi.row("Atividade", filterBox));

		JButton create = PartyUi.button("Criar grupo", this::showCreate);
		create.setEnabled(!store.isBusy());
		if (store.isBusy())
		{
			create.setToolTipText("Você já está em um grupo");
		}
		screen.add(create);

		// Grupos cheios não aparecem: não há o que fazer com eles
		List<Party> open = store.list(filter, false);
		if (open.isEmpty())
		{
			PluginErrorPanel empty = new PluginErrorPanel();
			empty.setContent("Nenhum grupo aberto", "Crie um grupo para chamar o clã. Quando alguém abrir um, ele aparece aqui.");
			screen.add(empty);
		}
		else
		{
			screen.add(PartyUi.section("Grupos abertos (" + open.size() + ")", true, partyItems(open)));
		}
		show(this::showList, screen);
	}

	private JPanel partyItems(List<Party> parties)
	{
		// Primeiro os que posso entrar; os que falta requisito vão para o fim (ordem estável dentro de cada bloco)
		List<Party> eligible = new ArrayList<>();
		List<Party> ineligible = new ArrayList<>();
		for (Party p : parties)
		{
			(MockPartyStore.allMet(store.check(p)) ? eligible : ineligible).add(p);
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
		return PartyUi.partyItem(p.name, p.slotsText(), p.summary(), p.lootMode.displayName, p.learners, eligible,
			() -> showDetails(p));
	}

	// ---------- Criar ----------

	private void showCreate()
	{
		JPanel screen = PartyUi.column();

		PartyForm form = new PartyForm(MockPartyStore.MY_WORLD);
		form.addSections(screen, "Novo grupo");

		screen.add(PartyUi.spacer(6));
		screen.add(PartyUi.button("Publicar grupo", () ->
		{
			if (!confirm("Publicar o grupo? Depois de criado, ele não pode ser alterado."))
			{
				return;
			}
			Party p = store.create(form.name(), form.activity(), form.mode(), form.world(), form.slots(),
				form.learners(), form.partyLoot(), form.ownLoot(), form.minCombat(), form.minKillCount(),
				form.description());
			showManage(p);
		}));
		screen.add(PartyUi.spacer(10));
		screen.add(PartyUi.backButton(this::showList));
		// Formulário: atualizações (ícones, nick, /me) não podem redesenhar e apagar o que foi digitado
		show(this::showCreate, screen, false);
	}

	// ---------- Ver e candidatar-se ----------

	private void showDetails(Party p)
	{
		JPanel screen = PartyUi.column();

		JPanel info = PartyUi.column();
		info.add(PartyUi.row("Atividade", p.activity.displayName));
		if (p.mode != null)
		{
			info.add(PartyUi.row("Modo", p.mode));
		}
		info.add(PartyUi.row("Mundo", String.valueOf(p.world)));
		info.add(PartyUi.row("Vagas", p.slotsText()));
		info.add(PartyUi.row("Loot do grupo", p.lootMode.displayName));
		info.add(PartyUi.row("Learners", PartyUi.yesNo(p.learners)));
		if (!p.description.isEmpty())
		{
			info.add(PartyUi.paragraph(p.description, PartyUi.GRAY));
		}
		screen.add(PartyUi.section(p.name, true, info));

		List<MockPartyStore.Check> checks = store.check(p);
		JPanel requirements = PartyUi.column();
		if (checks.isEmpty())
		{
			requirements.add(PartyUi.gray("Sem requisitos"));
		}
		for (MockPartyStore.Check c : checks)
		{
			requirements.add(PartyUi.row(c.label, PartyUi.checkResult(c.value, c.met)));
		}
		screen.add(PartyUi.section("Requisitos", true, requirements));

		JPanel members = PartyUi.tileGrid();
		for (PartyMember m : p.members)
		{
			members.add(PartyUi.memberTile(m, m == p.leader, accountIcon(m), tileCaptions("CB " + m.combatLevel, m),
				actions(PartyUi.womButton(m.name))));
		}
		screen.add(PartyUi.section("Membros (" + p.slotsText() + ")", true, members));

		// O loot do grupo é só a preferência do líder: o candidato diz qual quer, e o líder vê na candidatura
		JComboBox<LootMode> myLoot = PartyUi.combo(LootMode.values());
		myLoot.setSelectedItem(p.lootMode);
		JPanel candidacy = PartyUi.column();
		candidacy.add(PartyUi.row("Seu loot", myLoot));
		screen.add(PartyUi.section("Sua candidatura", true, candidacy));

		String blockReason = blockReason(p, checks);
		JButton apply = PartyUi.button("Candidatar-se", () ->
		{
			store.apply(p, (LootMode) myLoot.getSelectedItem());
			mockAccept.restart();
			showList();
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
		screen.add(PartyUi.backButton(this::showList));
		show(() -> showDetails(p), screen);
	}

	private String blockReason(Party p, List<MockPartyStore.Check> checks)
	{
		if (store.isBusy())
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
		if (!MockPartyStore.allMet(checks))
		{
			return "Você não cumpre os requisitos.";
		}
		return null;
	}

	// ---------- Meu grupo (líder) ----------

	/** Depois de criado, o grupo não muda: o líder só administra membros e candidatos (e pode encerrar). */
	private void showManage(Party p)
	{
		JPanel screen = PartyUi.column();

		JPanel info = PartyUi.column();
		info.add(PartyUi.row("Atividade", p.activity.displayName));
		if (p.mode != null)
		{
			info.add(PartyUi.row("Modo", p.mode));
		}
		info.add(PartyUi.row("Mundo", String.valueOf(p.world)));
		info.add(PartyUi.row("Loot do grupo", p.lootMode.displayName));
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
		applicationsOpen.addActionListener(e -> p.applicationsOpen = applicationsOpen.isSelected());
		info.add(PartyUi.row("Inscrições abertas", applicationsOpen));
		info.add(PartyUi.row("Party do RuneLite", runeLitePartyStatus(() -> showManage(p))));
		if (!p.description.isEmpty())
		{
			info.add(PartyUi.paragraph(p.description, PartyUi.GRAY));
		}
		screen.add(PartyUi.section(p.name, true, info));

		JPanel members = PartyUi.tileGrid();
		for (PartyMember m : p.members)
		{
			List<JButton> memberActions = actions(PartyUi.womButton(m.name));
			if (m != p.leader)
			{
				memberActions.add(PartyUi.iconButton(PartyUi.TRASH, PartyUi.TRASH_HOVER, "Remover do grupo", () ->
				{
					if (confirm("Remover " + m.name + " do grupo?"))
					{
						store.kick(p, m);
						showManage(p);
					}
				}));
			}
			members.add(PartyUi.memberTile(m, m == p.leader, accountIcon(m), tileCaptions("CB " + m.combatLevel, m),
				memberActions));
		}
		screen.add(PartyUi.section("Membros (" + p.slotsText() + ")", true, members));

		JPanel applicants = PartyUi.column();
		if (p.applicants.isEmpty())
		{
			applicants.add(PartyUi.gray("Nenhum candidato"));
		}
		for (PartyMember a : new ArrayList<>(p.applicants))
		{
			JButton accept = PartyUi.iconButton(PartyUi.CHECK, PartyUi.CHECK_HOVER, p.isFull() ? "Grupo cheio" : "Aceitar", () ->
			{
				store.accept(p, a);
				showManage(p);
			});
			accept.setEnabled(!p.isFull());
			JButton decline = PartyUi.iconButton(PartyUi.CROSS, PartyUi.CROSS_HOVER, "Recusar", () ->
			{
				store.decline(p, a);
				showManage(p);
			});
			// Mesmo bloco com fundo dos membros, mas um candidato por linha
			applicants.add(PartyUi.memberTile(a, false, accountIcon(a),
				Arrays.asList(combatAndKc(a), "Loot: " + a.lootPreference.displayName),
				actions(PartyUi.womButton(a.name), accept, decline)));
		}
		screen.add(PartyUi.section("Candidatos (" + p.applicants.size() + ")", true, applicants));

		JButton close = PartyUi.button("Encerrar grupo", () ->
		{
			if (confirm("Encerrar o grupo \"" + p.name + "\"?"))
			{
				store.close(p);
				showList();
			}
		});
		close.setForeground(PartyUi.ORANGE);
		screen.add(PartyUi.spacer(6));
		screen.add(close);
		show(() -> showManage(p), screen);
	}

	// ---------- Lobby (membro aceito) ----------

	private void showLobby(Party p)
	{
		JPanel screen = PartyUi.column();
		screen.add(PartyUi.banner("Você está neste grupo", "Saia do grupo para ver os outros.", PartyUi.GREEN, null));

		JPanel info = PartyUi.column();
		info.add(PartyUi.row("Atividade", p.activity.displayName));
		if (p.mode != null)
		{
			info.add(PartyUi.row("Modo", p.mode));
		}
		info.add(PartyUi.row("Vagas", p.slotsText()));
		String world = String.valueOf(p.world);
		int myWorld = store.localPlayer().world;
		if (myWorld != p.world)
		{
			info.add(PartyUi.row(PartyUi.label("Mundo", PartyUi.TEXT, false),
				PartyUi.label(world + " (você: " + myWorld + ")", PartyUi.ORANGE, false)));
		}
		else
		{
			info.add(PartyUi.row("Mundo", world));
		}
		info.add(PartyUi.row("Loot do grupo", p.lootMode.displayName));
		info.add(PartyUi.row("Party do RuneLite", runeLitePartyStatus(() -> showLobby(p))));
		if (!p.description.isEmpty())
		{
			info.add(PartyUi.paragraph(p.description, PartyUi.GRAY));
		}
		screen.add(PartyUi.section(p.name, true, info));

		JPanel members = PartyUi.tileGrid();
		for (PartyMember m : p.members)
		{
			String first = m == store.localPlayer() ? "você" : "CB " + m.combatLevel;
			members.add(PartyUi.memberTile(m, m == p.leader, accountIcon(m), tileCaptions(first, m),
				actions(PartyUi.womButton(m.name))));
		}
		screen.add(PartyUi.section("Membros", true, members));

		JButton leave = PartyUi.button("Sair do grupo", () ->
		{
			if (confirm("Sair do grupo \"" + p.name + "\"?"))
			{
				store.leave();
				showList();
			}
		});
		leave.setForeground(PartyUi.ORANGE);
		screen.add(PartyUi.spacer(6));
		screen.add(leave);
		show(() -> showLobby(p), screen);
	}

	// ---------- Comum ----------

	/** "conectado" ou o botão Entrar. Entrar na party é sempre clique do jogador (auto-join é proibido no Hub). */
	private JComponent runeLitePartyStatus(Runnable redraw)
	{
		if (store.localPlayer().inRuneLiteParty)
		{
			return PartyUi.label("conectado", PartyUi.GREEN, false);
		}
		return PartyUi.button("Entrar", () ->
		{
			store.joinRuneLiteParty();
			redraw.run();
		});
	}

	/** "CB 121, 64 KC", ou só "CB 121" quando o KC não veio (atividade sem KC ou abaixo do mínimo do hiscore). */
	private static String combatAndKc(PartyMember m)
	{
		return "CB " + m.combatLevel + (m.killCount != null ? ", " + m.killCount + " KC" : "");
	}

	/** Legendas do quadradinho: primeira linha (CB ou "você") e o loot que o jogador quer. */
	private static List<String> tileCaptions(String first, PartyMember m)
	{
		return Arrays.asList(first, "Loot: " + m.lootPreference.displayName);
	}

	private static List<JButton> actions(JButton... buttons)
	{
		List<JButton> list = new ArrayList<>();
		for (JButton b : buttons)
		{
			list.add(b);
		}
		return list;
	}

	private boolean confirm(String question)
	{
		Object[] options = {"Sim", "Não"};
		return JOptionPane.showOptionDialog(this, question, "OldVicios", JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE, null, options, options[1]) == JOptionPane.YES_OPTION;
	}
}
