package com.oldvicios;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Cliente HTTP da API do OldVicios. Todas as chamadas usam enqueue() (thread pool do OkHttp),
 * nunca a client thread.
 */
@Slf4j
@Singleton
class OldViciosApi
{
	/** Versão do plugin, enviada em toda chamada (X-OV-Version). Manter igual a runelite-plugin.properties. */
	static final String VERSION = "0.1.0";

	private static final String BASE_URL = "https://api.oldvicios.com.br/v1/plugin";
	private static final String ACCOUNT_SALT = "oldvicios-plugin-v1";
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static final MediaType PNG = MediaType.parse("image/png");

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private Gson gson;

	/**
	 * Identificador anônimo da conta: sha256(accountHash + salt). O accountHash do RuneLite
	 * é fixo por conta OSRS, então funciona em qualquer PC e sobrevive a troca de nick.
	 */
	static String accountId(long accountHash)
	{
		try
		{
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest((accountHash + ":" + ACCOUNT_SALT).getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder(hash.length * 2);
			for (byte b : hash)
			{
				hex.append(String.format("%02x", b));
			}
			return hex.toString();
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new IllegalStateException(e);
		}
	}

	/** Resultado de uma chamada: status HTTP (0 = falha de rede) + corpo `data` quando houver. */
	static final class Result
	{
		final int status;
		final JsonObject data;
		final JsonArray dataArray;
		final String error;

		Result(int status, JsonObject data, JsonArray dataArray, String error)
		{
			this.status = status;
			this.data = data;
			this.dataArray = dataArray;
			this.error = error;
		}

		boolean ok()
		{
			return status == 200 && data != null;
		}
	}

	void authenticate(String accountId, String playerName, Consumer<Result> callback)
	{
		JsonObject body = new JsonObject();
		body.addProperty("playerName", playerName);
		Request request = new Request.Builder()
			.url(BASE_URL + "/auth")
			.header("X-OV-Account", accountId)
			.post(RequestBody.create(JSON, gson.toJson(body)))
			.build();
		send(request, callback);
	}

	void submitLoot(String accountId, JsonObject payload, byte[] screenshotPng, Consumer<Result> callback)
	{
		MultipartBody.Builder body = new MultipartBody.Builder()
			.setType(MultipartBody.FORM)
			.addFormDataPart("payload", gson.toJson(payload));
		if (screenshotPng != null)
		{
			body.addFormDataPart("file", "drop.png", RequestBody.create(PNG, screenshotPng));
		}
		Request request = new Request.Builder()
			.url(BASE_URL + "/loot")
			.header("X-OV-Account", accountId)
			.post(body.build())
			.build();
		send(request, callback);
	}

	/**
	 * Lista de cards de uma aba ("notices" ou "events"). Exige a conta conectada e ainda no clã:
	 * 401/403 chama onDenied (o painel limpa o conteúdo); outros erros mantêm o que já está na tela.
	 */
	void fetchCards(String tab, String accountId, Consumer<List<ContentCard>> onSuccess, Runnable onDenied)
	{
		Request request = new Request.Builder()
			.url(BASE_URL + "/" + tab)
			.header("X-OV-Account", accountId)
			.cacheControl(CacheControl.FORCE_NETWORK)
			.get()
			.build();
		send(request, result ->
		{
			if (result.status == 401 || result.status == 403)
			{
				onDenied.run();
				return;
			}
			if (result.status != 200 || result.dataArray == null)
			{
				return;
			}
			List<ContentCard> cards = gson.fromJson(result.dataArray, new TypeToken<List<ContentCard>>()
			{
			}.getType());
			onSuccess.accept(cards != null ? cards : Collections.emptyList());
		});
	}

	/** Notificações depois do cursor. since null = só pega o cursor atual. accountId opcional. */
	void fetchUpdates(String accountId, Integer since, Consumer<Result> callback)
	{
		String url = BASE_URL + "/updates" + (since != null ? "?since=" + since : "");
		Request.Builder request = new Request.Builder().url(url).cacheControl(CacheControl.FORCE_NETWORK).get();
		if (accountId != null)
		{
			request.header("X-OV-Account", accountId);
		}
		send(request.build(), callback);
	}

	/** Baixa uma imagem (fora da client thread). null em caso de erro. */
	void fetchImage(String url, Consumer<BufferedImage> callback)
	{
		Request request = new Request.Builder().url(url).get().build();
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("OldVicios image download failed: {}", url, e);
				callback.accept(null);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody body = response.body(); InputStream in = body == null ? null : body.byteStream())
				{
					callback.accept(response.isSuccessful() && in != null ? ImageIO.read(in) : null);
				}
				catch (IOException e)
				{
					log.debug("OldVicios image decode failed: {}", url, e);
					callback.accept(null);
				}
			}
		});
	}

	private void send(Request unversioned, Consumer<Result> callback)
	{
		// Toda chamada pra API leva a versão, pro backend saber quem está desatualizado
		Request request = unversioned.newBuilder().header("X-OV-Version", VERSION).build();
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("OldVicios API request failed: {}", request.url(), e);
				callback.accept(new Result(0, null, null, "network_error"));
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody responseBody = response.body())
				{
					JsonObject json = responseBody == null ? null : gson.fromJson(responseBody.charStream(), JsonObject.class);
					JsonObject data = json != null && json.has("data") && json.get("data").isJsonObject()
						? json.getAsJsonObject("data") : null;
					JsonArray dataArray = json != null && json.has("data") && json.get("data").isJsonArray()
						? json.getAsJsonArray("data") : null;
					String error = json != null && json.has("error") && !json.get("error").isJsonNull()
						? json.get("error").getAsString() : null;
					callback.accept(new Result(response.code(), data, dataArray, error));
				}
				catch (Exception e)
				{
					log.debug("OldVicios API bad response: {}", request.url(), e);
					callback.accept(new Result(response.code(), null, null, "bad_response"));
				}
			}
		});
	}
}
