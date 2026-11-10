package com.muswall.lite

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MusicNotificationListener : NotificationListenerService() {

    private var sessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateFromController(activeController)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateFromController(activeController)
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
        activeController?.unregisterCallback(callback)
        sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener)
        super.onListenerDisconnected()
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

        if (activeController?.sessionToken != playingController.sessionToken) {
            activeController?.unregisterCallback(callback)
            activeController = playingController
            activeController?.registerCallback(callback)
        }

        updateFromController(activeController)
    }

    private fun updateFromController(controller: MediaController?) {
        if (controller == null) return

        val metadata = controller.metadata
        val state = controller.playbackState
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING

        var bitmap: Bitmap? = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        if (bitmap == null) {
            bitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        }

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        WallpaperState.updateMedia(applicationContext, bitmap, isPlaying, title)
    }
}
