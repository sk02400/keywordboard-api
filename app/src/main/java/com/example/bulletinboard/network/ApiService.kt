// ApiService.kt
package com.example.bulletinboard.network

import com.example.bulletinboard.model.Post
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
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

    @GET("bookmark/{userId}/{boardId}")
    suspend fun isBookmarked(
        @Path("userId") userId: String,
        @Path("boardId") boardId: String
    ): Response<Boolean>

    @POST("bookmark")
    suspend fun addBookmark(@Body bookmark: BookmarkRequest): Response<Unit>

    @DELETE("bookmark/{userId}/{boardId}")
    suspend fun removeBookmark(
        @Path("userId") userId: String,
        @Path("boardId") boardId: String
    ): Response<Unit>

}
