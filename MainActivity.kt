package com.rohit.framelessshortcut

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.net.URL
import java.util.UUID
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etAppName = findViewById<EditText>(R.id.etAppName)
        val etUrl = findViewById<EditText>(R.id.etUrl)
        val cbUseQuetta = findViewById<CheckBox>(R.id.cbUseQuetta)
        val btnCreate = findViewById<Button>(R.id.btnCreateShortcut)

        btnCreate.setOnClickListener {
            val name = etAppName.text.toString().trim()
            var url = etUrl.text.toString().trim()

            if (name.isEmpty() || url.isEmpty()) {
                Toast.makeText(this, "कृपया नाम और URL दोनों डालें", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }

            btnCreate.isEnabled = false
            btnCreate.text = "Icon Fetch हो रहा है..."

            thread {
                val favicon = fetchFavicon(url)
                runOnUiThread {
                    btnCreate.isEnabled = true
                    btnCreate.text = "Create Home Screen Shortcut"
                    createPinnedShortcut(name, url, cbUseQuetta.isChecked, favicon)
                }
            }
        }
    }

    private fun fetchFavicon(siteUrl: String): Bitmap? {
        return try {
            val host = Uri.parse(siteUrl).host ?: return null
            val iconUrl = URL("https://www.google.com/s2/favicons?domain=$host&sz=128")
            BitmapFactory.decodeStream(iconUrl.openConnection().getInputStream())
        } catch (e: Exception) {
            null
        }
    }

    private fun createPinnedShortcut(name: String, url: String, useQuetta: Boolean, iconBitmap: Bitmap?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcutManager = getSystemService(ShortcutManager::class.java)

            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
                val launchIntent: Intent = if (useQuetta && isPackageInstalled("net.quetta.browser")) {
                    Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        setPackage("net.quetta.browser")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                } else {
                    if (useQuetta) {
                        Toast.makeText(this, "Quetta नहीं मिला, Frameless मोड में बना रहे हैं", Toast.LENGTH_SHORT).show()
                    }
                    Intent(this, WebAppActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        data = Uri.parse(url)
                        putExtra("APP_TITLE", name)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                        addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    }
                }

                val shortcutIcon = if (iconBitmap != null) {
                    Icon.createWithBitmap(iconBitmap)
                } else {
                    Icon.createWithResource(this, R.mipmap.ic_launcher)
                }

                val pinShortcutInfo = ShortcutInfo.Builder(this, "shortcut_${UUID.randomUUID()}")
                    .setShortLabel(name)
                    .setLongLabel(name)
                    .setIcon(shortcutIcon)
                    .setIntent(launchIntent)
                    .build()

                val pinnedShortcutCallbackIntent = shortcutManager.createShortcutResultIntent(pinShortcutInfo)
                val successCallback = PendingIntent.getBroadcast(
                    this, 0, pinnedShortcutCallbackIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )

                shortcutManager.requestPinShortcut(pinShortcutInfo, successCallback.intentSender)
            } else {
                Toast.makeText(this, "आपका लॉन्चर Pinned Shortcuts सपोर्ट नहीं करता", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
