package com.tiramisu.groupapplication.ui.component

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.tiramisu.groupapplication.ui.viewmodel.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.withContext

@Composable
fun ConnectivityToasts(
    viewModel: AppViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.isOnline
            .drop(1)
            .collect { isOnline ->
                withContext(Dispatchers.Main) {
                    val msg = if (isOnline) "Back online!" else "You're offline"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
    }
}