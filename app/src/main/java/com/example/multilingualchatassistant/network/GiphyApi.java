package com.example.multilingualchatassistant.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GiphyApi {

    @GET("gifs/search")
    Call<GiphyResponse> searchGifs(
            @Query("api_key") String apiKey,
            @Query("q") String query,
            @Query("limit") int limit,
            @Query("rating") String rating
    );
}
