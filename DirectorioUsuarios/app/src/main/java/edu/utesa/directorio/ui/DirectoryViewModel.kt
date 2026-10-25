package edu.utesa.directorio.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import edu.utesa.directorio.DirectoryApp
import edu.utesa.directorio.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import retrofit2.HttpException
import java.io.IOException

data class UiState(
    val users: List<User> = emptyList(), val favorites: Set<Int> = emptySet(),
    val loading: Boolean = true, val online: Boolean = true,
    val error: String? = null, val selectedId: Int? = null,
    val favoritesOnly: Boolean = false, val posts: List<Post> = emptyList(),
    val postsLoading: Boolean = false, val postsError: String? = null,
    val postsCached: Boolean = false
)

class DirectoryViewModel(app: Application, private val saved: SavedStateHandle) : AndroidViewModel(app) {
    private val services = app as DirectoryApp
    private val repo = services.repository
    private val mutable = MutableStateFlow(UiState(
        online = services.connectivity.isOnline(),
        selectedId = saved["selectedId"], favoritesOnly = saved["favoritesOnly"] ?: false
    ))
    val state = mutable.asStateFlow()
    private var listJob: Job? = null
    private var detailJob: Job? = null

    init {
        viewModelScope.launch {
            repo.users.catch { mutable.update { s -> s.copy(error = "No se pudo leer la copia local.") } }
                .collect { users -> mutable.update { it.copy(users = users) } }
        }
        viewModelScope.launch {
            repo.favorites.catch { mutable.update { s -> s.copy(error = "No se pudieron leer los favoritos.") } }
                .collect { ids -> mutable.update { it.copy(favorites = ids) } }
        }
        viewModelScope.launch {
            services.connectivity.online.collect { online ->
                val reconnected = !mutable.value.online && online
                mutable.update { it.copy(online = online) }
                if (reconnected) {
                    refresh()
                    mutable.value.selectedId?.let { loadPosts(it) }
                }
            }
        }
        refresh()
        mutable.value.selectedId?.let { loadPosts(it) }
    }
    fun refresh() {
        if (listJob?.isActive == true) return
        listJob = viewModelScope.launch {
            if (!services.connectivity.isOnline()) {
                mutable.update { it.copy(loading = false, online = false, error = null) }
                return@launch
            }
            mutable.update { it.copy(loading = true, error = null) }
            try { repo.refreshUsers() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(error = message(e)) } }
            finally { mutable.update { it.copy(loading = false) } }
        }
    }
    fun selectTab(favorites: Boolean) {
        saved["favoritesOnly"] = favorites
        mutable.update { it.copy(favoritesOnly = favorites) }
        closeDetail()
    }
    fun openUser(id: Int) {
        saved["selectedId"] = id
        mutable.update { it.copy(selectedId = id, posts = emptyList(), postsError = null) }
        loadPosts(id)
    }
    fun closeDetail() {
        detailJob?.cancel()
        saved.set<Int?>("selectedId", null)
        mutable.update { it.copy(selectedId = null, postsLoading = false) }
    }
    fun retryPosts() { mutable.value.selectedId?.let { loadPosts(it) } }
    private fun loadPosts(id: Int) {
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            mutable.update { it.copy(postsLoading = true, postsError = null, postsCached = false) }
            try {
                val cached = repo.cachedPosts(id)
                mutable.update { it.copy(posts = cached.orEmpty(), postsCached = cached != null) }
                if (!services.connectivity.isOnline()) {
                    mutable.update { it.copy(postsError = if (cached == null)
                        "Sin Internet. Las publicaciones aún no están guardadas." else null) }
                    return@launch
                }
                val posts = repo.refreshPosts(id)
                mutable.update { it.copy(posts = posts, postsCached = false) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(postsError = message(e)) } }
            finally {
                if (currentCoroutineContext().isActive && mutable.value.selectedId == id) mutable.update { it.copy(postsLoading = false) }
            }
        }
    }
    fun toggleFavorite(id: Int) {
        viewModelScope.launch {
            try { repo.toggleFavorite(id) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(error = "No se pudo guardar el favorito. Intenta de nuevo.") } }
        }
    }
    private fun message(e: Exception): String = when {
        !services.connectivity.isOnline() -> "Sin Internet. Puedes consultar tus datos guardados."
        e is HttpException -> "El servidor respondió con error HTTP ${e.code()}. Intenta de nuevo."
        e is IOException -> "No se pudo conectar con el servidor. Comprueba la conexión y reintenta."
        else -> "No se pudieron procesar los datos. Intenta de nuevo."
    }
}
