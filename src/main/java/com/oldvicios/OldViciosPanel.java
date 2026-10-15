package com.oldvicios;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

class OldViciosPanel extends PluginPanel
{
	private final JLabel status = new JLabel();

	OldViciosPanel()
	{
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JLabel title = new JLabel("Old Vicios", SwingConstants.CENTER);
		title.setFont(FontManager.getRunescapeBoldFont());
		add(title, BorderLayout.NORTH);

		status.setHorizontalAlignment(SwingConstants.CENTER);
		add(status, BorderLayout.CENTER);

		setStatus("Desconectado");
	}

	// Must be called on the Swing EDT.
	void setStatus(String text)
	{
		status.setText(text);
	}
}
