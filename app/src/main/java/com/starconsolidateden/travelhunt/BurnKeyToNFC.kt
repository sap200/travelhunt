package com.starconsolidateden.travelhunt

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareUltralight
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.starconsolidateden.travelhunt.utils.SecurePrefs

class BurnKeyToNFC : Fragment() {

    private var nfcAdapter: NfcAdapter? = null
    private var objectId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get ObjectId from SecurePrefs
        objectId = SecurePrefs.getString("OBJECT_ID")

        if (objectId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "No ObjectId found", Toast.LENGTH_LONG).show()
        }

        // Initialize NFC adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_burn_key_to_n_f_c, container, false)
    }

    override fun onResume() {
        super.onResume()

        val activity = requireActivity()

        if (nfcAdapter == null) {
            Toast.makeText(activity, "NFC not supported", Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(activity, activity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            activity,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE
        )

        nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, null, null)

        Toast.makeText(activity, "Tap NFC card to burn key", Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(requireActivity())
    }

    /**
     * Call this from Activity's onNewIntent()
     */
    fun onNfcTagDetected(tag: Tag) {
        val id = objectId ?: return

        try {
            writeObjectId(tag, id)
            Toast.makeText(requireContext(), "Key burned to NFC successfully", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Write failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun writeObjectId(tag: Tag, objectIdHex: String) {
        val ultralight = MifareUltralight.get(tag)
            ?: throw IllegalStateException("Not a MIFARE Ultralight tag")

        val bytes = hexToBytes(objectIdHex)
        require(bytes.size == 12)

        ultralight.connect()
        try {
            ultralight.writePage(4, bytes.copyOfRange(0, 4))
            ultralight.writePage(5, bytes.copyOfRange(4, 8))
            ultralight.writePage(6, bytes.copyOfRange(8, 12))
        } finally {
            ultralight.close()
        }
    }

    private fun hexToBytes(hex: String): ByteArray {
        return ByteArray(hex.length / 2) {
            hex.substring(it * 2, it * 2 + 2).toInt(16).toByte()
        }
    }
}
