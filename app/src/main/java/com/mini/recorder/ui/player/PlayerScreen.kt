package com.mini.recorder.ui.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mini.recorder.data.db.Marker
import com.mini.recorder.ui.components.AppTopBar
import com.mini.recorder.ui.theme.hueColor
import com.mini.recorder.util.ShareUtils
import com.mini.recorder.util.TimeUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val recording by viewModel.recording.collectAsStateWithLifecycle()
    val markers by viewModel.markers.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val position by viewModel.position.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    var showTitleDialog by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showAddMarker by remember { mutableStateOf(false) }
    var editMarker by remember { mutableStateOf<Marker?>(null) }
    var draggingPosition by remember { mutableFloatStateOf(-1f) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(isPlaying) {
        if (isPlaying &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val shown = if (draggingPosition >= 0f) draggingPosition.toLong() else position
    val effectiveDuration = duration.coerceAtLeast(1L)
    val activeMarkerId = remember(shown, markers) {
        markers
            .filter { kotlin.math.abs(shown - it.timestampMs) < 1500L }
            .minByOrNull { kotlin.math.abs(shown - it.timestampMs) }?.id
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = "播放",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        recording?.recording?.let { ShareUtils.share(context, it) }
                    }) { Icon(Icons.Filled.Share, contentDescription = "分享") }
                    IconButton(onClick = { showDelete = true }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "刪除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val rec = recording?.recording
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            if (rec == null) {
                item { Text("載入中…") }
                return@LazyColumn
            }

            item {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = rec.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { showTitleDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "編輯標題",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${TimeUtils.formatDateTime(rec.createdAt)} · ${if (rec.channelCount == 2) "立體聲" else "單聲道"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(20.dp))

                    // 2.3 時間軸與標記點視覺化 (Markers Pin Visualizer)
                    TimelineWithMarkers(
                        currentMs = shown,
                        totalMs = effectiveDuration,
                        markers = markers,
                        onSeek = { target ->
                            draggingPosition = target.toFloat()
                        },
                        onSeekFinished = {
                            if (draggingPosition >= 0f) viewModel.seekTo(draggingPosition.toLong())
                            draggingPosition = -1f
                        }
                    )

                    // 時間戳資訊（等寬數字）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            TimeUtils.formatDuration(shown),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            TimeUtils.formatDuration(effectiveDuration),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // 2.2 聽寫控制項（快退 10 秒 / 快進 10 秒 + 0.5x~2.0x 倍速播放 + 前後要點快速跳轉）
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 倍速選單
                                Box {
                                    var speedMenuExpanded by remember { mutableStateOf(false) }
                                    AssistChip(
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            speedMenuExpanded = true
                                        },
                                        label = { Text("${playbackSpeed}x", fontWeight = FontWeight.Bold) },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )
                                    DropdownMenu(
                                        expanded = speedMenuExpanded,
                                        onDismissRequest = { speedMenuExpanded = false }
                                    ) {
                                        viewModel.speedOptions.forEach { speed ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        "${speed}x",
                                                        fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (playbackSpeed == speed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.setPlaybackSpeed(speed)
                                                    speedMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // 上一要點
                                IconButton(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.seekToPrevMarker()
                                    },
                                    enabled = markers.isNotEmpty()
                                ) {
                                    Icon(Icons.Filled.SkipPrevious, contentDescription = "上一要點")
                                }

                                // 快退 10 秒
                                IconButton(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.seekBy(-10_000L)
                                    }
                                ) {
                                    Icon(Icons.Filled.Replay10, contentDescription = "快退 10 秒", modifier = Modifier.size(28.dp))
                                }

                                // 主播放按鈕
                                FilledIconButton(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.togglePlay()
                                    },
                                    modifier = Modifier.size(68.dp)
                                ) {
                                    Icon(
                                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (isPlaying) "暫停" else "播放",
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                // 快進 10 秒
                                IconButton(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.seekBy(10_000L)
                                    }
                                ) {
                                    Icon(Icons.Filled.Forward10, contentDescription = "快進 10 秒", modifier = Modifier.size(28.dp))
                                }

                                // 下一要點
                                IconButton(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.seekToNextMarker()
                                    },
                                    enabled = markers.isNotEmpty()
                                ) {
                                    Icon(Icons.Filled.SkipNext, contentDescription = "下一要點")
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // 標籤選擇
                    Text("標籤", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    val selectedNames = recording?.tags?.map { it.name }?.toSet().orEmpty()
                    if (allTags.isEmpty()) {
                        Text(
                            "尚無標籤，請至「設定」頁新增標籤後再選擇。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allTags.forEach { tag ->
                                val selected = tag.name in selectedNames
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.toggleTag(tag.name) },
                                    leadingIcon = if (selected) {
                                        {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                                            )
                                        }
                                    } else null,
                                    label = {
                                        Text(
                                            tag.name,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        selectedContainerColor = hueColor(tag.colorHue),
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // 要點清單
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("要點（${markers.size}）", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (markers.isNotEmpty()) {
                                Text("點擊跳轉", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { showAddMarker = true }) {
                                Icon(Icons.Filled.Add, contentDescription = "新增要點")
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    if (markers.isEmpty()) {
                        Text("無標記要點", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (markers.isNotEmpty()) {
                items(markers, key = { it.id }) { marker ->
                    val isNearCurrent = marker.id == activeMarkerId
                    Card(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.seekTo(marker.timestampMs)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isNearCurrent) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        )
                    ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                            Icon(
                                Icons.Filled.Flag,
                                contentDescription = null,
                                tint = if (isNearCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                TimeUtils.formatDuration(marker.timestampMs),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isNearCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                marker.note,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isNearCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { editMarker = marker },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "編輯要點", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { viewModel.deleteMarker(marker) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "刪除要點",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showTitleDialog) {
        val currentTitle = recording?.recording?.title.orEmpty()
        var draft by remember(currentTitle) { mutableStateOf(currentTitle) }
        AlertDialog(
            onDismissRequest = { showTitleDialog = false },
            title = { Text("編輯標題") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("標題") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateTitle(draft.trim().ifEmpty { currentTitle })
                    showTitleDialog = false
                }) { Text("儲存") }
            },
            dismissButton = {
                TextButton(onClick = { showTitleDialog = false }) { Text("取消") }
            }
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("刪除錄音") },
            text = { Text("確定要刪除這筆錄音嗎？此操作無法復原。") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    viewModel.delete { onBack() }
                }) { Text("刪除") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("取消") }
            }
        )
    }

    if (showAddMarker) {
        var draft by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddMarker = false },
            title = { Text("新增要點") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("註記（可留空自動命名）") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addMarkerAtCurrentPosition(draft)
                    showAddMarker = false
                }) { Text("新增") }
            },
            dismissButton = {
                TextButton(onClick = { showAddMarker = false }) { Text("取消") }
            }
        )
    }

    val editingMarker = editMarker
    if (editingMarker != null) {
        var draft by remember(editingMarker.id) { mutableStateOf(editingMarker.note) }
        AlertDialog(
            onDismissRequest = { editMarker = null },
            title = { Text("編輯要點") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("註記") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateMarkerNote(editingMarker, draft)
                    editMarker = null
                }) { Text("儲存") }
            },
            dismissButton = {
                TextButton(onClick = { editMarker = null }) { Text("取消") }
            }
        )
    }
}

/**
 * 2.3 時間軸與標記點視覺化組件
 */
@Composable
fun TimelineWithMarkers(
    currentMs: Long,
    totalMs: Long,
    markers: List<Marker>,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (currentMs.toFloat() / totalMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    val markerTint = MaterialTheme.colorScheme.error

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            // 背景標記點指示小旗/線條
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .padding(horizontal = 8.dp)
            ) {
                val availableWidth = size.width
                markers.forEach { marker ->
                    val ratio = (marker.timestampMs.toFloat() / totalMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                    val x = ratio * availableWidth
                    // 繪製垂直刻度與頂部小圓點
                    drawLine(
                        color = markerTint,
                        start = Offset(x, 4f),
                        end = Offset(x, size.height - 4f),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawCircle(
                        color = markerTint,
                        radius = 3.dp.toPx(),
                        center = Offset(x, 5f)
                    )
                }
            }

            // 主要滑桿
            Slider(
                value = currentMs.toFloat().coerceIn(0f, totalMs.coerceAtLeast(1L).toFloat()),
                onValueChange = { onSeek(it.toLong()) },
                onValueChangeFinished = onSeekFinished,
                valueRange = 0f..totalMs.coerceAtLeast(1L).toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
