package com.muswall.lite

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

class MusicNotificationListener : NotificationListenerService() {

    private val listenerJob = Job()
    private val scope = CoroutineScope(Dispatchers.Default + listenerJob)
    private var extractJob: Job? = null

    private var sessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null

    private var lastPackageName: String? = null
    private var lastTrackTitle: String? = null
    private var lastArtist: String? = null
    private var lastArtUri: String? = null
    private var lastExtractedArt: Bitmap? = null
    private var lastPlayingState: Boolean = false

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateFromController(activeController, metadataChanged = true)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateFromController(activeController, metadataChanged = false)
        }
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        findAndAttachController(controllers)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        WallpaperState.init(applicationContext)
        try {
            sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val component = ComponentName(this, MusicNotificationListener::class.java)
            sessionManager?.addOnActiveSessionsChangedListener(sessionsListener, component)
            val controllers = sessionManager?.getActiveSessions(component)
            findAndAttachController(controllers)
        } catch (_: SecurityException) {
        }
    }

    override fun onListenerDisconnected() {
        extractJob?.cancel()
        activeController?.unregisterCallback(callback)
        sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener)
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        extractJob?.cancel()
        listenerJob.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        checkSessions()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        checkSessions()
    }

    private fun checkSessions() {
        try {
            val component = ComponentName(this, MusicNotificationListener::class.java)
            val controllers = sessionManager?.getActiveSessions(component)
            findAndAttachController(controllers)
        } catch (_: Exception) {
        }
    }

    private fun findAndAttachController(controllers: List<MediaController>?) {
        if (controllers.isNullOrEmpty()) {
            return
        }

        val playingController = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers.firstOrNull {
            it.metadata != null
        } ?: controllers.first()

        val changed = (activeController?.sessionToken != playingController.sessionToken)
        if (changed) {
            activeController?.unregisterCallback(callback)
            activeController = playingController
            activeController?.registerCallback(callback)
            updateFromController(activeController, metadataChanged = true)
        } else {
            updateFromController(activeController, metadataChanged = false)
        }
    }

    private fun updateFromController(controller: MediaController?, metadataChanged: Boolean) {
        if (controller == null) return

        val metadata = controller.metadata
        val state = controller.playbackState
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
        val artUriStr = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ART_URI)
        val pkg = controller.packageName

        val isSameTrack = (pkg == lastPackageName && title == lastTrackTitle && artist == lastArtist && artUriStr == lastArtUri)
        val isSamePlaying = (isPlaying == lastPlayingState)

        if (!metadataChanged && isSameTrack && isSamePlaying) {
            return
        }

        if (isSameTrack && lastExtractedArt != null) {
            lastPlayingState = isPlaying
            WallpaperState.updateMedia(applicationContext, lastExtractedArt, isPlaying, title)
            return
        }

        extractJob?.cancel()
        if (!isSamePlaying && isPlaying != lastPlayingState) {
            lastPlayingState = isPlaying
            WallpaperState.updateMedia(applicationContext, lastExtractedArt, isPlaying, title)
        }

        extractJob = scope.launch {
            delay(120L)
            processTrackArtwork(controller, isPlaying, title, artist, artUriStr, pkg)
        }
    }

    private fun processTrackArtwork(
        controller: MediaController,
        isPlaying: Boolean,
        title: String?,
        artist: String?,
        artUriStr: String?,
        pkg: String
    ) {
        val metadata = controller.metadata
        var bitmap: Bitmap? = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        if (bitmap == null) {
            bitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        }

        if (bitmap == null && artUriStr != null) {
            try {
                val uri = Uri.parse(artUriStr)
                if (uri.scheme == "http" || uri.scheme == "https") {
                    val connection = URL(artUriStr).openConnection() as HttpURLConnection
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    connection.doInput = true
                    connection.connect()
                    connection.inputStream.use { stream ->
                        bitmap = BitmapFactory.decodeStream(stream)
                    }
                } else {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        bitmap = BitmapFactory.decodeStream(stream)
                    }
                }
            } catch (_: Exception) {
            }
        }

        if (bitmap == null) {
            bitmap = extractArtFromNotifications(controller.packageName)
        }

        val optimized = optimizeBitmapSize(bitmap)

        if (optimized != null && lastExtractedArt != null && optimized.sameAs(lastExtractedArt)) {
            bitmap = lastExtractedArt
        } else if (optimized != null) {
            lastExtractedArt = optimized
        }

        lastPackageName = pkg
        lastTrackTitle = title
        lastArtist = artist
        lastArtUri = artUriStr
        lastPlayingState = isPlaying

        WallpaperState.updateMedia(applicationContext, lastExtractedArt, isPlaying, title)
    }

    private fun optimizeBitmapSize(bitmap: Bitmap?): Bitmap? {
        if (bitmap == null || bitmap.isRecycled) return null
        val maxDim = 1080
        if (bitmap.width <= maxDim && bitmap.height <= maxDim) return bitmap

        val scale = maxDim.toFloat() / max(bitmap.width, bitmap.height)
        val targetW = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val targetH = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        if (scaled !== bitmap) {
            bitmap.recycle()
        }
        return scaled
    }

    private fun extractArtFromNotifications(targetPkg: String): Bitmap? {
        try {
            val activeNotifications = getActiveNotifications() ?: return null
            for (sbn in activeNotifications) {
                if (sbn.packageName == targetPkg) {
                    val extras = sbn.notification.extras ?: continue
                    val art = extractArtFromExtras(extras)
                    if (art != null) return art
                }
            }
            for (sbn in activeNotifications) {
                if (sbn.notification.isMediaNotification()) {
                    val extras = sbn.notification.extras ?: continue
                    val art = extractArtFromExtras(extras)
                    if (art != null) return art
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun extractArtFromExtras(extras: Bundle): Bitmap? {
        val picture = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(Notification.EXTRA_PICTURE, Bitmap::class.java)
        } else {
            extras.getParcelable(Notification.EXTRA_PICTURE)
        }
        if (picture != null) return picture

        val largeIconObj = extras.get(Notification.EXTRA_LARGE_ICON)
        if (largeIconObj is Bitmap) {
            return largeIconObj
        } else if (largeIconObj is Icon) {
            val drawable = largeIconObj.loadDrawable(this)
            if (drawable is BitmapDrawable) {
                return drawable.bitmap
            } else if (drawable != null) {
                val w = drawable.intrinsicWidth.coerceAtLeast(1)
                val h = drawable.intrinsicHeight.coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, w, h)
                drawable.draw(canvas)
                return bmp
            }
        }
        return null
    }

    private fun Notification.isMediaNotification(): Boolean {
        val template = extras?.getString(Notification.EXTRA_TEMPLATE) ?: ""
        return template.contains("MediaStyle")
    }
}
