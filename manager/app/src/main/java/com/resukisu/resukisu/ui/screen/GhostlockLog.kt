package com.resukisu.resukisu.ui.screen

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.resukisu.resukisu.R
import com.resukisu.resukisu.magica.GhostlockService
import com.resukisu.resukisu.ui.component.settings.AppBackButton
import com.resukisu.resukisu.ui.navigation.LocalNavigator
import com.resukisu.resukisu.ui.theme.CardConfig
import com.resukisu.resukisu.ui.theme.ThemeConfig
import com.resukisu.resukisu.ui.theme.blurEffect
import com.resukisu.resukisu.ui.theme.blurSource
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GhostlockLogScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    var text by rememberSaveable { mutableStateOf("") }
    var status by remember { mutableStateOf(FlashingStatus.FLASHING) }
    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val cacheDir = context.cacheDir.absolutePath
    val logPath = "$cacheDir/ghostlock_boot.log"
    val ansiRegex = remember { Regex("\\x1b\\[[0-9;]*m") }

    LaunchedEffect(Unit) {
        context.startForegroundService(Intent(context, GhostlockService::class.java))
        scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffsetLimit
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            try {
                val logFile = java.io.File(logPath)
                if (!logFile.exists()) continue
                val cleaned = logFile.readText().replace(ansiRegex, "")
                if (cleaned.isNotEmpty() && cleaned != text) text = cleaned
                status = when {
                    cleaned.contains("load_policy done") -> FlashingStatus.SUCCESS
                    cleaned.contains("child is root") && cleaned.contains("mini-adb returned 0") -> FlashingStatus.SUCCESS
                    cleaned.contains("failed after") || cleaned.contains("adbd not on TCP") -> FlashingStatus.FAILED
                    cleaned.contains("exploit complete") -> FlashingStatus.SUCCESS
                    else -> FlashingStatus.FLASHING
                }
            } catch (_: Exception) {}
            if (status != FlashingStatus.FLASHING) break
        }
    }

    LaunchedEffect(text) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    val statusColor = when (status) {
        FlashingStatus.FLASHING -> MaterialTheme.colorScheme.onSurface
        FlashingStatus.SUCCESS -> MaterialTheme.colorScheme.tertiary
        FlashingStatus.FAILED -> MaterialTheme.colorScheme.error
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.blurEffect(),
                title = {
                    Text(
                        text = when (status) {
                            FlashingStatus.FLASHING -> "GhostLock"
                            FlashingStatus.SUCCESS -> stringResource(R.string.flash_success)
                            FlashingStatus.FAILED -> stringResource(R.string.flash_failed)
                        },
                        color = statusColor
                    )
                },
                navigationIcon = { AppBackButton(onClick = { navigator.pop() }) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (ThemeConfig.isEnableBlur)
                        Color.Transparent
                    else
                        MaterialTheme.colorScheme.surfaceContainer.copy(CardConfig.cardAlpha),
                    scrolledContainerColor = if (ThemeConfig.isEnableBlur)
                        Color.Transparent
                    else
                        MaterialTheme.colorScheme.surfaceContainer.copy(CardConfig.cardAlpha),
                ),
                windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .blurSource()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = text.ifEmpty { "Waiting for exploit..." },
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
