package com.oldvicios.loot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class KillCountServiceTest
{
	private static String kc(String message)
	{
		KillCountService.BossCount parsed = KillCountService.parseBoss(message);
		return parsed != null ? parsed.boss + "=" + parsed.count : null;
	}

	@Test
	public void parsesBossKillCount()
	{
		assertEquals("Maggot King=149", kc("Your Maggot King kill count is: 149."));
		assertEquals("Vorkath=1234", kc("Your Vorkath kill count is: 1,234."));
	}

	@Test
	public void parsesRaidsWithMode()
	{
		assertEquals("Chambers of Xeric=50", kc("Your completed Chambers of Xeric count is: 50."));
		assertEquals("Chambers of Xeric Challenge Mode=3", kc("Your completed Chambers of Xeric Challenge Mode count is: 3."));
		assertEquals("Tombs of Amascut: Expert Mode=12", kc("Your completed Tombs of Amascut: Expert Mode count is: 12."));
		assertEquals("Theatre of Blood: Hard Mode=7", kc("Your completed Theatre of Blood: Hard Mode count is: 7."));
	}

	@Test
	public void parsesChestsAndGauntlet()
	{
		assertEquals("Barrows=300", kc("Your Barrows chest count is: 300."));
		assertEquals("Crystalline Hunllef=25", kc("Your Gauntlet completion count is: 25."));
		assertEquals("Corrupted Hunllef=8", kc("Your Corrupted Gauntlet completion count is: 8."));
		assertEquals("Wintertodt=10", kc("Your subdued Wintertodt count is: 10."));
	}

	@Test
	public void ignoresOtherMessages()
	{
		assertNull(kc("Fight duration: 1:34. Personal best: 1:20"));
		assertNull(kc("Your completed Some Quest count is: 3."));
	}

	@Test
	public void cleansBossNameLikeChatCommands()
	{
		assertEquals("gauntlet", KillCountService.cleanBossName("The Gauntlet"));
		assertEquals("corrupted gauntlet", KillCountService.cleanBossName("Corrupted Hunllef"));
		assertEquals("whisperer", KillCountService.cleanBossName("The Whisperer"));
		assertEquals("barrows chests", KillCountService.cleanBossName("Barrows"));
		assertEquals("tombs of amascut expert mode", KillCountService.cleanBossName("Tombs of Amascut: Expert Mode"));
		assertEquals("vorkath", KillCountService.cleanBossName("Vorkath"));
	}
}
