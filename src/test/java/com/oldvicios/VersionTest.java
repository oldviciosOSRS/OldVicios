package com.oldvicios;

import static org.junit.Assert.assertEquals;
import java.io.FileInputStream;
import java.io.InputStream;
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
}
