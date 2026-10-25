package edu.utesa.directorio.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.map

class DirectoryRepository(private val api: DirectoryApi, private val dao: DirectoryDao) {
    private val gson = Gson()
    val users = dao.observeUsers().map { rows ->
        rows.map { gson.fromJson(it.json, User::class.java) }
    }
    val favorites = dao.observeFavorites().map { it.toSet() }

    suspend fun refreshUsers() {
        val users = api.users()
        require(users.all { it.id > 0 && !it.name.isNullOrBlank() &&
            !it.email.isNullOrBlank() && it.company != null }) { "Datos inválidos" }
        // No se reemplaza la tabla de favoritos al actualizar el directorio.
        dao.saveUsers(users.map { UserRow(it.id, gson.toJson(it)) })
    }
    suspend fun toggleFavorite(id: Int) = dao.toggleFavorite(id)
    suspend fun cachedPosts(id: Int): List<Post>? = dao.cachedPosts(id)?.let {
        gson.fromJson(it.json, object : TypeToken<List<Post>>() {}.type)
    }
    suspend fun refreshPosts(id: Int): List<Post> {
        val posts = api.posts(id)
        require(posts.all { it.userId == id && !it.title.isNullOrBlank() && it.body != null })
        dao.savePosts(PostCacheRow(id, gson.toJson(posts)))
        return posts
    }
}
