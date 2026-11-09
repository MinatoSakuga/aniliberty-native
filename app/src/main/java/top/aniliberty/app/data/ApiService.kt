package top.aniliberty.app.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.*
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

interface AniLibertyApi {
    @GET("anime/releases/latest") suspend fun latest(@Query("limit") limit: Int = 40): List<Release>
    @GET("anime/catalog/releases") suspend fun catalog(@QueryMap query: Map<String, String>): CatalogPage
    @GET("app/search/releases") suspend fun search(@Query("query") query: String): List<Release>
    @GET("anime/releases/{ref}") suspend fun release(@Path("ref") ref: String, @Query("exclude") exclude: String = "torrents,sponsors,background_covers"): Release
    @GET("anime/releases/list") suspend fun releases(@Query("ids") ids: String, @Query("limit") limit: Int = 50): CatalogPage
    @GET("anime/releases/recommended") suspend fun recommended(): List<Release>
    @GET("anime/genres") suspend fun genres(): List<Genre>
    @GET("anime/schedule/week") suspend fun schedule(): ScheduleResponse
    @GET("anime/releases/episodes/{id}") suspend fun episode(@Path("id") id: String, @Header("Authorization") authorization: String): Episode
    @POST("accounts/users/auth/login") suspend fun login(@Body body: LoginRequest): TokenResponse
    @POST("accounts/users/auth/logout") suspend fun logout(@Header("Authorization") authorization: String, @Body body: EmptyRequest = EmptyRequest()): okhttp3.ResponseBody
    @GET("accounts/users/me/profile") suspend fun profile(@Header("Authorization") authorization: String, @Query("include") include: String = "id,login,nickname,avatar"): Profile
    @POST("accounts/otp/get") suspend fun otpStart(@Body body: OtpStartRequest): OtpStartResponse
    @POST("accounts/otp/login") suspend fun otpLogin(@Body body: OtpLoginRequest): TokenResponse
    @GET("accounts/users/auth/social/{provider}/login") suspend fun socialLogin(@Path("provider") provider: String = "vk"): SocialResponse
    @GET("accounts/users/auth/social/authenticate") suspend fun socialAuthenticate(@Query("state") state: String): TokenResponse
    @GET("accounts/users/me/favorites/releases")
    suspend fun favoriteReleases(@Header("Authorization") auth: String, @Query("page") page: Int = 1, @Query("limit") limit: Int = 100, @Query("f[sorting]") sorting: String = "CREATED_AT_DESC", @Query("exclude") exclude: String = "torrents,members,sponsors,background_covers"): CatalogPage
    @GET("accounts/users/me/favorites/ids") suspend fun favoriteIds(@Header("Authorization") authorization: String): List<Int>
    @POST("accounts/users/me/favorites") suspend fun addFavorite(@Header("Authorization") authorization: String, @Body body: List<ReleaseId>): List<Int>
    @HTTP(method = "DELETE", path = "accounts/users/me/favorites", hasBody = true) suspend fun removeFavorite(@Header("Authorization") authorization: String, @Body body: List<ReleaseId>): List<Int>
    @GET("accounts/users/me/collections/ids") suspend fun collectionIds(@Header("Authorization") authorization: String): JsonArray
    @POST("accounts/users/me/collections") suspend fun addCollection(@Header("Authorization") authorization: String, @Body body: List<CollectionChange>): JsonArray
    @HTTP(method = "DELETE", path = "accounts/users/me/collections", hasBody = true) suspend fun removeCollection(@Header("Authorization") authorization: String, @Body body: List<ReleaseId>): JsonArray
    @GET("accounts/users/me/views/history") suspend fun history(@Header("Authorization") authorization: String, @Query("page") page: Int = 1, @Query("limit") limit: Int = 100): HistoryPage
    @POST("accounts/users/me/views/timecodes") suspend fun saveTimecodes(@Header("Authorization") authorization: String, @Body body: List<TimecodeRequest>): okhttp3.ResponseBody
}

object Network {
    const val BASE = "https://aniliberty.top/api/v1/"
    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false; isLenient = true }
    fun create(cacheDir: File, base: String = BASE): AniLibertyApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS)
            .cache(Cache(File(cacheDir, "http"), 20L * 1024 * 1024))
            .addInterceptor { chain -> chain.proceed(chain.request().newBuilder().header("Accept", "application/json").header("User-Agent", "AniLibertyNative/1.8.5").build()) }
            .build()
        return Retrofit.Builder().baseUrl(base).client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(AniLibertyApi::class.java)
    }
}
