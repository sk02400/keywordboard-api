// ApiService.kt
package com.example.bulletinboard.network

import com.example.bulletinboard.model.Post
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @GET("posts/{boardId}")
    suspend fun getPosts(@Path("boardId") boardId: String): Response<List<Post>>

    @POST("boards/{boardId}/posts")
    suspend fun createPost(@Path("boardId") boardId: String, @Body post: Post): Response<Unit>
}
