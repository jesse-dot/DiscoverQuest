package com.discoverquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.discoverquest.audio.DiscoveryAudioManager
import com.discoverquest.permissions.PermissionHandler
import com.discoverquest.ui.MainViewModel
import com.discoverquest.ui.MapScreen

class MainActivity : ComponentActivity() {

    private lateinit var permissionHandler: PermissionHandler
    private lateinit var audioManager: DiscoveryAudioManager

    private val soundPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { audioManager.copyCustomSound(this, it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionHandler = PermissionHandler(this)
        audioManager = DiscoveryAudioManager(this)

        permissionHandler.requestPermissions()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val mainViewModel: MainViewModel = viewModel(
                        factory = MainViewModel.Factory(application)
                    )
                    MapScreen(
                        viewModel = mainViewModel,
                        onPickSound = { soundPickerLauncher.launch("audio/*") }
                    )
                }
            }
        }
    }
}
