package com.example.currency.ui

import android.text.format.DateUtils
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.currency.R
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: MainViewModel, onBack: () -> Unit) {
    val items by vm.history.collectAsStateWithLifecycle()
    val today = stringResource(R.string.today)
    Column(Modifier.fillMaxSize()) {
        GlassTopBar(stringResource(R.string.history), onBack)
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text(stringResource(R.string.no_history), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            // Group by date (Today / localized date)
            val grouped = items.groupBy { if (DateUtils.isToday(it.ts)) today else DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it.ts)) }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                grouped.forEach { (label, list) ->
                    item(key = "h$label") { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                    items(list, key = { it.id }) { h ->
                        val st = rememberSwipeToDismissBoxState(confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) { vm.deleteHistory(h.id); true } else false
                        })
                        SwipeToDismissBox(st, enableDismissFromStartToEnd = false, backgroundContent = {
                            Box(Modifier.fillMaxSize().padding(end = 24.dp), Alignment.CenterEnd) { Icon(Icons.Default.Delete, null, tint = ErrorRed) }
                        }) {
                            Text(stringResource(R.string.history_item, String.format("%,.2f", h.amount), h.from, String.format("%,.2f", h.result), h.to),
                                Modifier.fillMaxWidth().liquidGlass(12.dp, 20.dp, 0.1f, 0.2f).padding(16.dp),
                                color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}
