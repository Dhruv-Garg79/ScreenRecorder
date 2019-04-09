package com.andro_dhruv.screenrecorder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
import android.media.MediaMetadataRetriever.OPTION_CLOSEST
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.video_item.view.*
import java.io.File

class VideoFileAdapter(private val listener: VideoFileAdapter.OnItemClickListener) : RecyclerView.Adapter<VideoFileAdapter.MyViewHolder>(){
    private var list = emptyList<File>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoFileAdapter.MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.video_item, parent, false))
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: VideoFileAdapter.MyViewHolder, position: Int) {
        val videoFile = list[position]
        val metadata = MediaMetadataRetriever()
        metadata.setDataSource(videoFile.absolutePath)

        val runningTime : Long = metadata.extractMetadata(METADATA_KEY_DURATION).toLong()

        holder.name.text = videoFile.name
        holder.running_time.text = "${runningTime / 60000}:${runningTime / 1000}"

        val thumbnail : Bitmap? = metadata.getFrameAtTime(runningTime / 2, OPTION_CLOSEST)
        if (thumbnail != null) holder.thumbNail.setImageBitmap(thumbnail)

        holder.play.setOnClickListener { listener.onItemClickListener(videoFile) }
    }

    fun convertToBitmap(array : ByteArray) : Bitmap{
        return BitmapFactory.decodeByteArray(array, 0, array.size)
    }

    class MyViewHolder(itemview: View) : RecyclerView.ViewHolder(itemview){
        val name : TextView = itemview.name
        val running_time : TextView = itemview.running_time
        val play : ImageView = itemview.play
        val thumbNail : ImageView = itemview.thumbnail
    }

    fun updateData(list: List<File>){
        this.list = list
        notifyDataSetChanged()
    }

    interface OnItemClickListener{
        fun onItemClickListener(videoFile: File)
    }

}