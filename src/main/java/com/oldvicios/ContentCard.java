package com.oldvicios;

/** Item das abas Avisos/Eventos, no formato de /v1/plugin/notices e /v1/plugin/events. */
class ContentCard
{
	int id;
	String title;
	String text;
	String imageUrl;
	// ISO-8601. Avisos: data de publicação; eventos: data do evento.
	String date;
	boolean pinned;
}
