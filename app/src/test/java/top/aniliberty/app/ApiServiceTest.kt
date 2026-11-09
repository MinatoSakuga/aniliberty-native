package top.aniliberty.app

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import top.aniliberty.app.data.*

class ApiServiceTest {
    private lateinit var server: MockWebServer
    private lateinit var api: AniLibertyApi

    @Before fun setup() {
        server = MockWebServer(); server.start()
        api = Network.create(java.io.File(System.getProperty("java.io.tmpdir"), "aniliberty-test-cache"), server.url("/api/v1/").toString())
    }
    @After fun teardown() { server.shutdown() }

    @Test fun favoritesRequestUsesCreatedAtDescending() = runTest {
        server.enqueue(MockResponse().setBody("""{"data":[{"id":30},{"id":20},{"id":10}],"meta":{"pagination":{"total":3,"total_pages":1,"page":1}}}"""))
        val page = api.favoriteReleases("Bearer test-token")
        assertEquals(listOf(30, 20, 10), page.data.map { it.id })
        val request = server.takeRequest()
        assertEquals("Bearer test-token", request.getHeader("Authorization"))
        assertTrue(request.path!!.contains("f%5Bsorting%5D=CREATED_AT_DESC") || request.path!!.contains("f[sorting]=CREATED_AT_DESC"))
        assertTrue(request.path!!.contains("accounts/users/me/favorites/releases"))
    }

    @Test fun loginSendsCredentialsAndParsesToken() = runTest {
        server.enqueue(MockResponse().setBody("""{"token":"secret-token"}"""))
        val result = api.login(LoginRequest("user@example.com", "password"))
        assertEquals("secret-token", result.token)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.body.readUtf8().contains("user@example.com"))
    }

    @Test fun scheduleWrapperParses() = runTest {
        server.enqueue(MockResponse().setBody("""{"data":[{"release":{"id":1,"name":{"main":"Test"}},"full_season_is_released":false,"next_release_episode_number":5}]}"""))
        val result = api.schedule()
        assertEquals(1, result.data.size)
        assertEquals(5, result.data[0].next)
    }

    @Test fun catalogQueryIncludesAdultFilterOnlyWhenRequested() = runTest {
        server.enqueue(MockResponse().setBody("""{"data":[],"meta":{"pagination":{"total":0,"total_pages":1,"page":1}}}"""))
        api.catalog(CatalogFilter().query(1, hideAdult = true))
        assertTrue(server.takeRequest().path!!.contains("age_ratings"))
        server.enqueue(MockResponse().setBody("""{"data":[],"meta":{"pagination":{"total":0,"total_pages":1,"page":1}}}"""))
        api.catalog(CatalogFilter().query(1, hideAdult = false))
        assertFalse(server.takeRequest().path!!.contains("age_ratings"))
    }
}
