package com.starconsolidateden.travelhunt

import android.app.Application
import com.starconsolidateden.travelhunt.utils.SecurePrefs

class TravelHuntApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize EncryptedSharedPreferences here
        SecurePrefs.init(this)
    }
}
