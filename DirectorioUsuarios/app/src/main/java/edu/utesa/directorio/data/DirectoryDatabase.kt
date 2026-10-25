package edu.utesa.directorio.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users")
data class UserRow(@PrimaryKey val id: Int, val json: String)
@Entity(tableName = "favorites")
data class FavoriteRow(@PrimaryKey val userId: Int)
@Entity(tableName = "post_cache")
data class PostCacheRow(@PrimaryKey val userId: Int, val json: String)

@Dao
interface DirectoryDao {
    @Query("SELECT * FROM users ORDER BY id")
    fun observeUsers(): Flow<List<UserRow>>
    @Query("SELECT userId FROM favorites")
    fun observeFavorites(): Flow<List<Int>>
    @Query("SELECT * FROM post_cache WHERE userId = :id")
    suspend fun cachedPosts(id: Int): PostCacheRow?
    @Upsert suspend fun saveUsers(users: List<UserRow>)
    @Upsert suspend fun savePosts(posts: PostCacheRow)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavorite(favorite: FavoriteRow)
    @Query("DELETE FROM favorites WHERE userId = :id")
    suspend fun removeFavorite(id: Int)
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :id)")
    suspend fun isFavorite(id: Int): Boolean

    @Transaction
    suspend fun toggleFavorite(id: Int) {
        if (isFavorite(id)) removeFavorite(id) else addFavorite(FavoriteRow(id))
    }
}

@Database(entities = [UserRow::class, FavoriteRow::class, PostCacheRow::class],
    version = 1, exportSchema = false)
abstract class DirectoryDatabase : RoomDatabase() {
    abstract fun dao(): DirectoryDao
}
