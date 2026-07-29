package br.com.fenix.bilingualreader.service.tracker.mal

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MyAnimeListModelsTest {

    private val gson = Gson()

    @Test
    fun `OAuth JSON deserialization should parse token response`() {
        val json = """
            {
                "token_type": "Bearer",
                "expires_in": 3600,
                "access_token": "mock_access_token",
                "refresh_token": "mock_refresh_token"
            }
        """.trimIndent()

        val oauth = gson.fromJson(json, OAuth::class.java)
        assertNotNull(oauth)
        assertEquals("Bearer", oauth.token_type)
        assertEquals(3600L, oauth.expires_in)
        assertEquals("mock_access_token", oauth.access_token)
        assertEquals("mock_refresh_token", oauth.refresh_token)
    }

    @Test
    fun `User JSON deserialization should parse user profile`() {
        val json = """
            {
                "id": 12345,
                "name": "TestUser",
                "picture": "http://example.com/pic.jpg",
                "joined_at": "2020-01-01T00:00:00Z"
            }
        """.trimIndent()

        val user = gson.fromJson(json, User::class.java)
        assertNotNull(user)
        assertEquals(12345, user.id)
        assertEquals("TestUser", user.name)
        assertEquals("http://example.com/pic.jpg", user.picture)
        assertEquals("2020-01-01T00:00:00Z", user.joinedAt)
    }

    @Test
    fun `MyAnimeList enums should have expected serialized values`() {
        assertEquals(STATUS.FINISHED, gson.fromJson("\"finished\"", STATUS::class.java))
        assertEquals(MEDIA.MANGA, gson.fromJson("\"manga\"", MEDIA::class.java))
        assertEquals(RANKING.ALL, gson.fromJson("\"all\"", RANKING::class.java))
        assertEquals(NSFW.WHITE, gson.fromJson("\"white\"", NSFW::class.java))
    }
}
