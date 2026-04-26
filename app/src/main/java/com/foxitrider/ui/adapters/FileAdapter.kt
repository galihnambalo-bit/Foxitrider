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
import com.foxitrider.utils.FileUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileAdapter(
    private val onFileClick: (File) -> Unit,
    private val onFileLongClick: (File) -> Unit
) : ListAdapter<File, FileAdapter.VH>(Diff()) {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.ivFileIcon)
        val name: TextView = v.findViewById(R.id.tvFileName)
        val info: TextView = v.findViewById(R.id.tvFileInfo)
        val date: TextView = v.findViewById(R.id.tvFileDate)
        fun bind(f: File) {
            name.text = f.name
            val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            date.text = fmt.format(Date(f.lastModified()))
            if (f.isDirectory) {
                icon.setImageResource(R.drawable.ic_folder)
                info.text = "${f.listFiles()?.size ?: 0} item"
            } else {
                icon.setImageResource(R.drawable.ic_pdf)
                info.text = FileUtils.formatSize(f.length())
            }
            itemView.setOnClickListener { onFileClick(f) }
            itemView.setOnLongClickListener { onFileLongClick(f); true }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class Diff : DiffUtil.ItemCallback<File>() {
        override fun areItemsTheSame(a: File, b: File) = a.absolutePath == b.absolutePath
        override fun areContentsTheSame(a: File, b: File) = a.absolutePath == b.absolutePath && a.lastModified() == b.lastModified()
    }
}
