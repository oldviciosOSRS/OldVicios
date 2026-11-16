package com.oldvicios;

/** Item das abas Avisos/Eventos, no formato de /v1/plugin/notices e /v1/plugin/events. */
class ContentCard
{
	int id;
	String title;
	String text;
	// Muda quando a staff troca a imagem; null = sem imagem. A imagem vem de
	// /v1/plugin/{notices|events}/{id}/image: o plugin nunca recebe URL de imagem do servidor.
	String imageVersion;
	// ISO-8601. Avisos: data de publicação; eventos: data do evento.
	String date;
	boolean pinned;
}
