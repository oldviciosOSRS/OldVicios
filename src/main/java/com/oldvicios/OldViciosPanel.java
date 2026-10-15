package com.oldvicios;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

/**
 * Painel lateral com duas telas: boas-vindas (deslogado / sem acesso: logo, descrição do clã e o
 * motivo) e a do clã (cabeçalho "[cargo] nick" + abas Avisos/Eventos). Rodapé com links em ambas.
 * Todos os métodos devem ser chamados na Swing EDT.
 */
class OldViciosPanel extends PluginPanel
{
	// Amarelo do OV: tom das letras do logo (miolo das letras do favicon do site)
	static final java.awt.Color YELLOW = new java.awt.Color(0xECE65C);

	private static final int LOGO_WIDTH = 56;
	private static final int WELCOME_LOGO_WIDTH = 96;
	private static final String WELCOME_CARD = "welcome";
	private static final String CLAN_CARD = "clan";
	private static final int LINK_ICON_SIZE = 16;
	private static final String SITE_URL = "https://oldvicios.com.br";
	private static final String DISCORD_URL = "https://discord.gg/GV5qbrGyAA";
	private static final String WOM_URL = "https://wiseoldman.net/groups/715";

	private final JLabel status = new JLabel();
	private final JLabel welcomeMessage = new JLabel();
	private final CardLayout cards = new CardLayout();
	private final JPanel center = new JPanel(cards);
	private final JPanel noticesList = cardList();
	private final JPanel eventsList = cardList();
	private final BiConsumer<String, Consumer<BufferedImage>> imageLoader;

	/**
	 * @param imageLoader baixa (ou pega do cache) a imagem de uma URL e chama o callback na EDT
	 */
	OldViciosPanel(BiConsumer<String, Consumer<BufferedImage>> imageLoader, Runnable openConfig)
	{
		// false = sem scroll externo; cada aba tem o seu
		super(false);
		this.imageLoader = imageLoader;
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		add(configButton(openConfig), BorderLayout.NORTH);

		center.setOpaque(false);
		center.add(welcome(), WELCOME_CARD);
		center.add(clan(), CLAN_CARD);
		add(center, BorderLayout.CENTER);
		add(footer(), BorderLayout.SOUTH);

		showWelcome("Entre no jogo com seu personagem do clã para conectar.");
	}

	/** Tela de deslogado / sem acesso: logo e nome centralizados, descrição do clã e o motivo. */
	private JPanel welcome()
	{
		JPanel box = new JPanel();
		box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
		box.setOpaque(false);

		BufferedImage logoImage = ImageUtil.loadImageResource(OldViciosPanel.class, "logo.png");
		int logoHeight = Math.round(logoImage.getHeight() * (WELCOME_LOGO_WIDTH / (float) logoImage.getWidth()));
		JLabel logo = new JLabel(new ImageIcon(ImageUtil.resizeImage(logoImage, WELCOME_LOGO_WIDTH, logoHeight)));
		logo.setAlignmentX(Component.CENTER_ALIGNMENT);
		box.add(logo);
		box.add(Box.createVerticalStrut(10));

		JLabel title = new JLabel("OldVicios");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(YELLOW);
		title.setAlignmentX(Component.CENTER_ALIGNMENT);
		box.add(title);
		box.add(Box.createVerticalStrut(8));

		JLabel description = new JLabel(centered("A maior comunidade brasileira de Old School RuneScape."));
		description.setFont(FontManager.getRunescapeSmallFont());
		description.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		description.setAlignmentX(Component.CENTER_ALIGNMENT);
		box.add(description);
		box.add(Box.createVerticalStrut(16));

		welcomeMessage.setFont(FontManager.getRunescapeSmallFont());
		welcomeMessage.setForeground(java.awt.Color.WHITE);
		welcomeMessage.setAlignmentX(Component.CENTER_ALIGNMENT);
		box.add(welcomeMessage);

		JPanel welcome = new JPanel(new GridBagLayout());
		welcome.setOpaque(false);
		welcome.setBorder(BorderFactory.createEmptyBorder(0, 10, 20, 10));
		welcome.add(box);
		return welcome;
	}

	/** Tela do clã: cabeçalho com o jogador e as abas. */
	private JPanel clan()
	{
		JPanel display = new JPanel(new BorderLayout());
		display.setOpaque(false);
		MaterialTabGroup tabs = new MaterialTabGroup(display);
		tabs.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
		MaterialTab noticesTab = new MaterialTab("Avisos", tabs, scroll(noticesList));
		MaterialTab eventsTab = new MaterialTab("Eventos", tabs, scroll(eventsList));
		tabs.addTab(noticesTab);
		tabs.addTab(eventsTab);
		tabs.select(noticesTab);

		JPanel content = new JPanel(new BorderLayout());
		content.setOpaque(false);
		content.setBorder(BorderFactory.createEmptyBorder(0, 6, 6, 6));
		content.add(tabs, BorderLayout.NORTH);
		content.add(display, BorderLayout.CENTER);

		JPanel clan = new JPanel(new BorderLayout());
		clan.setOpaque(false);
		clan.add(header(), BorderLayout.NORTH);
		clan.add(content, BorderLayout.CENTER);
		return clan;
	}

	private static String centered(String text)
	{
		return "<html><div style='text-align:center;width:170px'>" + text + "</div></html>";
	}

