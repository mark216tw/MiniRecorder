package com.mini.recorder.widget

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mini.recorder.QuickRecordActivity
import com.mini.recorder.R

private val WIDGET_BG = Color(0xFFFFDFE5)
private val WIDGET_FG = Color(0xFF8C1D3A)

class RecordWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(DpSize(60.dp, 40.dp), DpSize(170.dp, 40.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent()
        }
    }
}

@Composable
private fun WidgetContent() {
    val context = LocalContext.current
    val target = ComponentName(context, QuickRecordActivity::class.java)
    val size: DpSize? = LocalSize.current
    val compact = (size?.width ?: 170.dp) < 120.dp

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WIDGET_BG))
            .cornerRadius(16.dp)
            .clickable(actionStartActivity(target))
            .padding(horizontal = if (compact) 8.dp else 14.dp),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_mic),
            contentDescription = null,
            colorFilter = ColorFilter.tint(ColorProvider(WIDGET_FG)),
            modifier = GlanceModifier.size(if (compact) 26.dp else 32.dp)
        )
        Spacer(GlanceModifier.width(if (compact) 6.dp else 10.dp))
        Column {
            Text(
                text = context.getString(R.string.widget_brand),
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(WIDGET_FG.copy(alpha = 0.7f)),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = context.getString(R.string.widget_action_start),
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(WIDGET_FG),
                    fontSize = if (compact) 16.sp else 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

class RecordWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RecordWidget()
}
