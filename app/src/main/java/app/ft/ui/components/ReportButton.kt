package app.ft.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import app.ft.ui.theme.Localized
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.ft.core.DiagReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.stringResource
import app.ft.R

@Composable
fun ReportButton() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var systemHelp by rememberSaveable { mutableStateOf(false) }
    val sendReport: () -> Unit = {
        menu = false
        busy = true
        scope.launch {
            val app = context.applicationContext
            val report = withContext(Dispatchers.IO) { runCatching { DiagReport.build(app) }.getOrNull() }
            val uri = report?.let { withContext(Dispatchers.IO) { DiagReport.saveToDownloads(app, it) } }
            busy = false
            if (report == null || uri == null) {
                Toast.makeText(context, context.getString(R.string.report_failed), Toast.LENGTH_LONG).show()
                return@launch
            }
            Toast.makeText(context, context.getString(R.string.report_saved), Toast.LENGTH_LONG).show()
            val send = Intent(Intent.ACTION_SEND)
                .setType("application/zip")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_SUBJECT, report.name)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            runCatching { context.startActivity(Intent.createChooser(send, context.getString(R.string.report_send)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }
    Box {
        IconButton(enabled = !busy, onClick = { menu = true }) {
            if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            else Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.report_cd))
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            Localized {
                DropdownMenuItem(text = { Text(stringResource(R.string.report_send)) }, onClick = sendReport)
                DropdownMenuItem(text = { Text(stringResource(R.string.report_system_help)) }, onClick = {
                    menu = false
                    systemHelp = true
                })
            }
        }
    }
    if (systemHelp) SystemReportHelp(onDismiss = { systemHelp = false })
}
