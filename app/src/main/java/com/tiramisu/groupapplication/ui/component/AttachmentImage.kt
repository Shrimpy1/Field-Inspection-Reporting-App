package com.tiramisu.groupapplication.ui.component

import android.util.Log
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

@Composable
fun AttachmentImage(
    model: Any,
    modifier: Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current

    Log.d("Image", "model: $model")

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(model)
            .diskCachePolicy(CachePolicy.ENABLED)        // keep on disk
            .memoryCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)     // use HTTP cache if remote
            .crossfade(true)
            .build(),
        contentDescription = null,
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        contentScale = contentScale
    )
}