package com.ancient.wenyan.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.ancient.wenyan.MainActivity
import com.ancient.wenyan.R
import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.ui.screens.CURATED_QUOTES
import java.time.LocalDate

/**
 * 桌面微件 (AppWidget)：文言背诵 · 每日待办与名句
 * 桌面直显今日待复习句数、打卡连胜火焰与每日精选古文名句，点击直达主界面背诵。
 */
class WenYanTodayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val componentName = ComponentName(context, WenYanTodayWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: return
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            } catch (_: Throwable) {
                // Defensive catch for widget manager availability
            }
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_wenyan_today)

            // 1. Pending intent to launch MainActivity
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            // 2. Fetch stats safely from repository
            val repo = WenYanRepository.getInstance(context)
            val dueCount = try {
                repo.getDueQueue().size
            } catch (_: Throwable) {
                0
            }
            val streak = try {
                repo.computeHeatmapStats().currentStreak
            } catch (_: Throwable) {
                0
            }

            views.setTextViewText(R.id.widget_streak_text, "🔥 ${streak}天连胜")
            views.setTextViewText(R.id.widget_due_count, if (dueCount > 0) "待复习 ${dueCount} 句" else "今日已完成 ✨")

            // 3. Daily Curated Classical Quote
            val dayOfYear = LocalDate.now().dayOfYear
            val quote = CURATED_QUOTES[dayOfYear % CURATED_QUOTES.size]
            views.setTextViewText(R.id.widget_quote_text, quote.quote)
            views.setTextViewText(R.id.widget_quote_author, "—— ${quote.author}《${quote.title}》")

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
