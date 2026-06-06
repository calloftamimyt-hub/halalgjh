package com.example

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.TextView
import com.example.ui.theme.*

class SocialAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isOverlayShowing = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val foregroundPackage = event.packageName?.toString() ?: ""
        val eventType = event.eventType

        // 1. Skip safe apps and self
        if (foregroundPackage == packageName) {
            // Fix for overlay flashing: if the overlay is showing, don't hide it 
            // when we receive an event from our own package (likely from the overlay itself).
            if (isOverlayShowing) return
            return
        }

        if (isLauncher(foregroundPackage)) {
            hideBlockingOverlay()
            return
        }

        val sharedPrefs = getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)
        val isSocialBlocked = sharedPrefs.getBoolean("social_blocked", false)
        val isWebBlocked = sharedPrefs.getBoolean("web_blocked", false)

        if (!isSocialBlocked && !isWebBlocked) {
            hideBlockingOverlay()
            return
        }

        var shouldBlock = false
        var platformNameBengali = ""

        // 2. Check Apps
        val appBlockName = getAppBlockedName(foregroundPackage, sharedPrefs)
        if (appBlockName != null) {
            shouldBlock = true
            platformNameBengali = appBlockName
        } 
        
        // 3. Check Websites (if app not already blocked)
        if (!shouldBlock && isWebBlocked && isBrowserPackage(foregroundPackage)) {
            val rootNode = event.source ?: rootInActiveWindow
            val url = rootNode?.let { findUrlInNodes(it) }
            if (url != null) {
                val blockedSiteMatch = getBlockedWebsiteMatch(url, sharedPrefs)
                if (blockedSiteMatch != null) {
                    shouldBlock = true
                    platformNameBengali = "ওয়েবসাইট ($blockedSiteMatch)"
                }
            }
        }

        if (shouldBlock) {
            showBlockingOverlay(platformNameBengali)
        } else if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // Only hide on package change if we're certain it shouldn't be blocked.
            hideBlockingOverlay()
        }
    }

    private fun isLauncher(pkg: String): Boolean {
        return pkg == "com.android.launcher" || 
               pkg.contains("launcher") || 
               pkg.contains("com.google.android.googlequicksearchbox")
    }

    private fun isBrowserPackage(pkg: String): Boolean {
        val browsers = listOf("chrome", "browser", "firefox", "opera", "sbrowser", "msedge", "vivaldi", "duckduckgo", "brave")
        return browsers.any { pkg.contains(it) }
    }

    private fun findUrlInNodes(node: AccessibilityNodeInfo): String? {
        val nodeId = node.viewIdResourceName?.lowercase() ?: ""
        if (nodeId.contains("url") || nodeId.contains("address") || nodeId.contains("location")) {
            val text = node.text?.toString()
            if (text != null && (text.contains(".") || text.contains("http"))) {
                return text
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findUrlInNodes(child)
            if (result != null) return result
        }
        return null
    }

    private fun getBlockedWebsiteMatch(url: String, sharedPrefs: android.content.SharedPreferences): String? {
        val blockedList = sharedPrefs.getString("blocked_websites_list", "") ?: ""
        if (blockedList.isBlank()) return null
        
        val sites = blockedList.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        val lowerUrl = url.lowercase()
        return sites.firstOrNull { lowerUrl.contains(it) }
    }

    private fun getAppBlockedName(foregroundPackage: String, sharedPrefs: android.content.SharedPreferences): String? {
        return when {
            foregroundPackage.contains("com.facebook.katana") || foregroundPackage.contains("com.facebook.lite") -> {
                if (isAnyBlocked(sharedPrefs, listOf("fb_app_blocked", "fb_story_blocked", "fb_search_blocked", "fb_reels_blocked", "fb_entire_blocked"))) {
                    "ফেসবুক (Facebook)"
                } else null
            }
            foregroundPackage.contains("com.google.android.youtube") || foregroundPackage.contains("com.google.android.apps.youtube.kids") -> {
                if (isAnyBlocked(sharedPrefs, listOf("yt_long_blocked", "yt_reels_blocked", "yt_search_blocked", "yt_entire_blocked"))) {
                    "ইউটিউব (YouTube)"
                } else null
            }
            foregroundPackage.contains("com.instagram.android") -> {
                if (isAnyBlocked(sharedPrefs, listOf("ig_app_blocked", "ig_search_blocked", "ig_reels_blocked", "ig_features_blocked", "ig_entire_blocked"))) {
                    "ইনস্টাগ্রাম (Instagram)"
                } else null
            }
            foregroundPackage.contains("org.telegram.messenger") -> {
                if (isAnyBlocked(sharedPrefs, listOf("tg_app_blocked", "tg_search_blocked", "tg_story_blocked", "tg_entire_blocked"))) {
                    "টেলিগ্রাম (Telegram)"
                } else null
            }
            foregroundPackage.contains("com.whatsapp") -> {
                if (isAnyBlocked(sharedPrefs, listOf("wa_app_blocked", "wa_story_blocked", "wa_entire_blocked"))) {
                    "হোয়াটসঅ্যাপ (WhatsApp)"
                } else null
            }
            foregroundPackage.contains("com.facebook.orca") -> {
                if (isAnyBlocked(sharedPrefs, listOf("ms_app_blocked", "ms_story_blocked", "ms_entire_blocked"))) {
                    "মেসেঞ্জার (Messenger)"
                } else null
            }
            else -> null
        }
    }

    private fun isAnyBlocked(sharedPrefs: android.content.SharedPreferences, keys: List<String>): Boolean {
        return keys.any { sharedPrefs.getBoolean(it, false) }
    }

    private fun showBlockingOverlay(platformName: String) {
        if (isOverlayShowing) return
        if (!Settings.canDrawOverlays(this)) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.blocking_overlay, null)
        
        overlayView?.apply {
            findViewById<TextView>(R.id.blocked_platform_text).text = platformName
            findViewById<Button>(R.id.btn_back_home).setOnClickListener {
                hideBlockingOverlay()
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_HOME)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        }

        try {
            windowManager?.addView(overlayView, params)
            isOverlayShowing = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideBlockingOverlay() {
        if (isOverlayShowing && overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
            isOverlayShowing = false
        }
    }

    override fun onInterrupt() {
        hideBlockingOverlay()
    }

    override fun onDestroy() {
        hideBlockingOverlay()
        super.onDestroy()
    }
}

