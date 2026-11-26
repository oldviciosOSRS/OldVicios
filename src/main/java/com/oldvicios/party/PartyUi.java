package com.oldvicios.party;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.LinkBrowser;

/**
 * Peças da aba Grupos, no visual do painel de configuração do RuneLite: seções laranja recolhíveis,
 * linhas rótulo/controle, cards de membro como os do plugin Party e ações em ícone na linha.
 */
final class PartyUi
{
	static final Color TEXT = ColorScheme.TEXT_COLOR;
	static final Color GRAY = ColorScheme.LIGHT_GRAY_COLOR;
	static final Color GREEN = new Color(127, 214, 127);
	static final Color ORANGE = new Color(232, 160, 96);
	static final Color RED = new Color(235, 105, 105);
	private static final Color UNMET_BACKGROUND = new Color(56, 30, 30);
	private static final Color UNMET_BACKGROUND_HOVER = new Color(74, 38, 38);

	private static final BufferedImage ARROW_OPEN = triangle(true);
	private static final BufferedImage ARROW_CLOSED = triangle(false);
	private static final BufferedImage WOM = barChart(TEXT);
	private static final BufferedImage WOM_HOVER = barChart(Color.WHITE);
	static final BufferedImage CROSS = cross(TEXT);
	static final BufferedImage CROSS_HOVER = cross(Color.WHITE);
	static final BufferedImage CHECK = check(GREEN);
	static final BufferedImage CHECK_HOVER = check(Color.WHITE);
	private static final BufferedImage CHECK_SMALL = check(GREEN);
	private static final BufferedImage UNMET = cross(ORANGE);
	private static final BufferedImage CROSS_RED = cross(RED);
	private static final BufferedImage STAR = star(new Color(232, 185, 35));
	static final BufferedImage TRASH = trash(TEXT);
	static final BufferedImage TRASH_HOVER = trash(RED);

	private PartyUi()
	{
	}

	// ---------- Texto ----------

	/** Label com texto de outro jogador: html desligado (Swing renderiza HTML em JLabel). */
	static JLabel label(String text, Color color, boolean small)
	{
		JLabel label = new JLabel(text);
		label.putClientProperty("html.disable", Boolean.TRUE);
		label.setForeground(color);
		if (small)
		{
			label.setFont(FontManager.getRunescapeSmallFont());
		}
		return label;
	}

	static JLabel white(String text)
	{
		return label(text, Color.WHITE, false);
	}

	static JLabel gray(String text)
	{
		return label(text, GRAY, true);
	}

	/** Texto que quebra linha. Escapado antes de ir pro HTML. */
	static JLabel paragraph(String text, Color color)
	{
		JLabel label = new JLabel("<html><div style='width:170px'>" + escape(text) + "</div></html>");
		label.setForeground(color);
		label.setFont(FontManager.getRunescapeSmallFont());
		return label;
	}

