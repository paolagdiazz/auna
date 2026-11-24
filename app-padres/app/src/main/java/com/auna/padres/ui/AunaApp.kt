package com.auna.padres.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.auna.padres.AunaViewModel
import com.auna.padres.ui.screens.AccesosScreen
import com.auna.padres.ui.screens.AjustesScreen
import com.auna.padres.ui.screens.HablarScreen
import com.auna.padres.ui.screens.InicioScreen
import com.auna.padres.ui.screens.PasaporteScreen
import com.auna.padres.ui.screens.PerfilesScreen
import com.auna.padres.ui.screens.ProbarScreen
import com.auna.padres.ui.screens.PropuestasScreen
import com.auna.padres.ui.screens.QrScreen
import com.auna.padres.ui.theme.Auna

object Routes {
    const val INICIO = "inicio"
    const val PASAPORTE = "pasaporte"
    const val HABLAR = "hablar"
    const val ACCESOS = "accesos"
    const val PERFILES = "perfiles"
    const val PROBAR = "probar"
    const val PROPUESTAS = "propuestas"
    const val QR = "qr"
    const val AJUSTES = "ajustes"
}

private data class Tab(val route: String, val label: String, val icon: AunaIcon)

private val tabs = listOf(
    Tab(Routes.INICIO, "Inicio", Icons.Home),
    Tab(Routes.PASAPORTE, "Pasaporte", Icons.Passport),
    Tab(Routes.HABLAR, "Hablar", Icons.Mic),
    Tab(Routes.ACCESOS, "Accesos", Icons.People),
    Tab(Routes.PERFILES, "Mis perfiles", Icons.Profile),
)

/** Navegación entre pantallas: pestañas de la barra inferior y pantallas de detalle. */
class Navi(private val nav: NavController) {
    fun tab(route: String) = nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    fun open(route: String) = nav.navigate(route) { launchSingleTop = true }

    fun back() {
        if (!nav.popBackStack()) tab(Routes.INICIO)
    }
}

@Composable
fun AunaApp(vm: AunaViewModel) {
    val nav = rememberNavController()
    val navi = remember(nav) { Navi(nav) }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    Box(Modifier.fillMaxSize().background(Auna.Bg).statusBarsPadding()) {
        NavHost(
            nav,
            startDestination = Routes.INICIO,
            modifier = Modifier.fillMaxSize(),
            // Pestañas: cambio instantáneo. Pantallas de detalle: entran deslizando desde la derecha.
            enterTransition = {
                if (isTabSwitch(initialState.destination.route, targetState.destination.route)) EnterTransition.None
                else slideInHorizontally(tween(NAV_MS, easing = FastOutSlowInEasing)) { it }
            },
            exitTransition = {
                if (isTabSwitch(initialState.destination.route, targetState.destination.route)) ExitTransition.None
                else slideOutHorizontally(tween(NAV_MS, easing = FastOutSlowInEasing)) { -it / 4 }
            },
            popEnterTransition = {
                if (isTabSwitch(initialState.destination.route, targetState.destination.route)) EnterTransition.None
                else slideInHorizontally(tween(NAV_MS, easing = FastOutSlowInEasing)) { -it / 4 }
            },
            popExitTransition = {
                if (isTabSwitch(initialState.destination.route, targetState.destination.route)) ExitTransition.None
                else slideOutHorizontally(tween(NAV_MS, easing = FastOutSlowInEasing)) { it }
            },
        ) {
            composable(Routes.INICIO) { TabFrame(Routes.INICIO, navi) { InicioScreen(vm, navi) } }
            composable(Routes.PASAPORTE) { TabFrame(Routes.PASAPORTE, navi) { PasaporteScreen(vm, navi) } }
            composable(Routes.HABLAR) { TabFrame(Routes.HABLAR, navi) { HablarScreen(vm, navi) } }
            composable(Routes.ACCESOS) { TabFrame(Routes.ACCESOS, navi) { AccesosScreen(vm, navi) } }
            composable(Routes.PERFILES) { TabFrame(Routes.PERFILES, navi) { PerfilesScreen(vm, navi) } }
            composable(Routes.PROBAR) { DetailFrame { ProbarScreen(vm, navi) } }
            composable(Routes.PROPUESTAS) { DetailFrame { PropuestasScreen(vm, navi) } }
            composable(Routes.QR) { DetailFrame { QrScreen(vm, navi) } }
            composable(Routes.AJUSTES) { DetailFrame { AjustesScreen(vm, navi) } }
        }
    }
}

private const val NAV_MS = 300

private fun isTabSwitch(from: String?, to: String?) = tabs.any { it.route == from } && tabs.any { it.route == to }

/** Pantalla de pestaña: contenido + barra inferior, con fondo opaco para que el deslizamiento se vea limpio. */
@Composable
private fun TabFrame(route: String, navi: Navi, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(Auna.Bg)) {
        Box(Modifier.weight(1f).fillMaxWidth().imePadding()) { content() }
        BottomNav(route) { navi.tab(it) }
    }
}

/** Pantalla de detalle: ocupa toda la pantalla (tapa la barra inferior al entrar). */
@Composable
private fun DetailFrame(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Auna.Bg).navigationBarsPadding().imePadding()) { content() }
}

@Composable
private fun BottomNav(current: String?, onTab: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Auna.White)
            .drawBehind { drawLine(Auna.Line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .navigationBarsPadding()
            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        tabs.forEach { t ->
            val selected = t.route == current
            val isTalk = t.route == Routes.HABLAR
            Column(
                Modifier
                    .weight(1f)
                    .clickable(role = Role.Tab) { onTab(t.route) }
                    .semantics { this.selected = selected },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (isTalk) {
                    // Botón central elevado (sobresale 30 dp por encima de la barra).
                    Box(Modifier.width(60.dp).height(30.dp)) {
                        Box(
                            Modifier
                                .requiredSize(60.dp)
                                .offset(y = (-15).dp)
                                .clip(CircleShape)
                                .background(Auna.Leaf)
                                .border(4.dp, Auna.White, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { AIcon(Icons.Mic, Auna.Green, 26.dp, 2.2f) }
                    }
                    Txt(t.label, 12, 900, if (selected) Auna.Green else Auna.Ink)
                } else {
                    Box(
                        Modifier
                            .size(52.dp, 32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) Auna.Mint else Auna.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        AIcon(t.icon, if (selected) Auna.Green else Auna.Muted, 22.dp, if (selected) 2.4f else 2f)
                    }
                    Txt(t.label, 12, 700, if (selected) Auna.Green else Auna.Muted)
                }
            }
        }
    }
}
