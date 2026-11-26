package com.oldvicios.party;

/** Como o loot do grupo é dividido. */
enum LootMode
{
	FFA("FFA"),
	SPLIT("Split"),
	SPLIT_DUPES("Split Dupes");

	final String displayName;

	LootMode(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
