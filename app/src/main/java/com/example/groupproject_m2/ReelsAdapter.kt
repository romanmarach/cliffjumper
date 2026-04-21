package com.example.groupproject_m2

import android.annotation.SuppressLint
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReelsAdapter(
    private val videoIds: List<String>,
    private val spotName: String,
    private val spotLocation: String
) : RecyclerView.Adapter<ReelsAdapter.ReelViewHolder>() {

    private val activeWebViews = SparseArray<WebView>()
    var currentPosition = 0

    inner class ReelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val webView: WebView = view.findViewById(R.id.webView)
        val tvSpotName: TextView = view.findViewById(R.id.tvSpotName)
        val tvSpotLocation: TextView = view.findViewById(R.id.tvSpotLocation)

        init {
            configureWebView(webView)
        }

        fun bind(videoId: String, position: Int) {
            tvSpotName.text = spotName
            tvSpotLocation.text = spotLocation
            activeWebViews.put(position, webView)
            if (position == currentPosition) {
                loadEmbed(webView, videoId)
            }
            // Off-screen pages stay blank until playAt() scrolls to them
        }

        fun blank() {
            webView.loadUrl("about:blank")
        }

        fun destroy() {
            webView.loadUrl("about:blank")
            webView.stopLoading()
            webView.destroy()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowContentAccess = true
            allowFileAccess = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            loadWithOverviewMode = true
            useWideViewPort = true
            userAgentString =
                "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        }
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
    }

    private fun embedUrl(videoId: String) =
        "https://www.youtube.com/watch?v=$videoId"

    private fun loadEmbed(webView: WebView, videoId: String) {
        webView.loadUrl(embedUrl(videoId))
    }

    fun playAt(position: Int) {
        val previous = currentPosition
        currentPosition = position
        if (previous != position) {
            activeWebViews[previous]?.loadUrl("about:blank")
        }
        val webView = activeWebViews[position]
        if (webView != null && position < videoIds.size) {
            loadEmbed(webView, videoIds[position])
        }
    }

    fun pauseAll() {
        for (i in 0 until activeWebViews.size()) {
            activeWebViews.valueAt(i).loadUrl("about:blank")
        }
    }

    fun destroyAll() {
        for (i in 0 until activeWebViews.size()) {
            activeWebViews.valueAt(i).apply {
                loadUrl("about:blank")
                stopLoading()
                destroy()
            }
        }
        activeWebViews.clear()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reel, parent, false)
        return ReelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReelViewHolder, position: Int) {
        holder.bind(videoIds[position], position)
    }

    override fun onViewRecycled(holder: ReelViewHolder) {
        super.onViewRecycled(holder)
        val pos = holder.bindingAdapterPosition
        if (pos != RecyclerView.NO_POSITION) {
            activeWebViews.remove(pos)
        }
        holder.blank()
    }

    override fun getItemCount() = videoIds.size
}
