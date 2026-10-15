package com.oldvicios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import java.util.regex.Matcher;
import org.junit.Test;

public class KillCountPatternTest
{
	private static String kc(String message)
	{
		Matcher m = OldViciosPlugin.KILL_COUNT.matcher(message);
		return m.find() ? m.group("name") + "=" + m.group("kc") : null;
	}

	@Test
	public void matchesBossKillCount()
	{
		assertEquals("Maggot King=149", kc("Your Maggot King kill count is: 149."));
		assertEquals("Vorkath=1,234", kc("Your Vorkath kill count is: 1,234."));
	}

	@Test
	public void matchesRaidsAndChests()
	{
		assertEquals("Chambers of Xeric=50", kc("Your completed Chambers of Xeric count is: 50."));
		assertEquals("Tombs of Amascut: Expert Mode=12", kc("Your completed Tombs of Amascut: Expert Mode count is: 12."));
		assertEquals("Barrows=300", kc("Your Barrows chest count is: 300."));
	}

	@Test
	public void ignoresOtherMessages()
	{
		assertFalse(OldViciosPlugin.KILL_COUNT.matcher("Fight duration: 1:34. Personal best: 1:20").find());
	}
}
