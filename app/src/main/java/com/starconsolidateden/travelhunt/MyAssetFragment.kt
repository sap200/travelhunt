package com.starconsolidateden.travelhunt

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.starconsolidateden.travelhunt.api.RestService
import com.starconsolidateden.travelhunt.api.models.DigitalAssetResponse
import com.starconsolidateden.travelhunt.utils.SecurePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import javax.net.ssl.HttpsURLConnection

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MyAssetFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MyAssetFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var rvAssets: RecyclerView
    private val assets = mutableListOf<DigitalAssetResponse>()
    private lateinit var adapter: AssetAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        // Inflate the existing layout
        val view = inflater.inflate(R.layout.fragment_my_asset, container, false)

        // --- NEW CODE: RecyclerView setup ---
        rvAssets = view.findViewById(R.id.rv_assets)

        // Set up 2-column grid layout
        rvAssets.layoutManager = GridLayoutManager(requireContext(), 2)

        adapter = AssetAdapter(assets) { asset ->
            // For now: show a simple Toast on click
            val intent = Intent(requireContext(), AssetDetailsActivity::class.java)
            intent.putExtra("assetId", asset.assetId)
            intent.putExtra("imageUrl", asset.imagePath)
            intent.putExtra("claimed", asset.claimed)
            startActivity(intent)

        }

        rvAssets.adapter = adapter

        // Load dummy data
        fetchAssets()


        return view
    }



    private fun fetchAssets() {

        val publicAddress = SecurePrefs.getString("ADDRESS") ?: run {
            Toast.makeText(requireContext(), "User address not found", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val result = RestService.getAssetsByOwner(requireContext(), publicAddress)

            result.onSuccess { data ->
                assets.clear()
                assets.addAll(data)
                adapter.notifyDataSetChanged()
            }.onFailure { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MyAssetFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MyAssetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}