package com.gybra.terminallauncher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gybra.terminallauncher.launcher.InstalledApp

@Composable
public fun HomeScreen(
    state: HomeUiState,
    onAppClick: (InstalledApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(
            items = state.apps,
            key = InstalledApp::packageName,
        ) { app ->
            AppRow(
                app = app,
                onClick = { onAppClick(app) },
            )
        }
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    onClick: () -> Unit,
) {
    BasicText(
        text = app.label,
        style = TextStyle(
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            lineHeight = 24.sp,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
    )
}
