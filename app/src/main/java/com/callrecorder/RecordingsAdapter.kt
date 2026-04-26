package com.callrecorder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.callrecorder.databinding.ItemRecordingBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingsAdapter(
    private val onPlay: (File) -> Unit,
    private val onDelete: (File) -> Unit
) : RecyclerView.Adapter<RecordingsAdapter.ViewHolder>() {

    private var recordings: List<File> = emptyList()

    fun submitList(list: List<File>) {
        recordings = list
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemRecordingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(file: File) {
            // Parse filename: call_NUMBER_TIMESTAMP.m4a
            val name = file.nameWithoutExtension
            val parts = name.split("_")
            val callerNumber = if (parts.size >= 2) parts[1].ifEmpty { "Unknown" } else "Unknown"

            // Format date from file last modified
            val dateFormat = SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(Date(file.lastModified()))

            // File size
            val sizeKB = file.length() / 1024
            val sizeStr = if (sizeKB > 1024) "%.1f MB".format(sizeKB / 1024f) else "$sizeKB KB"

            binding.tvCallerNumber.text = "📞 $callerNumber"
            binding.tvDate.text = dateStr
            binding.tvFileSize.text = sizeStr

            binding.btnPlay.setOnClickListener { onPlay(file) }
            binding.btnDelete.setOnClickListener { onDelete(file) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecordingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(recordings[position])
    }

    override fun getItemCount() = recordings.size
}
