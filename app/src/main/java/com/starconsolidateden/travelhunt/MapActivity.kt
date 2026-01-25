package com.starconsolidateden.travelhunt

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.starconsolidateden.travelhunt.base.BaseActivity


class MapActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_map)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.MapActivity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_map -> MapFragment()
                R.id.nav_home -> MyAssetFragment()
                R.id.nav_profile -> ProfileFragment()
                R.id.nav_export -> BurnKeyToNFC()
                R.id.nav_create -> CreateDigitalAssetFragment()
                else -> MapFragment()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()

            true
        }

        // ✅ Default screen = MAP
        bottomNav.selectedItemId = R.id.nav_map
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return

        val fragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (fragment is BurnKeyToNFC && fragment.isVisible) {
            fragment.onNfcTagDetected(tag)
        }
    }

}

private fun Intent.getParcelableExtra(extraTag: Any) {}
