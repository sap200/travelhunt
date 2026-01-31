package com.starconsolidateden.travelhunt

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareUltralight
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.starconsolidateden.travelhunt.api.RestService
import com.starconsolidateden.travelhunt.utils.SecurePrefs
import kotlinx.coroutines.launch

private const val TOKEN_EXPIRY_MS = 36_000_000L // 10 hours

class NFCLoginActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfclogin) // Your layout with pulse view

        // Get NFC adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            Toast.makeText(this, "This device does not support NFC", Toast.LENGTH_LONG).show()
            finish()
            return
        }

    }

    override fun onResume() {
        super.onResume()

        // Enable foreground dispatch to capture NFC while activity is in foreground
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_MUTABLE
        )

        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)


    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    // Handle NFC tag when scanned
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        tag?.let {
            try {
                val objectId = readObjectId(it)

                lifecycleScope.launch {
                    try {
                        val result = RestService.objectIdLogin(objectId);
                        result.onSuccess {response->
                            if(response.status.equals("success")) {
                                SecurePrefs.saveString("JWT_TOKEN", response.token)
                                SecurePrefs.saveString("EMAIL", response.email)
                                SecurePrefs.saveString("ADDRESS", response.address)
                                SecurePrefs.saveString("PASSWORD", "")
                                SecurePrefs.saveString("OBJECT_ID", objectId)
                                SecurePrefs.saveString("EXPIRY_TIME",  (System.currentTimeMillis() + TOKEN_EXPIRY_MS).toString())

                                Toast.makeText(this@NFCLoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                                // move on to the map activity
                                val intent = Intent(this@NFCLoginActivity, MapActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                            } else {
                                Toast.makeText(this@NFCLoginActivity, response.errorMessage, Toast.LENGTH_SHORT).show()
                            }
                        }.onFailure { error ->
                            Toast.makeText(this@NFCLoginActivity, "Response-Error: ${error.message}", Toast.LENGTH_SHORT).show()

                        }
                    } catch(ex:Exception) {
                        Toast.makeText(this@NFCLoginActivity, "Error: ${ex.message}", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                Toast.makeText(this, "Failed to read NFC tag: ${e.message}", Toast.LENGTH_LONG).show()
            }

        }
    }

    // Read 12-byte MongoDB ObjectId from Mifare Ultralight tag (pages 4–6)
    private fun readObjectId(tag: Tag): String {
        val ultralight = MifareUltralight.get(tag)
            ?: throw IllegalStateException("Not a Mifare Ultralight tag")

        ultralight.connect()
        return try {
            val page4 = ultralight.readPages(4) // returns 16 bytes (pages 4–7)
            val objectIdBytes = page4.copyOfRange(0, 12) // take first 12 bytes
            objectIdBytes.joinToString("") { "%02x".format(it) } // convert to hex string
        } finally {
            ultralight.close()
        }
    }
}
