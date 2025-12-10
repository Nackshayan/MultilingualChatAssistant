package com.example.multilingualchatassistant.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GiphyResponse {

    @SerializedName("data")
    public List<GifObject> data;

    public static class GifObject {
        @SerializedName("images")
        public Images images;
    }

    public static class Images {
        @SerializedName("downsized_medium")
        public GifImage downsizedMedium;

        @SerializedName("original")
        public GifImage original;
    }

    public static class GifImage {
        @SerializedName("url")
        public String url;
    }
}
