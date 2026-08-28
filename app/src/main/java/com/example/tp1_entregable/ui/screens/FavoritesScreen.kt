package com.example.tp1_entregable.ui.screens

import android.view.LayoutInflater
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.tp1_entregable.R

@Composable
fun FavoritesScreen(innerPadding: PaddingValues) {
    AndroidView(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize(),
        factory = { context ->
            LayoutInflater.from(context).inflate(R.layout.layout_favorites, null)
        },
        update = { _ -> }
    )
}