package com.oigit.admin.core.http;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

public final class OutboundHttpRequests {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private OutboundHttpRequests() {
    }

    public static Request jsonPost(String url, byte[] body) {
        return new Request.Builder()
                .url(url)
                .header("Accept-Encoding", "identity")
                .header("Accept", "application/json")
                .post(RequestBody.create(body, JSON))
                .build();
    }

    public static Request authenticatedJsonPost(String url, byte[] body, String bearerToken) {
        return jsonPost(url, body).newBuilder()
                .header("Authorization", "Bearer " + bearerToken)
                .build();
    }

    public static Request get(String url) {
        return new Request.Builder()
                .url(url)
                .header("Accept-Encoding", "identity")
                .header("Accept", "application/json")
                .get()
                .build();
    }
}
