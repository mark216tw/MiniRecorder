package com.mini.recorder.ui.record

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mini.recorder.service.RecordingService
import com.mini.recorder.ui.components.AppTopBar
import com.mini.recorder.util.TimeUtils
import kotlin.math.log10

@Composable
fun RecordScreen(viewModel: RecordViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var titleDraft by remember { mutableStateOf("") }
    var showTitleDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasAudioPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun requestPermissions() {
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    LaunchedEffect(Unit) {
        if (!hasAudioPermission) requestPermissions()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AppTopBar(title = "錄音") }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            if (!hasAudioPermission) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("需要麥克風權限才能錄音。", fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { requestPermissions() }) { Text("授予權限") }
                    }
                }
                return@Column
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (titleDraft.isBlank()) "標題：自動命名" else "標題：$titleDraft",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { showTitleDialog = true },
                    enabled = !state.isRecording,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "編輯標題",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (showTitleDialog) {
                var draft by remember { mutableStateOf(titleDraft) }
                AlertDialog(
                    onDismissRequest = { showTitleDialog = false },
                    title = { Text("設定標題") },
                    text = {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            label = { Text("標題（可留空自動命名）") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            titleDraft = draft.trim()
                            showTitleDialog = false
                        }) { Text("確定") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTitleDialog = false }) { Text("取消") }
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            // 1.2 等寬計時器 + 呼吸動效錄音狀態指示燈
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.35f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(750, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseAlpha"
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (state.isRecording && !state.isPaused) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.Red.copy(alpha = pulseAlpha))
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = when {
                            !state.isRecording -> "待機"
                            state.isPaused -> "已暫停"
                            else -> "錄音中"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = when {
                            !state.isRecording -> MaterialTheme.colorScheme.onSurfaceVariant
                            state.isPaused -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.error
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = TimeUtils.formatDuration(state.elapsedMs),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 44.sp
                    )
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = buildString {
                        append(if (state.channelCount == 2) "立體聲" else "單聲道")
                        append(" · ")
                        append(if (settings.effectsEnabled) "音效開啟" else "音效關閉")
                        if (settings.silenceSkip) append(" · 靜音跳過")
                        append(" · 上限 ${settings.maxDurationMin} 分")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            // 1.1 即時聲波 Visualizer
            LiveWaveformVisualizer(
                amplitudes = state.amplitudeHistory,
                isRecording = state.isRecording && !state.isPaused,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            )

            state.message?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(Modifier.height(24.dp))

            // 1.3 拇指熱區人體工學按鈕佈局 + 觸覺反饋
            if (!state.isRecording) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    FilledIconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            ContextCompat.startForegroundService(
                                context,
                                RecordingService.startIntent(context, titleDraft)
                            )
                        },
                        enabled = !isPlaying,
                        modifier = Modifier
                            .size(80.dp)
                            .border(
                                width = 3.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                shape = CircleShape
                            ),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Filled.Mic,
                            contentDescription = "開始錄音",
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 標記要點
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledTonalIconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                context.startService(
                                    RecordingService.actionIntent(context, RecordingService.ACTION_MARK)
                                )
                            },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(Icons.Filled.Flag, contentDescription = "標記要點", modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("標記", style = MaterialTheme.typography.labelSmall)
                    }

                    // 暫停 / 繼續（核心主鍵）
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledIconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                val action = if (state.isPaused) {
                                    RecordingService.ACTION_RESUME
                                } else {
                                    RecordingService.ACTION_PAUSE
                                }
                                context.startService(RecordingService.actionIntent(context, action))
                            },
                            enabled = !(state.isPaused && isPlaying),
                            modifier = Modifier.size(76.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (state.isPaused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(
                                if (state.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = if (state.isPaused) "繼續" else "暫停",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(if (state.isPaused) "繼續" else "暫停", style = MaterialTheme.typography.labelSmall)
                    }

                    // 停止
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledIconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                context.startService(
                                    RecordingService.actionIntent(context, RecordingService.ACTION_STOP)
                                )
                            },
                            modifier = Modifier.size(56.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = "停止", modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("停止", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // 標記要點列表
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "要點記錄 (${state.markers.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(6.dp))

            if (state.markers.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (state.isRecording) "點擊「標記」可記錄重點時間戳記" else "錄音時可隨時加入標記",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(state.markers, key = { it.id }) { marker ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Flag,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    TimeUtils.formatDuration(marker.timestampMs),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    marker.note,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 1.1 實時聲波視覺化 Canvas
 */
@Composable
fun LiveWaveformVisualizer(
    amplitudes: List<Float>,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    barWidth: Dp = 3.dp,
    barGap: Dp = 2.dp
) {
    Canvas(modifier = modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
        val widthPx = barWidth.toPx()
        val gapPx = barGap.toPx()
        val slotWidth = widthPx + gapPx
        val maxBars = (size.width / slotWidth).toInt().coerceAtLeast(10)

        // 取得填滿畫面的振幅資料（不足則在前面補微弱底噪）
        val recent = amplitudes.takeLast(maxBars)
        val padCount = (maxBars - recent.size).coerceAtLeast(0)
        val floorFactor = 0.05f

        val centerY = size.height / 2f
        val maxHeight = size.height * 0.9f

        fun factorOf(amp: Float): Float {
            val db = 20f * log10(amp.coerceAtLeast(1e-5f))
            return ((db + 50f) / 50f).coerceIn(0.04f, 1f)
        }

        for (index in 0 until maxBars) {
            val factor = when {
                index < padCount -> floorFactor
                !isRecording -> floorFactor
                else -> factorOf(recent[index - padCount])
            }
            val barHeight = (factor * maxHeight).coerceAtLeast(4f)
            val x = index * slotWidth + widthPx / 2f

            drawLine(
                color = if (isRecording) barColor else barColor.copy(alpha = 0.35f),
                start = Offset(x, centerY - barHeight / 2f),
                end = Offset(x, centerY + barHeight / 2f),
                strokeWidth = widthPx,
                cap = StrokeCap.Round
            )
        }
    }
}
