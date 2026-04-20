package com.example.groupproject_m2

import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

class ReelsAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private var videoIds: List<String>,
    private val spotName: String,
    private val spotLocation: String,
    private val onVideoError: (position: Int) -> Unit = {}
) : RecyclerView.Adapter<ReelsAdapter.ReelViewHolder>() {

    // Tracks the YouTubePlayer for each currently-bound position
    private val activePlayers = SparseArray<YouTubePlayer>()
    var currentPosition = 0

    inner class ReelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val playerView: YouTubePlayerView = view.findViewById(R.id.youtubePlayerView)
        val loadingIndicator: ProgressBar = view.findViewById(R.id.itemLoadingIndicator)
        val tvSpotName: TextView = view.findViewById(R.id.tvSpotName)
        val tvSpotLocation: TextView = view.findViewById(R.id.tvSpotLocation)

        // Set once in onCreateViewHolder via onReady; null until the WebView is ready
        var youTubePlayer: YouTubePlayer? = null

        // If bind() is called before onReady fires, we park the request here
        var pendingVideoId: String? = null

        fun bind(videoId: String, position: Int) {
            tvSpotName.text = spotName
            tvSpotLocation.text = spotLocation

            val player = youTubePlayer
            if (player != null) {
                // Player already ready — load/cue immediately
                activePlayers.put(position, player)
                if (position == currentPosition) {
                    player.loadVideo(videoId, 0f)
                } else {
                    player.cueVideo(videoId, 0f)
                }
                pendingVideoId = null
            } else {
                // onReady hasn't fired yet — park until it does
                loadingIndicator.visibility = View.VISIBLE
                pendingVideoId = videoId
            }
        }
    }

    // initialize() is called here — exactly once per ViewHolder instance
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reel, parent, false)
        val holder = ReelViewHolder(view)

        lifecycleOwner.lifecycle.addObserver(holder.playerView)

        holder.playerView.enableAutomaticInitialization = false
        try {
            holder.playerView.initialize(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                holder.youTubePlayer = youTubePlayer
                holder.loadingIndicator.visibility = View.GONE

                // Deliver any video request that arrived before we were ready
                val videoId = holder.pendingVideoId ?: return
                val position = holder.bindingAdapterPosition
                    .takeIf { it != RecyclerView.NO_POSITION } ?: return

                holder.pendingVideoId = null
                activePlayers.put(position, youTubePlayer)
                if (position == currentPosition) {
                    youTubePlayer.loadVideo(videoId, 0f)
                } else {
                    youTubePlayer.cueVideo(videoId, 0f)
                }
            }

            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                val position = holder.bindingAdapterPosition
                    .takeIf { it != RecyclerView.NO_POSITION } ?: return
                Log.w(TAG, "Player error at position $position: $error")
                onVideoError(position)
            }
        }) } catch (e: Exception) {
            Log.e(TAG, "initialize() failed — already initialized? $e")
        }

        return holder
    }

    override fun onBindViewHolder(holder: ReelViewHolder, position: Int) {
        holder.bind(videoIds[position], position)
    }

    // Pause and clear tracking when a ViewHolder scrolls off screen.
    // Do NOT release the player — the ViewHolder and its initialized player
    // will be reused via onBindViewHolder, not onCreateViewHolder.
    override fun onViewRecycled(holder: ReelViewHolder) {
        super.onViewRecycled(holder)
        val pos = holder.bindingAdapterPosition
        if (pos != RecyclerView.NO_POSITION) {
            activePlayers.remove(pos)
        }
        holder.youTubePlayer?.pause()
        holder.pendingVideoId = null
    }

    fun playAt(position: Int) {
        currentPosition = position
        for (i in 0 until activePlayers.size()) {
            val pos = activePlayers.keyAt(i)
            val player = activePlayers.valueAt(i)
            if (pos == position) player.play() else player.pause()
        }
    }

    fun pauseAll() {
        for (i in 0 until activePlayers.size()) {
            activePlayers.valueAt(i).pause()
        }
    }

    fun getVideoIds(): List<String> = videoIds

    override fun getItemCount() = videoIds.size

    companion object {
        private const val TAG = "ReelsAdapter"
    }
}
