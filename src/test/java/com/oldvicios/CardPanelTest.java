package com.oldvicios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.Test;

public class CardPanelTest
{
	@Test
	public void convertsDiscordMarkdown()
	{
		assertEquals("<b>Bingo</b> de <i>Halloween</i><br><u>dia</u> 31",
			CardPanel.markdownToHtml("**Bingo** de *Halloween*\n__dia__ 31"));
	}

	@Test
	public void escapesHtmlAndStripsLinks()
	{
		assertEquals("a &lt;b&gt; site", CardPanel.markdownToHtml("a <b> [site](https://x.com)"));
	}

	@Test
	public void formatsEventDateInBrasiliaTime()
	{
		// 23:00 UTC = 20:00 em Brasília
		String iso = ZonedDateTime.now(ZoneId.of("UTC")).plusDays(16).withHour(23).withMinute(0).toString();
		String formatted = CardPanel.formatDate(iso, CardPanel.Kind.EVENT);
		assertTrue(formatted, formatted.contains(" às 20h · em "));
	}

	@Test
	public void formatsNoticeDate()
	{
		assertEquals("Publicado em 15/10", CardPanel.formatDate("2026-10-15T15:00:00.000Z", CardPanel.Kind.NOTICE));
	}
}
