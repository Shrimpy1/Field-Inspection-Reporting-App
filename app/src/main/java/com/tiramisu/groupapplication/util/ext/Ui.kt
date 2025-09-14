package com.tiramisu.groupapplication.util.ext

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@SuppressLint("SuspiciousModifierThen")
@Composable
fun Modifier.clickableNoIndication(onClick: () -> Unit) =
    this.then(
        Modifier
            .background(Color.Transparent)
            .let {
                clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { onClick() }
            }
    )