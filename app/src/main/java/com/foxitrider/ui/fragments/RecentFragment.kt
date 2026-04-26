package com.foxitrider.ui.fragments
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.foxitrider.databinding.FragmentRecentBinding
class RecentFragment : Fragment() {
    private var _b: FragmentRecentBinding? = null
    private val b get() = _b!!
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View { _b = FragmentRecentBinding.inflate(i,c,false); return b.root }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
