package com.example.groupproject_m2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.groupproject_m2.databinding.FragmentReelsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ReelsFragment : Fragment() {

    private var _binding: FragmentReelsBinding? = null
    private val binding get() = _binding!!
    private var adapter: ReelsAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        _binding = FragmentReelsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated start")
        try {
            super.onViewCreated(view, savedInstanceState)

            val spotName = arguments?.getString(ARG_SPOT_NAME) ?: ""
            val spotLocation = arguments?.getString(ARG_SPOT_LOCATION) ?: ""
            Log.d(TAG, "Args — spotName='$spotName' spotLocation='$spotLocation'")

            val reelsAdapter = ReelsAdapter(
                lifecycleOwner = viewLifecycleOwner,
                videoIds = emptyList(),
                spotName = spotName,
                spotLocation = spotLocation
            )
            adapter = reelsAdapter
            Log.d(TAG, "Adapter created")

            binding.viewPager.adapter = reelsAdapter
            binding.viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
            binding.viewPager.offscreenPageLimit = 1
            Log.d(TAG, "ViewPager2 configured")

            binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    Log.d(TAG, "Page selected: $position")
                    adapter?.playAt(position)
                }
            })

            binding.btnBack.setOnClickListener { requireActivity().finish() }
            Log.d(TAG, "onViewCreated complete — starting video load")

            loadVideos(spotName, spotLocation)
        } catch (e: Exception) {
            Log.e(TAG, "Crash in onViewCreated", e)
        }
    }

    private fun loadVideos(spotName: String, spotLocation: String) {
        _binding?.loadingContainer?.visibility = View.VISIBLE
        _binding?.tvError?.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val apiKey = BuildConfig.YOUTUBE_API_KEY
                Log.d(TAG, "API key present: ${apiKey.isNotBlank()}")
                if (apiKey.isBlank()) {
                    showError("YouTube API key not configured.\nAdd YOUTUBE_API_KEY to local.properties.")
                    return@launch
                }

                val query = listOf(spotName, spotLocation)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                Log.d(TAG, "Fetching videos for query: '$query'")

                val videoIds = fetchYouTubeVideos(query, apiKey)
                Log.d(TAG, "Got ${videoIds.size} video IDs: $videoIds")

                _binding?.loadingContainer?.visibility = View.GONE
                if (videoIds.isEmpty()) {
                    showError("No videos found for this spot.\nTry another location.")
                } else {
                    val freshAdapter = ReelsAdapter(
                        lifecycleOwner = viewLifecycleOwner,
                        videoIds = videoIds,
                        spotName = spotName,
                        spotLocation = spotLocation,
                        onVideoError = { errorPosition ->
                            val next = errorPosition + 1
                            Log.d(TAG, "Video error at $errorPosition — advancing to $next")
                            if (next < (adapter?.itemCount ?: 0)) {
                                _binding?.viewPager?.setCurrentItem(next, true)
                            }
                        }
                    )
                    adapter = freshAdapter
                    _binding?.viewPager?.adapter = freshAdapter
                }
            } catch (e: Exception) {
                Log.e(TAG, "Crash in loadVideos coroutine", e)
                showError("Something went wrong loading videos.")
            }
        }
    }

    private fun showError(message: String) {
        _binding?.loadingContainer?.visibility = View.GONE
        _binding?.tvError?.text = message
        _binding?.tvError?.visibility = View.VISIBLE
    }

    private suspend fun fetchYouTubeVideos(query: String, apiKey: String): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = "https://www.googleapis.com/youtube/v3/search" +
                        "?part=id&type=video&videoEmbeddable=true" +
                        "&q=$encoded&maxResults=20&key=$apiKey"
                Log.d(TAG, "API URL: $url")
                val connection = URL(url).openConnection() as HttpURLConnection
                val response = try {
                    connection.connectTimeout = 8_000
                    connection.readTimeout = 8_000
                    connection.requestMethod = "GET"
                    connection.connect()
                    val code = connection.responseCode
                    Log.d(TAG, "HTTP status: $code")
                    if (code == HttpURLConnection.HTTP_OK) {
                        connection.inputStream.bufferedReader().readText()
                    } else {
                        connection.errorStream?.bufferedReader()?.readText().also {
                            Log.e(TAG, "HTTP error $code — body: $it")
                        } ?: ""
                    }
                } finally {
                    connection.disconnect()
                }
                Log.d(TAG, "YouTube API response: $response")
                val json = JSONObject(response)
                val items = json.getJSONArray("items")
                (0 until items.length()).mapNotNull { i ->
                    items.getJSONObject(i)
                        .optJSONObject("id")
                        ?.optString("videoId")
                        ?.takeIf { it.isNotBlank() }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in fetchYouTubeVideos: ${e::class.java.simpleName}: ${e.message}", e)
                emptyList()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        adapter?.pauseAll()
    }

    override fun onResume() {
        super.onResume()
        val currentItem = _binding?.viewPager?.currentItem ?: return
        adapter?.playAt(currentItem)
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView")
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ReelsFragment"
        private const val ARG_SPOT_NAME = "spot_name"
        private const val ARG_SPOT_LOCATION = "spot_location"

        fun newInstance(spotName: String, spotLocation: String) = ReelsFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_SPOT_NAME, spotName)
                putString(ARG_SPOT_LOCATION, spotLocation)
            }
        }
    }
}
