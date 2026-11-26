package com.oldvicios.party;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Cliente da API dos grupos (/v1/plugin/party). Tudo assíncrono no thread pool do OkHttp (enqueue),
 * nunca na client thread; os callbacks rodam no thread do OkHttp (a aba repassa para a EDT).
 * A URL é sempre montada a partir da base fixa do plugin (api.oldvicios.com.br).
 */
@Slf4j
public class PartyApi
{
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static final Type PARTY_LIST = new TypeToken<List<Party>>()
	{
	}.getType();

	/** Resultado de uma chamada: status HTTP (0 = falha de rede), data quando deu certo, código de erro quando não. */
	static final class Result<T>
	{
		final int status;
		final T data;
		final String error;

		Result(int status, T data, String error)
		{
			this.status = status;
			this.data = data;
			this.error = error;
		}

		boolean ok()
		{
			return status >= 200 && status < 300 && error == null;
		}
	}

	private final OkHttpClient http;
	private final Gson gson;
	private final HttpUrl base;
	private final String version;
	private final Supplier<String> accountId;

	/**
	 * @param pluginBaseUrl base da API do plugin (https://api.oldvicios.com.br/v1/plugin)
	 * @param accountId     X-OV-Account da conta conectada (null = desconectado)
	 */
	public PartyApi(OkHttpClient http, Gson gson, String pluginBaseUrl, String version, Supplier<String> accountId)
	{
		this.http = http;
		this.gson = gson;
		this.base = HttpUrl.get(pluginBaseUrl).newBuilder().addPathSegment("party").build();
		this.version = version;
		this.accountId = accountId;
	}

	void status(Consumer<Result<MyStatus>> callback)
	{
		call("GET", url("me"), null, MyStatus.class, callback);
	}

	void list(Activity filter, Consumer<Result<List<Party>>> callback)
	{
		HttpUrl.Builder url = base.newBuilder().addPathSegment("parties");
		if (filter != null)
		{
			url.addQueryParameter("activity", filter.name());
		}
		call("GET", url.build(), null, PARTY_LIST, callback);
	}

	void get(String partyId, Consumer<Result<Party>> callback)
	{
		call("GET", url("parties", partyId), null, Party.class, callback);
	}

	void create(JsonObject body, Consumer<Result<Party>> callback)
	{
		call("POST", url("parties"), body, Party.class, callback);
	}

	void close(String partyId, Consumer<Result<Void>> callback)
	{
		call("DELETE", url("parties", partyId), null, null, callback);
	}

	void setApplicationsOpen(String partyId, boolean open, Consumer<Result<Party>> callback)
	{
		JsonObject body = new JsonObject();
		body.addProperty("open", open);
		call("PUT", url("parties", partyId, "applications-open"), body, Party.class, callback);
	}

	void apply(String partyId, JsonObject body, Consumer<Result<Party>> callback)
	{
		call("POST", url("parties", partyId, "applications"), body, Party.class, callback);
	}

	void accept(String partyId, int applicantId, Consumer<Result<Party>> callback)
	{
		call("POST", url("parties", partyId, "applications", Integer.toString(applicantId), "accept"), new JsonObject(), Party.class, callback);
	}

	void decline(String partyId, int applicantId, Consumer<Result<Party>> callback)
	{
		call("POST", url("parties", partyId, "applications", Integer.toString(applicantId), "decline"), new JsonObject(), Party.class, callback);
	}

	void kick(String partyId, int memberId, Consumer<Result<Party>> callback)
	{
		call("DELETE", url("parties", partyId, "members", Integer.toString(memberId)), null, Party.class, callback);
	}

	void cancelApplication(Consumer<Result<Void>> callback)
	{
		call("DELETE", url("me", "application"), null, null, callback);
	}

	void leave(Consumer<Result<Void>> callback)
	{
		call("DELETE", url("me", "membership"), null, null, callback);
	}

	private HttpUrl url(String... segments)
	{
		HttpUrl.Builder url = base.newBuilder();
		for (String segment : segments)
		{
			// addPathSegment codifica o segmento (o id do grupo vem do servidor)
			url.addPathSegment(segment);
		}
		return url.build();
	}

	private <T> void call(String method, HttpUrl url, JsonObject body, Type dataType, Consumer<Result<T>> callback)
	{
		String account = accountId.get();
		if (account == null)
		{
			callback.accept(new Result<>(401, null, "not_connected"));
			return;
		}
		RequestBody requestBody = body != null ? RequestBody.create(JSON, gson.toJson(body)) : null;
		Request request = new Request.Builder()
			.url(url)
			.header("X-OV-Account", account)
			.header("X-OV-Version", version)
			.cacheControl(CacheControl.FORCE_NETWORK)
			.method(method, requestBody)
			.build();
		http.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("OldVicios party request failed: {} {}", method, url, e);
				callback.accept(new Result<>(0, null, "network_error"));
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody responseBody = response.body())
				{
					JsonObject json = responseBody == null ? null : gson.fromJson(responseBody.charStream(), JsonObject.class);
					String error = json != null && json.has("error") && !json.get("error").isJsonNull()
						? json.get("error").getAsString() : null;
					if (!response.isSuccessful() && error == null)
					{
						error = "http_" + response.code();
					}
					JsonElement data = json != null && json.has("data") ? json.get("data") : null;
					T parsed = dataType != null && data != null && !data.isJsonNull() ? gson.fromJson(data, dataType) : null;
					callback.accept(new Result<>(response.code(), parsed, error));
				}
				catch (Exception e)
				{
					log.debug("OldVicios party bad response: {} {}", method, url, e);
					callback.accept(new Result<>(response.code(), null, "bad_response"));
				}
			}
		});
	}
}
