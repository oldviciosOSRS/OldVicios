package com.oldvicios;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
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
		final String error;

		Result(int status, JsonObject data, String error)
		{
			this.status = status;
			this.data = data;
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

	private void send(Request request, Consumer<Result> callback)
	{
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("OldVicios API request failed: {}", request.url(), e);
				callback.accept(new Result(0, null, "network_error"));
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody responseBody = response.body())
				{
					JsonObject json = responseBody == null ? null : gson.fromJson(responseBody.charStream(), JsonObject.class);
					JsonObject data = json != null && json.has("data") && json.get("data").isJsonObject()
						? json.getAsJsonObject("data") : null;
					String error = json != null && json.has("error") ? json.get("error").getAsString() : null;
					callback.accept(new Result(response.code(), data, error));
				}
				catch (Exception e)
				{
					log.debug("OldVicios API bad response: {}", request.url(), e);
					callback.accept(new Result(response.code(), null, "bad_response"));
				}
			}
		});
	}
}
