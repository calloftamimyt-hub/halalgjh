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
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val foregroundPackage = event.packageName?.toString() ?: return
            
            // Skip safe apps and self
            if (foregroundPackage == packageName || 
                foregroundPackage == "com.android.launcher" || 
                foregroundPackage.contains("launcher") ||
                foregroundPackage.contains("com.google.android.googlequicksearchbox")) {
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

            when {
                // ... (Logic for identifying blocked apps remains same)
                foregroundPackage.contains("com.facebook.katana") || foregroundPackage.contains("com.facebook.lite") -> {
                    val isFbApp = sharedPrefs.getBoolean("fb_app_blocked", false)
                    val isFbStory = sharedPrefs.getBoolean("fb_story_blocked", false)
                    val isFbSearch = sharedPrefs.getBoolean("fb_search_blocked", false)
                    val isFbReels = sharedPrefs.getBoolean("fb_reels_blocked", false)
                    val isFbEntire = sharedPrefs.getBoolean("fb_entire_blocked", false)
                    if (isFbApp || isFbStory || isFbSearch || isFbReels || isFbEntire) {
                        shouldBlock = true
                        platformNameBengali = "ফেসবুক (Facebook)"
                    }
                }
                foregroundPackage.contains("com.google.android.youtube") || foregroundPackage.contains("com.google.android.apps.youtube.kids") -> {
                    val isYtLong = sharedPrefs.getBoolean("yt_long_blocked", false)
                    val isYtReels = sharedPrefs.getBoolean("yt_reels_blocked", false)
                    val isYtSearch = sharedPrefs.getBoolean("yt_search_blocked", false)
                    val isYtEntire = sharedPrefs.getBoolean("yt_entire_blocked", false)
                    if (isYtLong || isYtReels || isYtSearch || isYtEntire) {
                        shouldBlock = true
                        platformNameBengali = "ইউটিউব (YouTube)"
                    }
                }
                foregroundPackage.contains("com.instagram.android") -> {
                    val isIgApp = sharedPrefs.getBoolean("ig_app_blocked", false)
                    val isIgSearch = sharedPrefs.getBoolean("ig_search_blocked", false)
                    val isIgReels = sharedPrefs.getBoolean("ig_reels_blocked", false)
                    val isIgFeatures = sharedPrefs.getBoolean("ig_features_blocked", false)
                    val isIgEntire = sharedPrefs.getBoolean("ig_entire_blocked", false)
                    if (isIgApp || isIgSearch || isIgReels || isIgFeatures || isIgEntire) {
                        shouldBlock = true
                        platformNameBengali = "ইনস্টাগ্রাম (Instagram)"
                    }
                }
                foregroundPackage.contains("org.telegram.messenger") -> {
                    val isTgApp = sharedPrefs.getBoolean("tg_app_blocked", false)
                    val isTgSearch = sharedPrefs.getBoolean("tg_search_blocked", false)
                    val isTgStory = sharedPrefs.getBoolean("tg_story_blocked", false)
                    val isTgEntire = sharedPrefs.getBoolean("tg_entire_blocked", false)
                    if (isTgApp || isTgSearch || isTgStory || isTgEntire) {
                        shouldBlock = true
                        platformNameBengali = "টেলিগ্রাম (Telegram)"
                    }
                }
                foregroundPackage.contains("com.whatsapp") -> {
                    val isWaApp = sharedPrefs.getBoolean("wa_app_blocked", false)
                    val isWaStory = sharedPrefs.getBoolean("wa_story_blocked", false)
                    val isWaEntire = sharedPrefs.getBoolean("wa_entire_blocked", false)
                    if (isWaApp || isWaStory || isWaEntire) {
                        shouldBlock = true
                        platformNameBengali = "হোয়াটসঅ্যাপ (WhatsApp)"
                    }
                }
                foregroundPackage.contains("com.facebook.orca") -> {
                    val isMsApp = sharedPrefs.getBoolean("ms_app_blocked", false)
                    val isMsStory = sharedPrefs.getBoolean("ms_story_blocked", false)
                    val isMsEntire = sharedPrefs.getBoolean("ms_entire_blocked", false)
                    if (isMsApp || isMsStory || isMsEntire) {
                        shouldBlock = true
                        platformNameBengali = "মেসেঞ্জার (Messenger)"
                    }
                }
                isWebBlocked && (foregroundPackage.contains("chrome") || 
                                 foregroundPackage.contains("browser") || 
                                 foregroundPackage.contains("firefox") || 
                                 foregroundPackage.contains("opera") || 
                                 foregroundPackage.contains("sbrowser")) -> {
                    val blockedList = sharedPrefs.getString("blocked_websites_list", "") ?: ""
                    if (blockedList.isNotEmpty()) {
                        val firstBlocked = blockedList.split(",").firstOrNull { it.isNotBlank() } ?: "ওয়েবসাইট"
                        shouldBlock = true
                        platformNameBengali = "ওয়েবসাইট ($firstBlocked)"
                    }
                }
            }

            if (shouldBlock) {
                showBlockingOverlay(platformNameBengali)
            } else {
                hideBlockingOverlay()
            }
        }
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

