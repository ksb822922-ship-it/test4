package com.example.currency.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.currency.R
import com.example.currency.data.CURRENCIES
import com.example.currency.data.Cur
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

// Inter / JetBrains Mono: drop the .ttf files into res/font and swap these two families.
val HeadFont = FontFamily.SansSerif
val MonoFont = FontFamily.Monospace
private val Accent = Brush.linearGradient(listOf(Blue, Purple))
private val QuickPairs = listOf("USD" to "EUR", "USD" to "BYN", "EUR" to "BYN", "USD" to "RUB")

@Composable
fun GlassTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    GlassBox(Modifier.fillMaxWidth(), blurRadius = 30.dp, cornerRadius = 0.dp, tintAlpha = 0.3f, borderAlpha = 0.15f) {
        Row(Modifier.statusBarsPadding().height(56.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = MaterialTheme.colorScheme.onBackground) }
            else Icon(Icons.Default.CurrencyExchange, null, tint = Blue, modifier = Modifier.padding(horizontal = 8.dp))
            Text(title, Modifier.weight(1f), style = TextStyle(fontFamily = HeadFont, fontWeight = FontWeight.Bold, fontSize = 20.sp), color = MaterialTheme.colorScheme.onBackground)
            actions()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel, onHistory: () -> Unit, onSettings: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    var picking by remember { mutableStateOf<Boolean?>(null) }   // true = "from", false = "to"
    val shake = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        vm.shake.collect {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            repeat(3) { shake.animateTo(16f, tween(40)); shake.animateTo(-16f, tween(40)) }
            shake.animateTo(0f, tween(40))
        }
    }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(stringResource(R.string.app_name)) {
            IconButton(onHistory) { Icon(Icons.Default.History, stringResource(R.string.history), tint = MaterialTheme.colorScheme.onBackground) }
            IconButton(onSettings) { Icon(Icons.Default.Settings, stringResource(R.string.settings), tint = MaterialTheme.colorScheme.onBackground) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AnimatedVisibility(s.offline, enter = fadeIn() + expandVertically()) {
                Row(Modifier.fillMaxWidth().liquidGlass(12.dp, 16.dp, 0.1f, 0.2f).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudOff, null, tint = ErrorRed); Spacer(Modifier.width(8.dp))
                    val date = if (s.updatedAt > 0) DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(s.updatedAt)) else "—"
                    Text(stringResource(R.string.offline_banner, date), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
            AnimatedVisibility(shown, enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 6 }) {
                GlassBox(Modifier.fillMaxWidth(), blurRadius = 24.dp, cornerRadius = 32.dp,
                    borderBrush = Brush.linearGradient(listOf(Blue, Purple))) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CurrencyRow(stringResource(R.string.from), s.from) { picking = true }
                        BasicTextField(
                            value = s.amount, onValueChange = vm::setAmount, singleLine = true,
                            textStyle = TextStyle(fontFamily = HeadFont, fontWeight = FontWeight.Bold, fontSize = 36.sp, color = MaterialTheme.colorScheme.onBackground),
                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                            cursorBrush = SolidColor(Blue),
                            modifier = Modifier.fillMaxWidth().offset { IntOffset(shake.value.roundToInt(), 0) },
                            decorationBox = { inner ->
                                Box { if (s.amount.isEmpty()) Text(stringResource(R.string.enter_amount), fontSize = 36.sp, color = ErrorRed.copy(0.6f)); inner() }
                            })
                        HorizontalDivider(color = if (s.value == null) ErrorRed else Color.White.copy(0.15f))
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { SwapButton { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.swap() } }
                        CurrencyRow(stringResource(R.string.to), s.to) { picking = false }
                        ResultText(s)
                    }
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(QuickPairs) { (f, t) ->
                    Text("$f → $t", Modifier.liquidGlass(12.dp, 20.dp, 0.1f, 0.2f).clickable { vm.setPair(f, t) }.padding(horizontal = 16.dp, vertical = 10.dp),
                        color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
                }
            }
            Button(vm::refresh, Modifier.fillMaxWidth().height(52.dp), enabled = !s.loading, shape = CircleShape) {
                Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.refresh))
            }
        }
    }

    picking?.let { isFrom ->
        PickerSheet({ picking = null }) { code -> if (isFrom) vm.setFrom(code) else vm.setTo(code); picking = null }
    }
}

@Composable
private fun CurrencyRow(label: String, code: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(CircleShape).clickable(onClick = onClick).padding(vertical = 4.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        // Crossfade (200ms) when the currency changes.
        Crossfade(code, animationSpec = tween(200), label = "cur") { c ->
            val cur = Cur(c)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(cur.flag, fontSize = 28.sp); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(cur.code, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp)
                    Text(cur.name, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1)
                }
                Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SwapButton(onSwap: () -> Unit) {
    var turns by remember { mutableIntStateOf(0) }
    val angle by animateFloatAsState(turns * 180f, tween(350), label = "rot")
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, label = "scale")
    GlassBox(Modifier.size(56.dp).scale(scale).clickable(src, null) { turns++; onSwap() },
        blurRadius = 16.dp, cornerRadius = 28.dp, tintAlpha = 0.15f) {
        // Specular highlight on the top half
        Box(Modifier.fillMaxWidth().height(24.dp).background(Brush.verticalGradient(listOf(Color.White.copy(0.35f), Color.Transparent))))
        Icon(Icons.Default.SwapVert, stringResource(R.string.swap), Modifier.align(Alignment.Center).rotate(angle), tint = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun ResultText(s: UiState) {
    // Animated counter towards the new value
    val target = (s.result ?: 0.0).toFloat()
    val v by animateFloatAsState(target, tween(500), label = "counter")
    // Shimmer (pulse) while rates are refreshing
    val pulse by rememberInfiniteTransition(label = "shimmer").animateFloat(0.3f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "p")
    val text = if (s.result == null) "—" else String.format("%,.2f", v)
    Text(text, Modifier.fillMaxWidth().alpha(if (s.loading) pulse else 1f), maxLines = 1, softWrap = false, textAlign = TextAlign.Center,
        style = TextStyle(brush = Accent, fontFamily = MonoFont, fontWeight = FontWeight.Bold, fontSize = 48.sp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerSheet(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    var q by remember { mutableStateOf("") }
    ModalBottomSheet(onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), singleLine = true,
            placeholder = { Text(stringResource(R.string.search)) }, leadingIcon = { Icon(Icons.Default.Search, null) })
        LazyColumn(Modifier.fillMaxHeight(0.7f)) {
            items(CURRENCIES.filter { it.code.contains(q, true) || it.name.contains(q, true) }) { c ->
                Row(Modifier.fillMaxWidth().clickable { onPick(c.code) }.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(c.flag, fontSize = 24.sp); Spacer(Modifier.width(12.dp))
                    Text(c.code, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Spacer(Modifier.width(8.dp))
                    Text(c.name, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}
