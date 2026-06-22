package com.footballxtream

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import java.util.Locale

/**
 * Per-app language override, persisted in SharedPreferences and applied via [wrap] from
 * `attachBaseContext` on both the Application and the Activity. An empty tag means "follow the
 * device language" (the resource system then picks the right values-xx automatically).
 *
 * Done manually rather than through the framework per-app LocaleManager (which only exists on
 * Android 13+) so it also works on the Android 12 TV devices we target. Changing the language
 * relaunches the process so every context — including ViewModels that read strings off the
 * Application — picks up the new locale.
 */
object LocaleHelper {
    private const val PREFS = "locale_prefs"
    private const val KEY = "lang_tag"

    /** BCP-47 tags shipped with the app; "" = follow the device. Order drives the picker. */
    val supportedTags = listOf("", "en", "es", "ca", "eu", "gl", "pt", "fr", "it")

    fun persistedTag(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()

    /** Wraps [base] so its resources resolve in the chosen language (no-op for the device default). */
    fun wrap(base: Context): Context {
        val tag = persistedTag(base)
        if (tag.isEmpty()) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    /** Persists [tag] and relaunches the app so every context picks up the new language. */
    fun applyAndRestart(activity: Activity, tag: String) {
        // commit() (synchronous), not apply(): we kill the process right after, and an async apply()
        // would be lost before it reaches disk — leaving the language unchanged on relaunch.
        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, tag).commit()
        val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        activity.startActivity(intent)
        activity.finish()
        Runtime.getRuntime().exit(0)
    }
}
