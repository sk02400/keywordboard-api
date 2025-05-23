package com.example.bulletinboard.network

import com.example.bulletinboard.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

import com.example.bulletinboard.network.RegisterAfterGoogleRequest

interface ApiService {
    @POST("login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("boards")
    suspend fun getOrCreateBoard(@Body request: BoardNameRequest): BoardIdResponse

    @GET("posts/{boardId}")
    suspend fun getPosts(@Path("boardId") boardId: String): Response<List<Post>>

    @POST("boards/{boardId}/posts")
    suspend fun createPost(
        @Path("boardId") boardId: String,
        @Body post: Post
    ): Response<Unit>

    @GET("bookmark/status/{userId}/{boardId}")
    suspend fun getBookmarkStatus(
        @Path("userId") userId: String,
        @Path("boardId") boardId: String
    ): Response<BookmarkStatusResponse>

    @POST("bookmark/{userId}/{boardId}")
    suspend fun bookmarkBoard(
        @Path("userId") userId: String,
        @Path("boardId") boardId: String
    ): Response<Unit>

    @POST("unbookmark/{userId}/{boardId}")
    suspend fun unbookmarkBoard(
        @Path("userId") userId: String,
        @Path("boardId") boardId: String
    ): Response<Unit>

    @GET("bookmarks/{userId}")
    suspend fun getBookmarks(@Path("userId") userId: String): List<Bookmark>

    @GET("chats/{userId}")
    suspend fun getChatRooms(@Path("userId") userId: String): List<ChatRoom>

    @GET("users/{userId}/exists")
    suspend fun checkUserExists(@Path("userId") userId: String): ExistsResponse

    @POST("chats")
    suspend fun createChat(@Body body: CreateChatRequest): ChatRoom

    @GET("messages/{roomId}")
    suspend fun getMessages(@Path("roomId") roomId: String): List<Message>

    @POST("messages")
    suspend fun sendMessage(@Body request: SendMessageRequest): Response<Unit>

    @POST("auth/google")
    suspend fun googleLogin(
        @Body request: GoogleLoginRequest
    ): Response<LoginResponse>

    @POST("/auth/register_after_google")
    suspend fun registerAfterGoogle(@Body request: RegisterAfterGoogleRequest): Response<RegisterAfterGoogleResponse>

    data class CheckUserResponse(val exists: Boolean, val userId: String?, val name: String?)
    data class RegisterResponse(val success: Boolean, val message: String?)

    @GET("user/exists")
    suspend fun checkUserExistsByEmail(@Query("email") email: String): CheckUserResponse

    @POST("user/register")
    suspend fun registerUser(@Body req: RegisterRequest): RegisterResponse

    @POST("/auth/google-register")
    suspend fun registerAfterGoogleLogin(@Body request: RegisterRequest): Response<BasicResponse>
    // === データクラス ===

    data class ExistsResponse(val exists: Boolean)

    data class CreateChatRequest(
        val user1_id: String,
        val user2_id: String
    )

    data class SendMessageRequest(
        val room_id: String,
        val sender_id: String,
        val sender_name: String,
        val content: String
    )

}
