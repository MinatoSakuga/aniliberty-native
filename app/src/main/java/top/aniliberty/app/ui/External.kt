package top.aniliberty.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Account confirmation always uses the user's real browser, never an embedded web surface. */
fun openExternal(context: Context, url: String): Boolean {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    if (uri.scheme != "https") return false
    return try { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true } catch (e: ActivityNotFoundException) { false }
}
const val OTP_DEVICE_URL = "https://aniliberty.top/app/auth/otp/linkDevice"
