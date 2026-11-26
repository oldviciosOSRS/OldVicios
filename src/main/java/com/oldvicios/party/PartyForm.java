package com.oldvicios.party;

import java.awt.Dimension;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/**
 * Campos para criar um grupo. Depois de criado, o grupo não muda mais (não há edição).
 * Dois lootes: o do grupo (regra da PT) e o do criador (o modo que ele quer, como cada candidato escolhe o seu).
 */
final class PartyForm
{
	private final JTextField name = new JTextField();
	private final JComboBox<Activity> activity = PartyUi.combo(Activity.values());
	private final JComboBox<String> mode = PartyUi.combo(new String[0]);
	private final JSpinner slots = PartyUi.spinner(2, 2, 2);
	private final JSpinner world;
	private final JComboBox<LootMode> partyLoot = PartyUi.combo(LootMode.values());
	private final JComboBox<LootMode> ownLoot = PartyUi.combo(LootMode.values());
	private final JCheckBox learners = new JCheckBox();
	private final JSpinner minCombat = PartyUi.spinner(0, 0, 126);
	private final JSpinner minKillCount = PartyUi.spinner(0, 0, 99999);
	private final JTextField description = new JTextField();

	PartyForm(int defaultWorld)
	{
		world = PartyUi.spinner(defaultWorld, 301, 650);
		learners.setSelected(true);

		// Rótulos de loot são mais longos; as listas ficam mais estreitas para caber na linha
		partyLoot.setPreferredSize(new Dimension(105, 25));
		ownLoot.setPreferredSize(new Dimension(105, 25));
		partyLoot.setToolTipText("Regra de loot do grupo");
		ownLoot.setToolTipText("O loot que você quer (cada candidato também escolhe o dele)");
		// O loot do criador acompanha o do grupo até ele escolher outro
		partyLoot.addActionListener(e -> ownLoot.setSelectedItem(partyLoot.getSelectedItem()));

		activity.addActionListener(e -> onActivityChanged());
		onActivityChanged();
	}

	private void onActivityChanged()
	{
		Activity a = activity();
		mode.removeAllItems();
		for (String m : a.modes)
		{
			mode.addItem(m);
		}
		if (a.modes.length == 0)
		{
			mode.addItem("—");
		}
		mode.setEnabled(a.modes.length > 1);

		int current = Math.min(Math.max((int) slots.getValue(), a.minSlots), a.maxSlots);
		slots.setModel(new SpinnerNumberModel(current, a.minSlots, a.maxSlots, 1));
		PartyUi.setSpinnerColumns(slots);

		minKillCount.setEnabled(a.kcLabel != null);
		minKillCount.setToolTipText(a.kcLabel != null ? "Soma no hiscore: " + a.kcLabel : "Sem KC no hiscore");
	}

	/** Seções do formulário: dados do grupo (com o título dado), Requisitos e Descrição. */
	void addSections(JPanel screen, String title)
	{
		JPanel party = PartyUi.column();
		party.add(PartyUi.label("Nome", PartyUi.TEXT, false));
		party.add(name);
		party.add(PartyUi.row("Atividade", activity));
		party.add(PartyUi.row("Modo", mode));
		party.add(PartyUi.row("Vagas", slots));
		party.add(PartyUi.row("Mundo", world));
		party.add(PartyUi.row("Loot do grupo", partyLoot));
		party.add(PartyUi.row("Seu loot", ownLoot));
		party.add(PartyUi.row("Aceita learners", learners));
		screen.add(PartyUi.section(title, true, party));

		JPanel requirements = PartyUi.column();
		requirements.add(PartyUi.row("Combate mínimo", minCombat));
		requirements.add(PartyUi.row("KC mínimo", minKillCount));
		screen.add(PartyUi.section("Requisitos", true, requirements));

		JPanel text = PartyUi.column();
		text.add(description);
		screen.add(PartyUi.section("Descrição", true, text));
	}

	String name()
	{
		return name.getText();
	}

	Activity activity()
	{
		return (Activity) activity.getSelectedItem();
	}

	/** null quando a atividade não tem modos. */
	String mode()
	{
		return activity().modes.length > 0 ? (String) mode.getSelectedItem() : null;
	}

	int slots()
	{
		return (int) slots.getValue();
	}

	int world()
	{
		return (int) world.getValue();
	}

	/** Regra de loot do grupo. */
	LootMode partyLoot()
	{
		return (LootMode) partyLoot.getSelectedItem();
	}

	/** Loot que o criador quer para ele. */
	LootMode ownLoot()
	{
		return (LootMode) ownLoot.getSelectedItem();
	}

	boolean learners()
	{
		return learners.isSelected();
	}

	int minCombat()
	{
		return (int) minCombat.getValue();
	}

	/** 0 quando a atividade não tem KC no hiscore. */
	int minKillCount()
	{
		return activity().kcLabel != null ? (int) minKillCount.getValue() : 0;
	}

	String description()
	{
		return description.getText().trim();
	}
}