	/** Engrenagem no canto superior direito que abre as configurações do plugin. */
	private static JPanel configButton(Runnable openConfig)
	{
		JButton button = new JButton(new ImageIcon(ImageUtil.loadImageResource(OldViciosPanel.class, "config.png")));
		button.setToolTipText("Configurações do OldVicios");
		button.setBorderPainted(false);
		button.setContentAreaFilled(false);
		button.setFocusPainted(false);
		button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
		button.addActionListener(e -> openConfig.run());

		JPanel row = new JPanel(new BorderLayout());
		row.setOpaque(false);
		row.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 4));
		row.add(button, BorderLayout.EAST);
		return row;
	}

	/** Rodapé com os links do site, Discord e WOM (ícone + nome, clicáveis). */
	private static JPanel footer()
	{
		JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
		footer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		footer.add(link("site.png", "Site", SITE_URL));
		footer.add(link("discord.png", "Discord", DISCORD_URL));
		footer.add(link("wom.png", "WOM", WOM_URL));
		return footer;
	}

	private static JLabel link(String icon, String text, String url)
	{
		BufferedImage image = ImageUtil.loadImageResource(OldViciosPanel.class, icon);
		int height = Math.round(image.getHeight() * (LINK_ICON_SIZE / (float) image.getWidth()));
		JLabel label = new JLabel(text, new ImageIcon(ImageUtil.resizeImage(image, LINK_ICON_SIZE, height)), SwingConstants.LEFT);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setIconTextGap(4);
		label.setToolTipText(url);
		label.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
		label.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				LinkBrowser.browse(url);
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				label.setForeground(YELLOW);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			}
		});
		return label;
	}

	private JPanel header()
	{
		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setOpaque(false);
		header.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

		BufferedImage logoImage = ImageUtil.loadImageResource(OldViciosPanel.class, "logo.png");
		int logoHeight = Math.round(logoImage.getHeight() * (LOGO_WIDTH / (float) logoImage.getWidth()));
		JLabel logo = new JLabel(new ImageIcon(ImageUtil.resizeImage(logoImage, LOGO_WIDTH, logoHeight)));
		logo.setAlignmentX(Component.CENTER_ALIGNMENT);
		header.add(logo);
		header.add(Box.createVerticalStrut(6));

		JLabel title = new JLabel("OldVicios");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(YELLOW);
		title.setAlignmentX(Component.CENTER_ALIGNMENT);
		header.add(title);
		header.add(Box.createVerticalStrut(2));

		status.setFont(FontManager.getRunescapeSmallFont());
		status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		status.setAlignmentX(Component.CENTER_ALIGNMENT);
		header.add(status);
		return header;
	}

	private static JPanel cardList()
	{
		JPanel list = new JPanel();
		list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
		list.setBackground(ColorScheme.DARK_GRAY_COLOR);
		return list;
	}

	private static JScrollPane scroll(JPanel list)
	{
		// Wrapper com BorderLayout.NORTH pra lista não esticar quando tem poucos cards,
		// travado na largura visível (senão o conteúdo fica mais largo e corta na direita)
		JPanel wrapper = new ViewportWidthPanel();
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.add(list, BorderLayout.NORTH);

		JScrollPane scroll = new JScrollPane(wrapper);
		scroll.setBorder(null);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		return scroll;
	}

	/** "[ícone do cargo] nick", como no Clan Chat. Sem ícone, mostra o nome do cargo em texto. */
	void setPlayer(BufferedImage rankIcon, String rankName, String name)
	{
		status.setIcon(rankIcon != null ? new ImageIcon(rankIcon) : null);
		status.setToolTipText(rankName);
		String text = rankIcon == null && rankName != null ? rankName + " " + name : name;
		status.setText(CardPanel.escape(text));
		status.setForeground(java.awt.Color.WHITE);
		status.setFont(FontManager.getRunescapeBoldFont());
		cards.show(center, CLAN_CARD);
	}

	/** Mostra a tela de boas-vindas com a mensagem e tira o conteúdo do clã da tela. */
	void showWelcome(String message)
	{
		welcomeMessage.setText(centered(CardPanel.escape(message)));
		noticesList.removeAll();
		eventsList.removeAll();
		cards.show(center, WELCOME_CARD);
	}

	void setNotices(List<ContentCard> cards)
	{
		fill(noticesList, cards, CardPanel.Kind.NOTICE, "Nenhum aviso no momento");
	}

	void setEvents(List<ContentCard> cards)
	{
		fill(eventsList, cards, CardPanel.Kind.EVENT, "Nenhum evento agendado");
	}

	private void fill(JPanel list, List<ContentCard> cards, CardPanel.Kind kind, String emptyMessage)
	{
		list.removeAll();
		if (cards.isEmpty())
		{
			showMessage(list, emptyMessage);
			return;
		}
		for (ContentCard card : cards)
		{
			CardPanel panel = new CardPanel(card, kind);
			if (card.imageUrl != null && !card.imageUrl.isEmpty())
			{
				imageLoader.accept(card.imageUrl, panel::setImage);
			}
			list.add(panel);
			list.add(Box.createVerticalStrut(6));
		}
		list.revalidate();
		list.repaint();
	}

	private static void showMessage(JPanel list, String message)
	{
		list.removeAll();
		JPanel holder = new JPanel(new GridBagLayout());
		holder.setOpaque(false);
		holder.setBorder(BorderFactory.createEmptyBorder(30, 0, 30, 0));
		JLabel label = new JLabel(message, SwingConstants.CENTER);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		holder.add(label);
		list.add(holder);
		list.revalidate();
		list.repaint();
	}

	/** Painel que sempre tem a largura da área visível do scroll (como width: 100% no CSS). */
	private static class ViewportWidthPanel extends JPanel implements Scrollable
	{
		ViewportWidthPanel()
		{
			super(new BorderLayout());
		}

		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return visibleRect.height;
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}
	}
}
