package com.bignerdranch.android.advancedphotogallery.api

import okhttp3.*

private const val TAG = "FlickrInterceptor"
private const val API_KEY = "77157672e601a72154b6c34f8f577d4c"

class FlickrInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest: Request = chain.request()

        val newUrl: HttpUrl =
            originalRequest.url.newBuilder()            //add further params with '.' operator
                    .addQueryParameter("api_key", API_KEY)
                    .addQueryParameter("format", "json")
                    .addQueryParameter("nojsoncallback", "1")
                    .addQueryParameter("extras", "url_s")
                    .build()

        val newRequest: Request = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}