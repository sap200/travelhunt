package com.starconsolidateden.travelhunt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.starconsolidateden.travelhunt.api.models.DigitalAssetResponse

class AssetAdapter(
    private val assets: List<DigitalAssetResponse>,
    private val onClick: (DigitalAssetResponse) -> Unit
) : RecyclerView.Adapter<AssetAdapter.AssetViewHolder>() {

    inner class AssetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAsset: ImageView = itemView.findViewById(R.id.img_asset)
        val tvAssetId: TextView = itemView.findViewById(R.id.tv_asset_id)
        val tvClaimed: TextView = itemView.findViewById(R.id.tv_claimed)

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_asset, parent, false)
        return AssetViewHolder(view)
    }

    override fun onBindViewHolder(holder: AssetViewHolder, position: Int) {
        val asset = assets[position]
        holder.tvAssetId.text = asset.assetId
        holder.tvAssetId.isSelected = true


        // Load image
        Glide.with(holder.itemView.context)
            .load(asset.imagePath)
            .fitCenter()
            .into(holder.imgAsset)

        // Show "CLAIMED" tag if asset.claimed == true
        if (asset.claimed) {
            holder.tvClaimed.visibility = View.VISIBLE
        } else {
            holder.tvClaimed.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onClick(asset) }
    }

    override fun getItemCount(): Int = assets.size
}
