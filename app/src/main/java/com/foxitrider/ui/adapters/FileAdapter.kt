package com.foxitrider.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.foxitrider.R
import com.foxitrider.utils.PdfUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileAdapter(
    private val onFileClick: (File) -> Unit,
    private val onFileLongClick: (File) -> Unit
) : ListAdapter<File, FileAdapter.FileViewHolder>(FileDiffCallback()) {

    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivFileIcon)
        val tvName: TextView = itemView.findViewById(R.id.tvFileName)
        val tvInfo: TextView = itemView.findViewById(R.id.tvFileInfo)
        val tvDate: TextView = itemView.findViewById(R.id.tvFileDate)

        fun bind(file: File) {
            tvName.text = file.name

            val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

            if (file.isDirectory) {
                ivIcon.setImageResource(R.drawable.ic_folder)
                val fileCount = file.listFiles()?.size ?: 0
                tvInfo.text = "$fileCount item"
                tvDate.text = dateFormat.format(Date(file.lastModified()))
            } else {
                ivIcon.setImageResource(R.drawable.ic_pdf)
                val size = PdfUtils.formatFileSize(file.length())
                val pages = PdfUtils.getPageCount(file)
                tvInfo.text = "$size • $pages halaman"
                tvDate.text = dateFormat.format(Date(file.lastModified()))
            }

            itemView.setOnClickListener { onFileClick(file) }
            itemView.setOnLongClickListener {
                onFileLongClick(file)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FileDiffCallback : DiffUtil.ItemCallback<File>() {
        override fun areItemsTheSame(oldItem: File, newItem: File): Boolean {
            return oldItem.absolutePath == newItem.absolutePath
        }

        override fun areContentsTheSame(oldItem: File, newItem: File): Boolean {
            return oldItem.absolutePath == newItem.absolutePath &&
                    oldItem.lastModified() == newItem.lastModified()
        }
    }
}
