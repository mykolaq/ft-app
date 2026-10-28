package app.ft.ui.components

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.ft.R
import app.ft.core.BluetoothCalls
import app.ft.core.BluetoothCallsState
import app.ft.core.BluetoothCallsStatus
import app.ft.ui.home.Fixes

@Composable
fun BluetoothCallsSetting() {
    val context = LocalContext.current
    var state by remember { mutableStateOf(BluetoothCallsState()) }
    var monitor by remember { mutableStateOf<BluetoothCalls?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Fixes.open(context, Fixes.appSettings(context))
        monitor?.refresh()
    }
    LifecycleResumeEffect(context) {
        val current = BluetoothCalls(context)
        monitor = current
        current.start()
        onPauseOrDispose {
            current.close()
            if (monitor === current) monitor = null
        }
    }
    LaunchedEffect(monitor) {
        monitor?.state?.collect { state = it }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.bluetooth_calls_title), style = MaterialTheme.typography.titleMedium)
        Text(when (state.status) {
            BluetoothCallsStatus.CHECKING -> stringResource(R.string.bluetooth_calls_checking)
            BluetoothCallsStatus.CONNECTED -> stringResource(R.string.bluetooth_calls_connected, state.devices.joinToString())
            BluetoothCallsStatus.DISCONNECTED -> stringResource(R.string.bluetooth_calls_disconnected)
            BluetoothCallsStatus.OFF -> stringResource(R.string.bluetooth_calls_off)
            BluetoothCallsStatus.PERMISSION_REQUIRED -> stringResource(R.string.bluetooth_calls_permission)
            BluetoothCallsStatus.UNAVAILABLE -> stringResource(R.string.bluetooth_calls_unavailable)
        })
        Text(stringResource(R.string.bluetooth_calls_about), style = MaterialTheme.typography.bodySmall)
        FilledTonalButton(onClick = {
            if (state.status == BluetoothCallsStatus.PERMISSION_REQUIRED && Build.VERSION.SDK_INT >= 31) {
                permission.launch(Manifest.permission.BLUETOOTH_CONNECT)
            } else Fixes.open(context, Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }) {
            Text(stringResource(if (state.status == BluetoothCallsStatus.PERMISSION_REQUIRED) R.string.bluetooth_calls_allow else R.string.bluetooth_calls_settings))
        }
    }
}
