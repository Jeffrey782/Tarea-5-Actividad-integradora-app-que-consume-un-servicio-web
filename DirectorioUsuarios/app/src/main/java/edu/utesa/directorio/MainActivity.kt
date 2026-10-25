package edu.utesa.directorio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.utesa.directorio.data.User
import edu.utesa.directorio.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val model: DirectoryViewModel = viewModel()
            val state by model.state.collectAsStateWithLifecycle()
            DirectoryTheme {
                DirectoryScreen(state, model::refresh, model::selectTab, model::openUser,
                    model::closeDetail, model::toggleFavorite, model::retryPosts)
            }
        }
    }
}

@Composable
fun DirectoryTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(
        primary = Color(0xFF2457C5), onPrimary = Color.White,
        primaryContainer = Color(0xFFE4ECFF),
        background = Color(0xFFF5F7FB), surface = Color.White,
        onSurface = Color(0xFF172337), secondaryContainer = Color(0xFFE4ECFF)
    ), content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryScreen(
    state: UiState, refresh: () -> Unit, selectTab: (Boolean) -> Unit,
    openUser: (Int) -> Unit, back: () -> Unit, favorite: (Int) -> Unit,
    retryPosts: () -> Unit
) {
    val selected = state.users.firstOrNull { it.id == state.selectedId }
    BackHandler(enabled = state.selectedId != null, onBack = back)
    Scaffold(
        topBar = {
            TopAppBar(title = { Column {
                Text(if (state.selectedId != null) "Detalle del usuario" else "Directorio", fontWeight = FontWeight.Bold)
                Text("Jeffrey Fernandez · 1-21-1476", fontSize = 11.sp)
            } }, navigationIcon = {
                if (state.selectedId != null) IconButton(onClick = back) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                }
            }, actions = {
                if (state.selectedId == null) IconButton(onClick = refresh, enabled = !state.loading) {
                    Icon(Icons.Default.Refresh, "Actualizar directorio")
                }
            })
        }, bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = !state.favoritesOnly,
                    onClick = { selectTab(false) }, icon = { Icon(Icons.Default.People, null) },
                    label = { Text("Usuarios") })
                NavigationBarItem(selected = state.favoritesOnly,
                    onClick = { selectTab(true) }, icon = { Icon(Icons.Default.Star, null) },
                    label = { Text("Favoritos (${state.favorites.size})") })
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!state.online) Surface(color = Color(0xFFFFEBC2), modifier = Modifier.fillMaxWidth()) {
                Text("Sin Internet · Mostrando datos guardados", Modifier.padding(12.dp), color = Color(0xFF654100))
            }
            if (state.selectedId != null) {
                if (selected == null) EmptyPanel("Usuario no disponible", "Vuelve al directorio para cargar sus datos.")
                else Detail(selected, state, favorite, retryPosts)
            } else {
                val shown = state.users.filter { !state.favoritesOnly || it.id in state.favorites }
                if (state.loading && !state.favoritesOnly) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Cargando usuarios…", Modifier.padding(16.dp))
                }
                state.error?.let { ErrorPanel(it, refresh) }
                if (shown.isEmpty() && (!state.loading || state.favoritesOnly)) {
                    EmptyPanel(
                        if (state.favoritesOnly) "Todavía no tienes favoritos" else if (!state.online) "No hay usuarios guardados" else "No hay usuarios",
                        if (state.favoritesOnly) "Toca la estrella de un usuario para guardarlo en este dispositivo."
                        else if (!state.online) "Conéctate a Internet y pulsa actualizar. Después podrás consultar tus favoritos sin conexión."
                        else "Pulsa actualizar para volver a consultar el directorio."
                    )
                }
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (shown.isNotEmpty()) item {
                        Text(if (state.favoritesOnly) "TUS CONTACTOS GUARDADOS" else "${shown.size} USUARIOS · JSONPLACEHOLDER",
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF52627A))
                    }
                    items(shown, key = { it.id }) { user ->
                        UserCard(user, user.id in state.favorites, { openUser(user.id) }, { favorite(user.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun UserCard(user: User, isFavorite: Boolean, open: () -> Unit, favorite: () -> Unit) {
    Card(onClick = open, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(user.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(user.email, fontSize = 14.sp, color = Color(0xFF435674))
                Spacer(Modifier.height(8.dp))
                Text(user.company.name, fontSize = 13.sp, color = Color(0xFF52627A))
                Text("Ver perfil y publicaciones", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = favorite, modifier = Modifier.size(48.dp)) {
                Icon(if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    if (isFavorite) "Quitar a ${user.name} de favoritos" else "Guardar a ${user.name} en favoritos",
                    tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun Detail(user: User, state: UiState, favorite: (Int) -> Unit, retry: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text(user.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("@${user.username}", color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text("Correo: ${user.email}")
                    Text("Teléfono: ${user.phone}")
                    Text("Web: ${user.website}")
                    Text("Empresa: ${user.company.name}")
                    Text("${user.company.catchPhrase}\nActividad: ${user.company.bs}", fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Dirección: ${user.address.street}, ${user.address.suite}\n${user.address.city} · ${user.address.zipcode}")
                    Text("Coordenadas: ${user.address.geo.lat}, ${user.address.geo.lng}", fontSize = 12.sp)
                    FilledTonalButton(onClick = { favorite(user.id) }) {
                        Icon(Icons.Default.Star, null); Spacer(Modifier.width(8.dp))
                        Text(if (user.id in state.favorites) "Quitar de favoritos" else "Guardar en favoritos")
                    }
                }
            }
        }
        item { Text("Publicaciones", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        if (state.postsCached) item { Text("Copia guardada en este dispositivo", fontSize = 12.sp) }
        if (state.postsLoading) item {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Cargando publicaciones…")
        }
        state.postsError?.let { error -> item { ErrorPanel(error, retry) } }
        if (!state.postsLoading && state.postsError == null && state.posts.isEmpty()) item {
            Text("Este usuario no tiene publicaciones.")
        }
        items(state.posts, key = { it.id }) { post ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    Text(post.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp)); Text(post.body, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun ErrorPanel(message: String, retry: () -> Unit) {
    Surface(color = Color(0xFFFFE9E5), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(message, color = Color(0xFF8C2219))
            TextButton(onClick = retry) { Text("Reintentar") }
        }
    }
}
@Composable
fun EmptyPanel(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp)); Text(body)
    }
}
