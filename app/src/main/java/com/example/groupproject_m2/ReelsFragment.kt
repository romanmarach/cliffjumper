package com.example.groupproject_m2

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.groupproject_m2.databinding.FragmentReelsBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

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
                if (apiKey.isBlank()) {
                    showError("YouTube API key not configured.\nAdd YOUTUBE_API_KEY to local.properties.")
                    return@launch
                }

                // If launched from Explore tab (no spot), resolve device location
                val (query, displayName, displayLocation) =
                    if (spotName.isBlank() && spotLocation.isBlank()) {
                        resolveLocationQuery()
                    } else {
                        val q = listOf(spotName, spotLocation)
                            .filter { it.isNotBlank() }.joinToString(" ")
                        Triple(q, spotName, spotLocation)
                    }
                Log.d(TAG, "Fetching videos for query: '$query'")

                val videoIds = fetchYouTubeVideos(query, apiKey)
                Log.d(TAG, "Got ${videoIds.size} video IDs: $videoIds")

                _binding?.loadingContainer?.visibility = View.GONE
                if (videoIds.isEmpty()) {
                    showError("No videos found.\nTry another location.")
                } else {
                    val freshAdapter = ReelsAdapter(
                        videoIds = videoIds,
                        spotName = displayName,
                        spotLocation = displayLocation
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

    /** Returns Triple(searchQuery, overlayTitle, overlaySubtitle) based on device GPS. */
    @Suppress("DEPRECATION")
    private suspend fun resolveLocationQuery(): Triple<String, String, String> =
        withContext(Dispatchers.IO) {
            try {
                val ctx = context ?: return@withContext fallbackQuery()
                val hasPermission = ContextCompat.checkSelfPermission(
                    ctx, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasPermission) return@withContext fallbackQuery()

                val client = LocationServices.getFusedLocationProviderClient(ctx)
                val location: Location? = Tasks.await(client.lastLocation, 5, TimeUnit.SECONDS)
                if (location == null) return@withContext fallbackQuery()

                val geocoder = Geocoder(ctx, Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val city = addresses?.firstOrNull()?.locality
                    ?: addresses?.firstOrNull()?.adminArea

                if (city != null) {
                    Log.d(TAG, "Device location: $city")
                    Triple("cliff jumping $city", "Cliff Jumping", city)
                } else {
                    fallbackQuery()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Location resolve failed: $e")
                fallbackQuery()
            }
        }

    private fun fallbackQuery() = Triple("cliff jumping water", "Cliff Jumping", "Near You")

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
        adapter?.destroyAll()
        _binding?.viewPager?.adapter = null
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
