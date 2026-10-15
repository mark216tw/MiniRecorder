package com.mini.recorder.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mini.recorder.data.db.Tag
import com.mini.recorder.data.prefs.ChannelMode
import com.mini.recorder.ui.components.AppTopBar
import com.mini.recorder.ui.components.HueSlider
import com.mini.recorder.ui.components.SelectableChip
import com.mini.recorder.ui.theme.hueColor
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()

    var showAddTag by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Tag?>(null) }
    var deleteTarget by remember { mutableStateOf<Tag?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AppTopBar(title = "設定") }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            SectionTitle("聲道")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectableChip(
                    label = "單聲道",
                    selected = settings.channelMode == ChannelMode.MONO,
                    onClick = { viewModel.setChannelMode(ChannelMode.MONO) }
                )
                SelectableChip(
                    label = "立體聲",
                    selected = settings.channelMode == ChannelMode.STEREO,
                    onClick = { viewModel.setChannelMode(ChannelMode.STEREO) }
                )
            }
            Text(
                "立體聲錄音時，部分裝置可能無法同時套用降噪/回音消除/AGC，會自動降級為單聲道。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider(Modifier.padding(vertical = 12.dp))
            SectionTitle("音效")
            SwitchRow(
                title = "降噪 / 消除回音 / 自動增益控制",
                checked = settings.effectsEnabled,
                onCheckedChange = viewModel::setEffectsEnabled
            )

            Divider(Modifier.padding(vertical = 12.dp))
            SectionTitle("靜音跳過")
            SwitchRow(
                title = "錄音時跳過靜音片段",
                checked = settings.silenceSkip,
                onCheckedChange = viewModel::setSilenceSkip
            )
            if (settings.silenceSkip) {
                SliderRow(
                    label = "靜音門檻：${settings.silenceThresholdDb} dB",
                    value = settings.silenceThresholdDb.toFloat(),
                    valueRange = -60f..(-10f),
                    steps = 49,
                    onValueChange = { viewModel.setSilenceThreshold(it.roundToInt()) }
                )
                SliderRow(
                    label = "最短靜音長度：${settings.silenceMinDurationMs} ms",
                    value = settings.silenceMinDurationMs.toFloat(),
                    valueRange = 300f..5000f,
                    steps = 46,
                    onValueChange = { viewModel.setSilenceMinDuration((it / 100f).roundToInt() * 100) }
                )
            }

            Divider(Modifier.padding(vertical = 12.dp))
            SectionTitle("錄音長度")
            SliderRow(
                label = "最長錄音時間：${settings.maxDurationMin} 分鐘",
                value = settings.maxDurationMin.toFloat(),
                valueRange = 30f..180f,
                steps = 4,
                onValueChange = { viewModel.setMaxDuration((it / 30f).roundToInt() * 30) }
            )
            Text(
                "以 30 分鐘為單位（30～180）。預設 60 分鐘。到達前 10 / 5 / 3 分鐘會發出提醒。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SwitchRow(
                title = "啟用時間提醒",
                checked = settings.warningsEnabled,
                onCheckedChange = viewModel::setWarningsEnabled
            )

            Divider(Modifier.padding(vertical = 12.dp))
            SectionTitle("標籤維護")
            Text(
                "在此新增、重新命名、設定顏色或刪除標籤；錄音時可直接從這些標籤中選擇。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { showAddTag = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("新增標籤")
            }
            Spacer(Modifier.height(8.dp))
            if (tags.isEmpty()) {
                Text("尚無標籤", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                tags.forEach { tag ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(hueColor(tag.colorHue))
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { editTarget = tag }
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(tag.name, modifier = Modifier.weight(1f))
                        IconButton(onClick = { editTarget = tag }) {
                            Icon(Icons.Filled.Edit, contentDescription = "編輯")
                        }
                        IconButton(onClick = { deleteTarget = tag }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "刪除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddTag) {
        TagEditorDialog(
            title = "新增標籤",
            initialName = "",
            initialHue = 226,
            confirmLabel = "新增",
            onConfirm = { name, hue ->
                if (name.isNotBlank()) viewModel.addTag(name, hue)
                showAddTag = false
            },
            onDismiss = { showAddTag = false }
        )
    }

    val target = editTarget
    if (target != null) {
        TagEditorDialog(
            title = "編輯標籤",
            initialName = target.name,
            initialHue = target.colorHue,
            onConfirm = { name, hue ->
                viewModel.renameTag(target, name)
                viewModel.setTagColor(target, hue)
                editTarget = null
            },
            onDismiss = { editTarget = null }
        )
    }

    val tagToDelete = deleteTarget
    if (tagToDelete != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("刪除標籤") },
            text = { Text("確定要刪除「${tagToDelete.name}」嗎？此操作無法復原。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTag(tagToDelete)
                    deleteTarget = null
                }) { Text("刪除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun TagEditorDialog(
    title: String,
    initialName: String,
    initialHue: Int,
    confirmLabel: String = "儲存",
    onConfirm: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var hue by remember { mutableIntStateOf(initialHue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("標籤名稱") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(hueColor(hue))
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("顏色", modifier = Modifier.weight(1f))
                }
                HueSlider(hue = hue, onHueChange = { hue = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim(), hue) }) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Column {
        Text(label)
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}
