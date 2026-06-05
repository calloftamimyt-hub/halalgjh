package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import com.example.viewmodel.AppLanguage

data class AppStrings(
    val app_name: String = "দ্বীন ইনসাফ",
    val home: String = "হোম",
    val video: String = "ভিডিও",
    val create: String = "তৈরি",
    val tracker: String = "ট্র্যাকার",
    val profile: String = "প্রোফাইল",
    val prayer_times_title: String = "সালাতের সময়",
    val next_prayer: String = "পরবর্তী সালাত",
    val see_all: String = "সবগুলো দেখুন",
    val collapse: String = "কমিয়ে আনুন",
    val categories: String = "ক্যাটাগরি",
    val forbidden_times: String = "নিষিদ্ধ সময়",
    val sunrise: String = "সূর্যোদয়",
    val sunset: String = "সূর্যাস্ত",
    val noon: String = "দ্বিপ্রহর",
    val location: String = "লোকেশন",
    val my_location: String = "আমার অবস্থান",
    val sehri_remaining: String = "সাহরির বাকি",
    val iftar_remaining: String = "ইফতারের বাকি",
    val app_settings: String = "অ্যাপ সেটিংস",
    val edit_profile: String = "প্রোফাইল এডিট করুন",
    val settings: String = "সেটিংস",
    val language: String = "ভাষা",
    val select_language: String = "আপনার পছন্দের ভাষা নির্বাচন করুন",
    val back: String = "পিছনে",
    val now: String = "এখন",
    val next: String = "পরবর্তী",
    val dhaka: String = "ঢাকা",
    val bangladesh: String = "বাংলাদেশ",
    val manual_location: String = "ম্যানুয়ালি সেট করুন",
    val auto_location: String = "স্বয়ংক্রিয় লোকেশন",
    val search: String = "অনুসন্ধান",
    val premium_user: String = "প্রিমিয়াম ইউজার",
    val version: String = "ভার্সন",
    val ok: String = "ঠিক আছে"
)

val bnStrings = AppStrings()
val enStrings = AppStrings(
    app_name = "Deen Insaf",
    home = "Home",
    video = "Video",
    create = "Create",
    tracker = "Tracker",
    profile = "Profile",
    prayer_times_title = "Prayer Times",
    next_prayer = "Next Prayer",
    see_all = "See All",
    collapse = "Collapse",
    categories = "Categories",
    forbidden_times = "Forbidden Times",
    sunrise = "Sunrise",
    sunset = "Sunset",
    noon = "Noon",
    location = "Location",
    my_location = "My Location",
    sehri_remaining = "Sehri Remaining",
    iftar_remaining = "Iftar Remaining",
    app_settings = "App Settings",
    edit_profile = "Edit Profile",
    settings = "Settings",
    language = "Language",
    select_language = "Select your preferred language",
    back = "Back",
    now = "Now",
    next = "Next",
    dhaka = "Dhaka",
    bangladesh = "Bangladesh",
    manual_location = "Manual Location",
    auto_location = "Auto Location",
    search = "Search",
    premium_user = "Premium User",
    version = "Version",
    ok = "OK"
)

val LocalAppStrings = compositionLocalOf { bnStrings }

@Composable
@ReadOnlyComposable
fun getString(language: AppLanguage): AppStrings {
    return if (language == AppLanguage.ENGLISH) enStrings else bnStrings
}
