package com.oldvicios;

import java.awt.BorderLayout;
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
 * Painel lateral: cabeçalho (logo do OV + status da conexão) e abas Avisos/Eventos com cards.
 * Todos os métodos devem ser chamados na Swing EDT.
 */
class OldViciosPanel extends PluginPanel
{
	private static final int LOGO_WIDTH = 56;
	private static final int LINK_ICON_SIZE = 16;
	static final String LOCKED_MESSAGE = "<html><div style='text-align:center'>Entre no jogo com um personagem<br>do clã para ver o conteúdo</div></html>";
	private static final String SITE_URL = "https://oldvicios.com.br";
	private static final String DISCORD_URL = "https://discord.gg/GV5qbrGyAA";
	private static final String WOM_URL = "https://wiseoldman.net/groups/715";

	private final JLabel status = new JLabel();
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

		JPanel top = new JPanel(new BorderLayout());
		top.setOpaque(false);
		top.add(configButton(openConfig), BorderLayout.NORTH);
		top.add(header(), BorderLayout.CENTER);
		add(top, BorderLayout.NORTH);

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
		add(content, BorderLayout.CENTER);
		add(footer(), BorderLayout.SOUTH);

		setStatus("Entre no jogo para conectar");
		lockContent(LOCKED_MESSAGE);
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
				label.setForeground(ColorScheme.BRAND_ORANGE);
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
		title.setForeground(ColorScheme.BRAND_ORANGE);
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
	}

	void setStatus(String text)
	{
		status.setIcon(null);
		status.setToolTipText(null);
		status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		status.setFont(FontManager.getRunescapeSmallFont());
		status.setText("<html><div style='text-align:center;width:180px'>" + CardPanel.escape(text) + "</div></html>");
	}

	void setNotices(List<ContentCard> cards)
	{
		fill(noticesList, cards, CardPanel.Kind.NOTICE, "Nenhum aviso no momento");
	}

	void setEvents(List<ContentCard> cards)
	{
		fill(eventsList, cards, CardPanel.Kind.EVENT, "Nenhum evento agendado");
	}

	/** Tira o conteúdo do clã da tela (deslogado, fora do clã ou bloqueado). */
	void lockContent(String reason)
	{
		showMessage(noticesList, reason);
		showMessage(eventsList, reason);
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
