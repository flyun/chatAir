package com.theokanning.openai.service;

import android.text.TextUtils;

import java.io.IOException;
import java.util.ArrayList;
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
    private String oldUrl;
    private String url = null;
    private boolean isOpenRouter = false;

    private boolean isGoogle = false;

    AuthenticationInterceptor(String token, String oldUrl, boolean isGoogle) {
//        Objects.requireNonNull(token, "Token required");
        this.token = token;
        this.oldUrl = oldUrl;
        this.isGoogle = isGoogle;
        checkOpenRouter(oldUrl);
    }

    public void setToken(String token) {
//        Objects.requireNonNull(token, "Token required");
        this.token = token;
    }
    public void setUrl(String url) {
//        Objects.requireNonNull(token, "Url required");
        this.url = url;
        checkOpenRouter(url);
    }

    public void setGoogle(boolean google) {
        isGoogle = google;
    }

    private void checkOpenRouter(String checkUrl) {
        if (TextUtils.isEmpty(checkUrl)) return;
        isOpenRouter = checkUrl.contains("openrouter");
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request;
        if (isGoogle) {
            // Google
            request = chain.request()
                    .newBuilder()
                    .header("x-goog-api-key", token)
                    .build();
        } else if (!isOpenRouter) {
            // 默认
            request = chain.request()
                    .newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();
        } else {
            // OpenRouter
            request = chain.request()
                    .newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .header("HTTP-Referer", "https://www.ted.com/")
                    .header("X-Title", "ChatAir")
                    .build();
        }

        if (url != null) {
            Request.Builder builder = request.newBuilder();
            HttpUrl oldHttpUrl = request.url();

            HttpUrl newBaseUrl = HttpUrl.parse(url);
            if (newBaseUrl == null) {
                return chain.proceed(request);
            }

            StringBuilder segments = new StringBuilder();
            if (newBaseUrl.pathSegments().size() > 0) {

                for (String s:oldHttpUrl.pathSegments()
                     ) {
                    segments.append(s).append("/");
                }

            }

            HttpUrl newFullUrl;
            if (TextUtils.isEmpty(segments)) {
                newFullUrl = oldHttpUrl
                        .newBuilder()
                        .scheme(newBaseUrl.scheme())
                        .host(newBaseUrl.host())
                        .port(newBaseUrl.port())
                        .build();
            } else {

                String urlGet = "";
                if (oldHttpUrl.url().toString().contains("?")){
                    String[] url = oldHttpUrl.url().toString().split("\\?",2);
                    urlGet = "?" + url[1];   //url1 = "?id=111&time=222"
                }

                newFullUrl = newBaseUrl; //缺少接口字段

                ArrayList<String> oldPathSegments = new ArrayList<>();
                if (!TextUtils.isEmpty(oldUrl)) {
                    for (String s : Objects.requireNonNull(HttpUrl.parse(oldUrl)).pathSegments()) {
                        oldPathSegments.add(s);
                    }
                }

                for (int i = 0; i <= oldHttpUrl.pathSegments().size() - 1; i++) {
                    String s = oldHttpUrl.pathSegments().get(i);
                    //如果有之前的后缀则忽略
                    if (!TextUtils.isEmpty(s) && oldPathSegments.contains(s)) continue;
                    //循环add原先接口字段
                    newFullUrl = newFullUrl.newBuilder()
                            .addPathSegment(s)
                            .build();
                }

                if (!urlGet.isEmpty()){
                    newFullUrl = HttpUrl.parse(newFullUrl.url().toString() + urlGet);
                }
            }

            if (newFullUrl != null) {
                return chain.proceed(builder.url(newFullUrl).build());
            } else {
                return chain.proceed(request);
            }
        } else {
            return chain.proceed(request);

        }

    }
}
