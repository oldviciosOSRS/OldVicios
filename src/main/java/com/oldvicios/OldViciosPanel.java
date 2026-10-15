package com.oldvicios;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

/**
 * Painel lateral: logo do OV, nome e o aviso atual, tudo centralizado no meio do painel.
 */
class OldViciosPanel extends PluginPanel
{
	private final JLabel status = new JLabel();

	OldViciosPanel()
	{
		// false = sem scroll, o painel ocupa a altura toda e dá pra centralizar
		super(false);
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setOpaque(false);

		JLabel logo = new JLabel(new ImageIcon(ImageUtil.loadImageResource(OldViciosPanel.class, "logo.png")));
		logo.setAlignmentX(Component.CENTER_ALIGNMENT);
		content.add(logo);
		content.add(Box.createVerticalStrut(10));

		JLabel title = new JLabel("OldVicios");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(ColorScheme.BRAND_ORANGE);
		title.setAlignmentX(Component.CENTER_ALIGNMENT);
		content.add(title);
		content.add(Box.createVerticalStrut(8));

		status.setFont(FontManager.getRunescapeSmallFont());
		status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		status.setHorizontalAlignment(SwingConstants.CENTER);
		status.setAlignmentX(Component.CENTER_ALIGNMENT);
		content.add(status);

		JPanel center = new JPanel(new GridBagLayout());
		center.setOpaque(false);
		center.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		center.add(content);
		add(center, BorderLayout.CENTER);

		setStatus("Entre no jogo para conectar");
	}

	// Must be called on the Swing EDT.
	void setStatus(String text)
	{
		// HTML pra quebrar linha e manter centralizado em mensagens longas
		status.setText("<html><div style='text-align:center;width:170px'>" + text + "</div></html>");
	}
}
