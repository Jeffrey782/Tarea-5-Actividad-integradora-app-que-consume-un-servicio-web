package edu.utesa.directorio

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import edu.utesa.directorio.data.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class LocalPersistenceTest {
    @Test fun favoritesAndPostsSurviveDatabaseReopen() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "persistence-test.db"
        ctx.deleteDatabase(name)
        var db = Room.databaseBuilder(ctx, DirectoryDatabase::class.java, name).build()
        try {
            db.dao().saveUsers(listOf(UserRow(1, "{\"id\":1}")))
            db.dao().toggleFavorite(1)
            db.dao().savePosts(PostCacheRow(1, "[]"))
            db.close()
            db = Room.databaseBuilder(ctx, DirectoryDatabase::class.java, name).build()
            assertEquals(listOf(1), db.dao().observeFavorites().first())
            assertEquals("[]", db.dao().cachedPosts(1)?.json)
            db.dao().saveUsers(listOf(UserRow(1, "{\"id\":1,\"name\":\"Updated\"}")))
            assertTrue(db.dao().isFavorite(1))
            db.dao().toggleFavorite(1)
            assertFalse(db.dao().isFavorite(1))
        } finally { db.close(); ctx.deleteDatabase(name) }
    }
}
