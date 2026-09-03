package com.example.tp1_entregable.ui.screens

import android.view.LayoutInflater
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.tp1_entregable.R

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            LayoutInflater.from(context).inflate(R.layout.layout_profile, null)
        },
        modifier = modifier.fillMaxSize()
    )
}