	private static String escape(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	// ---------- Layout ----------

	/** Pilha vertical em largura total, como o PartyPanel. */
	static JPanel column()
	{
		JPanel panel = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
		panel.setOpaque(false);
		return panel;
	}

	/** Seção recolhível igual às do ConfigPanel: seta + título laranja com linha embaixo. */
	static JPanel section(String title, boolean open, JComponent content)
	{
		JPanel section = new JPanel(new BorderLayout());
		section.setOpaque(false);
		section.setBorder(new EmptyBorder(8, 0, 0, 0));

		JLabel header = label(title, ColorScheme.BRAND_ORANGE, false);
		header.setFont(FontManager.getRunescapeBoldFont());
		header.setIcon(new ImageIcon(open ? ARROW_OPEN : ARROW_CLOSED));
		header.setIconTextGap(6);
		header.setBorder(new CompoundBorder(
			new MatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(0, 0, 3, 0)));
		header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		content.setBorder(new EmptyBorder(5, 0, 0, 0));
		content.setVisible(open);
		header.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				boolean visible = !content.isVisible();
				content.setVisible(visible);
				header.setIcon(new ImageIcon(visible ? ARROW_OPEN : ARROW_CLOSED));
				section.revalidate();
			}
		});

		section.add(header, BorderLayout.NORTH);
		section.add(content, BorderLayout.CENTER);
		return section;
	}

	/** Linha do painel de config: rótulo à esquerda, controle/valor à direita. */
	static JPanel row(JComponent left, JComponent right)
	{
		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setOpaque(false);
		row.add(left, BorderLayout.WEST);
		row.add(centered(right), BorderLayout.EAST);
		return row;
	}

	static JPanel row(String label, JComponent right)
	{
		return row(label(label, TEXT, false), right);
	}

	static JPanel row(String label, String value)
	{
		return row(label, white(value));
	}

	/** Centraliza na vertical sem esticar (botões em BorderLayout esticam). */
	private static JPanel centered(JComponent... items)
	{
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setOpaque(false);
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0, 6, 0, 0);
		for (JComponent item : items)
		{
			panel.add(item, c);
		}
		return panel;
	}

	/** Espaço vertical vazio (para separar o botão final das seções). */
	static JComponent spacer(int height)
	{
		JPanel spacer = new JPanel();
		spacer.setOpaque(false);
		spacer.setPreferredSize(new Dimension(0, height));
		return spacer;
	}

	// ---------- Controles ----------

	static JButton button(String text, Runnable action)
	{
		JButton button = new JButton(text);
		button.setFocusPainted(false);
		button.addActionListener(e -> action.run());
		return button;
	}

	/** Ação em ícone, sem fundo, como no Inventory Setups. */
	static JButton iconButton(BufferedImage image, BufferedImage hover, String tooltip, Runnable action)
	{
		JButton button = new JButton(new ImageIcon(image));
		button.setRolloverIcon(new ImageIcon(hover));
		button.setToolTipText(tooltip);
		button.setBorder(new EmptyBorder(2, 2, 2, 2));
		button.setBorderPainted(false);
		button.setContentAreaFilled(false);
		button.setFocusPainted(false);
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.addActionListener(e -> action.run());
		return button;
	}

	static JButton womButton(String playerName)
	{
		return iconButton(WOM, WOM_HOVER, "Ver " + playerName + " no Wise Old Man", () ->
			LinkBrowser.browse("https://wiseoldman.net/players/"
				+ URLEncoder.encode(playerName, StandardCharsets.UTF_8).replace("+", "%20")));
	}

	static <T> JComboBox<T> combo(T[] items)
	{
		JComboBox<T> combo = new JComboBox<>(items);
		combo.setPreferredSize(new Dimension(130, 25));
		return combo;
	}

	static JSpinner spinner(int value, int min, int max)
	{
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, 1));
		setSpinnerColumns(spinner);
		return spinner;
	}

	static void setSpinnerColumns(JSpinner spinner)
	{
		((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(4);
	}

	/** ✓ verde ou ✗ vermelho (para sim/não, como Learners). */
	static JLabel yesNo(boolean yes)
	{
		JLabel label = new JLabel(new ImageIcon(yes ? CHECK_SMALL : CROSS_RED));
		label.setToolTipText(yes ? "Sim" : "Não");
		return label;
	}

	/** Botão "Voltar" centralizado, no fim da tela (perto do rodapé). */
	static JComponent backButton(Runnable action)
	{
		JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		row.setOpaque(false);
		row.add(button("Voltar", action));
		return row;
	}

	/**
	 * Ícones antes do nome: estrela dourada se for o líder e o ícone do modo de jogo (iron, HCIM, UIM, GIM...).
	 * accountIcon null = conta normal (ou sprite ainda não carregado).
	 */
	static JLabel withIcons(JLabel name, boolean leader, BufferedImage accountIcon, String accountName)
	{
		List<BufferedImage> icons = new ArrayList<>();
		List<String> tooltip = new ArrayList<>();
		if (leader)
		{
			icons.add(STAR);
			tooltip.add("Líder do grupo");
		}
		if (accountIcon != null)
		{
			icons.add(accountIcon);
			tooltip.add(accountName);
		}
		if (icons.isEmpty())
		{
			return name;
		}

		int gap = 3;
		int width = -gap;
		int height = 0;
		for (BufferedImage icon : icons)
		{
			width += icon.getWidth() + gap;
			height = Math.max(height, icon.getHeight());
		}
		BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = combined.createGraphics();
		int x = 0;
		for (BufferedImage icon : icons)
		{
			g.drawImage(icon, x, (height - icon.getHeight()) / 2, null);
			x += icon.getWidth() + gap;
		}
		g.dispose();

		name.setIcon(new ImageIcon(combined));
		name.setIconTextGap(4);
		name.setToolTipText(String.join(", ", tooltip));
		return name;
	}

	/** Valor de requisito: "118" com ✓ verde ou ✗ laranja (ícone desenhado, a fonte do RS não tem o glifo). */
	static JLabel checkResult(String value, boolean met)
	{
		JLabel label = label(value, met ? GREEN : ORANGE, false);
		label.setIcon(new ImageIcon(met ? CHECK_SMALL : UNMET));
		label.setHorizontalTextPosition(SwingConstants.LEFT);
		label.setIconTextGap(4);
		return label;
	}

	// ---------- Blocos ----------

	/**
	 * Item da lista de grupos (como um item da lista de plugins): clique abre o grupo.
	 * Pode entrar = vagas em verde; falta requisito = fundo avermelhado e "falta req." em vermelho.
	 */
	static JPanel partyItem(String title, String slots, String summary, String lootMode, boolean learners, boolean eligible,
		Runnable open)
	{
		Color background = eligible ? ColorScheme.DARKER_GRAY_COLOR : UNMET_BACKGROUND;
		Color backgroundHover = eligible ? ColorScheme.DARKER_GRAY_HOVER_COLOR : UNMET_BACKGROUND_HOVER;

		JPanel item = new JPanel(new DynamicGridLayout(0, 1, 0, 2));
		item.setBackground(background);
		item.setBorder(new EmptyBorder(5, 7, 5, 7));
		item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		JPanel top = new JPanel(new BorderLayout());
		top.setOpaque(false);
		top.add(white(title), BorderLayout.WEST);
		top.add(label(slots, eligible ? GREEN : RED, false), BorderLayout.EAST);

		// Linhas: título + vagas, resumo, loot, learners (✓/✗) com "falta req." à direita
		JLabel learnersLabel = gray("Learners");
		learnersLabel.setIcon(new ImageIcon(learners ? CHECK_SMALL : CROSS_RED));
		learnersLabel.setIconTextGap(4);
		JPanel bottom = new JPanel(new BorderLayout());
		bottom.setOpaque(false);
		bottom.add(learnersLabel, BorderLayout.WEST);
		if (!eligible)
		{
			bottom.add(label("falta req.", RED, true), BorderLayout.EAST);
		}

		item.add(top);
		item.add(gray(summary));
		item.add(gray("Loot: " + lootMode));
		item.add(bottom);
		item.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				open.run();
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				item.setBackground(backgroundHover);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				item.setBackground(background);
			}
		});
		return item;
	}

	/** Faixa de estado no topo (candidatura, grupo atual). action pode ser null. */
	static JPanel banner(String title, String subtitle, Color subtitleColor, JComponent action)
	{
		JPanel banner = new JPanel(new BorderLayout(6, 0));
		banner.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		banner.setBorder(new EmptyBorder(5, 7, 5, 5));
		JPanel texts = new JPanel(new DynamicGridLayout(2, 1, 0, 1));
		texts.setOpaque(false);
		texts.add(white(title));
		texts.add(label(subtitle, subtitleColor, true));
		banner.add(texts, BorderLayout.CENTER);
		if (action != null)
		{
			banner.add(centered(action), BorderLayout.EAST);
		}
		return banner;
	}

	/** Grade de 2 colunas com células do mesmo tamanho, para os quadradinhos de membro. */
	static JPanel tileGrid()
	{
		JPanel grid = new JPanel(new GridLayout(0, 2, 4, 4));
		grid.setOpaque(false);
		return grid;
	}

	/**
	 * Quadradinho de membro: ícones + nome na cor do jogador, legendas cinza (CB, loot...) e as ações
	 * em ícone no rodapé.
	 */
	static JPanel memberTile(PartyMember m, boolean leader, BufferedImage accountIcon, List<String> captions,
		List<JButton> actions)
	{
		JPanel tile = new JPanel(new BorderLayout(0, 3));
		tile.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		tile.setBorder(new EmptyBorder(5, 5, 4, 4));

		JPanel top = new JPanel(new BorderLayout(4, 0));
		top.setOpaque(false);
		JLabel name = label(m.name, m.color(), true);
		top.add(withIcons(name, leader, accountIcon, m.accountType().displayName), BorderLayout.CENTER);

		JPanel middle = new JPanel(new DynamicGridLayout(0, 1, 0, 1));
		middle.setOpaque(false);
		for (String caption : captions)
		{
			middle.add(gray(caption));
		}

		JPanel footer = new JPanel(new BorderLayout());
		footer.setOpaque(false);
		JPanel icons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
		icons.setOpaque(false);
		for (JButton action : actions)
		{
			icons.add(action);
		}
		footer.add(icons, BorderLayout.EAST);

		tile.add(top, BorderLayout.NORTH);
		tile.add(middle, BorderLayout.CENTER);
		tile.add(footer, BorderLayout.SOUTH);
		return tile;
	}

	// ---------- Ícones (desenhados em código, sem PNG) ----------

	private static Graphics2D brush(BufferedImage image, Color color, float width)
	{
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(color);
		g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		return g;
	}

	private static BufferedImage triangle(boolean open)
	{
		BufferedImage image = new BufferedImage(9, 9, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = brush(image, ColorScheme.BRAND_ORANGE, 1f);
		Path2D arrow = new Path2D.Float();
		if (open)
		{
			arrow.moveTo(1, 2);
			arrow.lineTo(8, 2);
			arrow.lineTo(4.5, 7);
		}
		else
		{
			arrow.moveTo(2, 1);
			arrow.lineTo(2, 8);
			arrow.lineTo(7, 4.5);
		}
		arrow.closePath();
		g.fill(arrow);
		g.dispose();
		return image;
	}

	private static BufferedImage star(Color color)
	{
		BufferedImage image = new BufferedImage(11, 11, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = brush(image, color, 1f);
		Path2D star = new Path2D.Float();
		for (int i = 0; i < 10; i++)
		{
			double angle = -Math.PI / 2 + i * Math.PI / 5;
			double radius = i % 2 == 0 ? 5.2 : 2.2;
			double x = 5.5 + radius * Math.cos(angle);
			double y = 5.8 + radius * Math.sin(angle);
			if (i == 0)
			{
				star.moveTo(x, y);
			}
			else
			{
				star.lineTo(x, y);
			}
		}
		star.closePath();
		g.fill(star);
		g.dispose();
		return image;
	}

	/** Lixeira: tampa, alça e corpo levemente afunilado. */
	private static BufferedImage trash(Color color)
	{
		BufferedImage image = new BufferedImage(12, 12, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = brush(image, color, 1.4f);
		g.drawLine(1, 3, 11, 3);
		g.drawLine(4, 1, 8, 1);
		g.drawLine(4, 1, 4, 3);
		g.drawLine(8, 1, 8, 3);
		Path2D body = new Path2D.Float();
		body.moveTo(2.5, 4);
		body.lineTo(3.5, 11);
		body.lineTo(8.5, 11);
		body.lineTo(9.5, 4);
		g.draw(body);
		g.drawLine(6, 5, 6, 9);
		g.dispose();
		return image;
	}

	private static BufferedImage cross(Color color)
	{
		BufferedImage image = new BufferedImage(12, 12, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = brush(image, color, 2f);
		g.drawLine(2, 2, 9, 9);
		g.drawLine(9, 2, 2, 9);
		g.dispose();
		return image;
	}

	private static BufferedImage check(Color color)
	{
		BufferedImage image = new BufferedImage(12, 12, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = brush(image, color, 2f);
		g.drawLine(2, 6, 5, 9);
		g.drawLine(5, 9, 10, 2);
		g.dispose();
		return image;
	}

	private static BufferedImage barChart(Color color)
	{
		BufferedImage image = new BufferedImage(12, 12, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = brush(image, color, 2f);
		g.drawLine(2, 10, 2, 6);
		g.drawLine(6, 10, 6, 2);
		g.drawLine(10, 10, 10, 5);
		g.dispose();
		return image;
	}
}
