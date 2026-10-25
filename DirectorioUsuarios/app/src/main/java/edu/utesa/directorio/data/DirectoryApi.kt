package edu.utesa.directorio.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface DirectoryApi {
    @GET("users")
    suspend fun users(): List<User>

    @GET("posts")
    suspend fun posts(@Query("userId") userId: Int): List<Post>

    companion object {
        fun create(baseUrl: String = "https://jsonplaceholder.typicode.com/"): DirectoryApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .build()
            return Retrofit.Builder().baseUrl(baseUrl).client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(DirectoryApi::class.java)
        }
    }
}
