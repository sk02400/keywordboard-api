// ApiService.kt
package com.example.bulletinboard.network

import com.example.bulletinboard.model.Post
import com.example.bulletinboard.model.BookmarkStatusResponse
import com.example.bulletinboard.model.Bookmark
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

    // 🆕 ブックマーク状態を取得
    @GET("bookmark/status/{userId}/{boardId}")
    suspend fun getBookmarkStatus(
        @Path("userId") userId: String,
        @Path("boardId") boardId: String
    ): Response<BookmarkStatusResponse>

    // 🆕 ブックマーク登録
    @POST("bookmark/{userId}/{boardId}")
    suspend fun bookmarkBoard(
        @Path("userId") userId: String,
        @Path("boardId") boardId: String
    ): Response<Unit>

    // 🆕 ブックマーク解除
    @POST("unbookmark/{userId}/{boardId}")
    suspend fun unbookmarkBoard(
        @Path("userId") userId: String,
        @Path("boardId") boardId: String
    ): Response<Unit>

    @GET("bookmarks/{userId}")
    suspend fun getBookmarks(@Path("userId") userId: String): List<Bookmark>
}
