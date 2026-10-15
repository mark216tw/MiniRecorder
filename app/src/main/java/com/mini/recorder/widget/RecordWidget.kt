package com.mini.recorder.widget

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mini.recorder.QuickRecordActivity

class RecordWidget : GlanceAppWidget() {

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
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF4C6EF5))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Mini錄音機",
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Text(
            text = "點此開始錄音",
            modifier = GlanceModifier
                .padding(top = 8.dp)
                .background(Color.White)
                .padding(10.dp)
                .clickable(actionStartActivity(target)),
            style = TextStyle(
                color = ColorProvider(Color(0xFF4C6EF5)),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

class RecordWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RecordWidget()
}
