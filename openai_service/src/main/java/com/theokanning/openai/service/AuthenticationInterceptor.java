package com.theokanning.openai.service;

import java.io.IOException;
import java.util.Objects;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp Interceptor that adds an authorization token header
 */
public class AuthenticationInterceptor implements Interceptor {

    private String token;
    private String url = null;

    AuthenticationInterceptor(String token) {
        Objects.requireNonNull(token, "OpenAI token required");
        this.token = token;
    }

    public void setToken(String token) {
        Objects.requireNonNull(token, "OpenAI token required");
        this.token = token;
    }
    public void setUrl(String url) {
        Objects.requireNonNull(token, "OpenAI url required");
        this.url = url;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request()
                .newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();

        if (url != null) {
            Request.Builder builder = request.newBuilder();
            HttpUrl oldHttpUrl = request.url();

            HttpUrl newBaseUrl = HttpUrl.parse(url);

            if (newBaseUrl == null) {
                return chain.proceed(request);
            }

            HttpUrl newFullUrl = oldHttpUrl
                    .newBuilder()
                    .scheme(newBaseUrl.scheme())
                    .host(newBaseUrl.host())
                    .port(newBaseUrl.port())
                    .build();

            return chain.proceed(builder.url(newFullUrl).build());

        } else {
            return chain.proceed(request);

        }

    }
}
