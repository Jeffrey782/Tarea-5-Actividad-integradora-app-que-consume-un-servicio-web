package edu.utesa.directorio

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import edu.utesa.directorio.data.*
import edu.utesa.directorio.ui.UiState
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Capturas reales del componente Compose con estados controlados, sin alterar la app. */
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class RenderEvidenceTest {
    @get:Rule val rule = createComposeRule()
    @Test fun captureRequiredStates() {
        val user = User(1, "Leanne Graham", "Bret", "Sincere@april.biz",
            "1-770-736-8031 x56442", "hildegard.org",
            Address("Kulas Light", "Apt. 556", "Gwenborough", "92998-3874", Geo("-37.3159", "81.1496")),
            Company("Romaguera-Crona", "Multi-layered client-server neural-net", "harness real-time e-markets"))
        val second = User(2, "Ervin Howell", "Antonette", "Shanna@melissa.tv",
            company = Company("Deckow-Crist"))
        val ui = mutableStateOf(UiState(loading = true))
        rule.setContent {
            DirectoryTheme {
                DirectoryScreen(ui.value, {},
                    { ui.value = ui.value.copy(favoritesOnly = it, selectedId = null) },
                    { ui.value = ui.value.copy(selectedId = it) },
                    { ui.value = ui.value.copy(selectedId = null) },
                    { id -> ui.value = ui.value.copy(favorites = if (id in ui.value.favorites)
                        ui.value.favorites - id else ui.value.favorites + id) }, {})
            }
        }
        fun capture(name: String) {
            rule.waitForIdle()
            val dir = File("build/evidence")
            dir.mkdirs()
            File(dir, "$name.png").outputStream().use {
                rule.runOnIdle {
                    val activity = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
                        .getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                        .first() as android.app.Activity
                    val view = activity.window.decorView
                    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                    view.draw(android.graphics.Canvas(bitmap))
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
        rule.onNodeWithText("Cargando usuarios…").assertIsDisplayed()
        capture("01_carga")
        rule.runOnIdle { ui.value = UiState(users = listOf(user, second), loading = false) }
        rule.onNodeWithText("Leanne Graham").assertIsDisplayed()
        capture("02_exito")
        rule.onNodeWithContentDescription("Guardar a Leanne Graham en favoritos").performClick()
        rule.onNodeWithText("Favoritos (1)").performClick()
        rule.runOnIdle { ui.value = ui.value.copy(online = false) }
        rule.onNodeWithText("Sin Internet · Mostrando datos guardados").assertIsDisplayed()
        capture("05_favoritos")
        rule.onNodeWithText("Leanne Graham").performClick()
        rule.runOnIdle { ui.value = ui.value.copy(online = true, posts = listOf(
            Post(1, 1, "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
                "quia et suscipit\nsuscipit recusandae consequuntur expedita et cum"))) }
        rule.onNodeWithText("Publicaciones").assertIsDisplayed()
        capture("04_detalle")
        rule.runOnIdle { ui.value = UiState(loading = false, online = false) }
        rule.onNodeWithText("No hay usuarios guardados").assertIsDisplayed()
        capture("03_sin_conexion")
        rule.runOnIdle { ui.value = UiState(loading = false, error = "El servidor respondió con error HTTP 500. Intenta de nuevo.") }
        rule.onNodeWithText("Reintentar").assertIsDisplayed()
        capture("06_error")
    }
}
