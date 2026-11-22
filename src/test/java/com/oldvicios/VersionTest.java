package com.oldvicios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import org.junit.Test;

public class VersionTest
{
	/** A versão no código (header X-OV-Version) tem que ser a mesma do manifesto do Plugin Hub. */
	@Test
	public void codeVersionMatchesPluginProperties() throws Exception
	{
		Properties properties = new Properties();
		try (InputStream in = new FileInputStream("runelite-plugin.properties"))
		{
			properties.load(in);
		}
		assertEquals(properties.getProperty("version"), OldViciosApi.VERSION);
	}

	/**
	 * O Plugin Hub lê o arquivo como ISO-8859-1: "ã" em UTF-8 aparecia como "Ã£" na descrição.
	 * Acentos vão escapados (cl\u00e3 no arquivo).
	 */
	@Test
	public void pluginPropertiesAreAscii() throws Exception
	{
		byte[] bytes = Files.readAllBytes(Paths.get("runelite-plugin.properties"));
		for (byte b : bytes)
		{
			assertTrue("runelite-plugin.properties tem caractere fora do ASCII; use \\uXXXX", b >= 0);
		}
		Properties properties = new Properties();
		try (InputStream in = new FileInputStream("runelite-plugin.properties"))
		{
			properties.load(in);
		}
		assertEquals("Plugin oficial do clã OldVicios", properties.getProperty("description"));
	}
}
