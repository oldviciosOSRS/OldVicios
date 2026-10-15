package com.oldvicios;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Card de aviso/evento. Fechado: imagem, título, data e "Ver mais". Aberto: + texto completo.
 */
class CardPanel extends JPanel
{
	// Largura usada antes do card ser exibido (depois ela acompanha a largura real do card)
	private static final int DEFAULT_IMAGE_WIDTH = 190;
	private static final int TEXT_WIDTH = 180;
	private static final ZoneId BRT = ZoneId.of("America/Sao_Paulo");
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM");

	private final JLabel image = new JLabel();
	private final JLabel body = new JLabel();
	private final JLabel toggle = new JLabel();
	private boolean expanded;
	private BufferedImage source;
	private int imageWidth;

	enum Kind
	{
		NOTICE,
		EVENT
	}

	CardPanel(ContentCard card, Kind kind)
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(6, 6, 8, 6));

		image.setAlignmentX(Component.LEFT_ALIGNMENT);
		image.setVisible(false);
		add(image);

		// A imagem ocupa a largura toda do card, qualquer que seja a largura do painel
		addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				renderImage();
			}
		});

		JLabel title = new JLabel(html(escape(card.title)));
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		title.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(Box.createVerticalStrut(4));
		add(title);

		String dateText = formatDate(card.date, kind);
		if (card.pinned)
		{
			dateText = dateText == null ? "Fixado" : "Fixado · " + dateText;
		}
		if (dateText != null)
		{
			JLabel date = new JLabel(dateText);
			date.setFont(FontManager.getRunescapeSmallFont());
			date.setForeground(kind == Kind.EVENT ? ColorScheme.BRAND_ORANGE : ColorScheme.LIGHT_GRAY_COLOR);
			date.setAlignmentX(Component.LEFT_ALIGNMENT);
			add(date);
		}

		boolean hasText = card.text != null && !card.text.isEmpty();
		body.setText(hasText ? html(markdownToHtml(card.text)) : "");
		body.setFont(FontManager.getRunescapeSmallFont());
		body.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		body.setAlignmentX(Component.LEFT_ALIGNMENT);
		body.setVisible(false);
		add(Box.createVerticalStrut(4));
		add(body);

		if (hasText)
		{
			toggle.setFont(FontManager.getRunescapeSmallFont());
			toggle.setForeground(ColorScheme.BRAND_ORANGE);
			toggle.setAlignmentX(Component.LEFT_ALIGNMENT);
			// O card inteiro abre/fecha. mousePressed (e não mouseClicked) porque o clicked
			// é descartado se o mouse mexer um pixel entre apertar e soltar.
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
					if (javax.swing.SwingUtilities.isLeftMouseButton(e))
					{
						setExpanded(!expanded);
					}
				}
			});
			add(Box.createVerticalStrut(2));
			add(toggle);
			setExpanded(false);
		}
	}

	private void setExpanded(boolean expanded)
	{
		this.expanded = expanded;
		body.setVisible(expanded);
		toggle.setText(expanded ? "Ver menos ▴" : "Ver mais ▾");
		revalidate();
		repaint();
	}

	// Must be called on the Swing EDT.
	void setImage(BufferedImage source)
	{
		if (source == null)
		{
			return;
		}
		this.source = source;
		imageWidth = 0;
		renderImage();
	}

	private void renderImage()
	{
		if (source == null)
		{
			return;
		}
		Insets insets = getInsets();
		int width = getWidth() > 0 ? getWidth() - insets.left - insets.right : DEFAULT_IMAGE_WIDTH;
		if (width <= 0 || width == imageWidth)
		{
			return;
		}
		imageWidth = width;

		// Igual a "width: 100%; height: auto" no CSS: cabe na largura e a altura acompanha, sem cortar
		int height = Math.max(1, Math.round(source.getHeight() * (width / (float) source.getWidth())));
		image.setIcon(new ImageIcon(source.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH)));
		image.setVisible(true);
		revalidate();
		repaint();
	}

	@Override
	public Dimension getMaximumSize()
	{
		// BoxLayout estica na largura, mas não na altura
		return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
	}

	/** "31/10 às 20h · em 16 dias" (evento) ou "Publicado em 15/10" (aviso), no horário de Brasília. */
	static String formatDate(String iso, Kind kind)
	{
		if (iso == null)
		{
			return null;
		}
		ZonedDateTime date;
		try
		{
			date = ZonedDateTime.parse(iso).withZoneSameInstant(BRT);
		}
		catch (Exception e)
		{
			return null;
		}
		if (kind == Kind.NOTICE)
		{
			return "Publicado em " + date.format(DAY);
		}

		String hour = date.getMinute() == 0
			? date.getHour() + "h"
			: date.getHour() + "h" + String.format("%02d", date.getMinute());
		long days = ChronoUnit.DAYS.between(LocalDate.now(BRT), date.toLocalDate());
		String relative = days <= 0 ? "hoje" : days == 1 ? "amanhã" : "em " + days + " dias";
		return date.format(DAY) + " às " + hour + " · " + relative;
	}

	static String escape(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/** Markdown simples do Discord → HTML do Swing: **negrito**, *itálico*, __sublinhado__, [texto](link) e quebras de linha. */
	static String markdownToHtml(String markdown)
	{
		return escape(markdown)
			.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>")
			.replaceAll("__(.+?)__", "<u>$1</u>")
			.replaceAll("\\*(.+?)\\*", "<i>$1</i>")
			.replaceAll("\\[(.+?)]\\(\\S+?\\)", "$1")
			.replace("\n", "<br>");
	}

	private static String html(String content)
	{
		return "<html><div style='width:" + TEXT_WIDTH + "px'>" + content + "</div></html>";
	}
}
