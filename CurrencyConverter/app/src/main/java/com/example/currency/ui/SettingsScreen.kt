package com.example.currency.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.currency.R
import com.example.currency.data.Cur

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val theme by vm.theme.collectAsStateWithLifecycle()
    val offline by vm.offlineMode.collectAsStateWithLifecycle()
    val from by vm.defFrom.collectAsStateWithLifecycle()
    val to by vm.defTo.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf<Boolean?>(null) }
    val on = MaterialTheme.colorScheme.onBackground

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(stringResource(R.string.settings), onBack)
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.fillMaxWidth().liquidGlass().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.theme), fontWeight = FontWeight.Bold, color = on)
                val names = listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    names.forEachIndexed { i, r ->
                        SegmentedButton(theme == i, { vm.setTheme(i) }, SegmentedButtonDefaults.itemShape(i, names.size)) { Text(stringResource(r), maxLines = 1) }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().liquidGlass().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.default_pair), fontWeight = FontWeight.Bold, color = on)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton({ picking = true }, Modifier.weight(1f)) { Text("${Cur(from).flag} $from") }
                    OutlinedButton({ picking = false }, Modifier.weight(1f)) { Text("${Cur(to).flag} $to") }
                }
            }
            Row(Modifier.fillMaxWidth().liquidGlass().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.offline_mode), Modifier.weight(1f), color = on, fontWeight = FontWeight.Bold)
                Switch(offline, vm::setOffline)
            }
        }
    }
    picking?.let { isFrom ->
        PickerSheet({ picking = null }) { c -> if (isFrom) vm.setDefaults(c, to) else vm.setDefaults(from, c); picking = null }
    }
}
