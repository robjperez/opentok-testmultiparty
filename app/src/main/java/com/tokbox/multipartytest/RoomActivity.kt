package com.tokbox.multipartytest

import android.Manifest
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.RelativeLayout
import com.opentok.android.*
import kotlinx.android.synthetic.main.activity_room.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug
import org.jetbrains.anko.toast

private class SubscriberData (val containerView: RelativeLayout) {
    var otSubscriber: Subscriber? = null
}

class RoomActivity : AppCompatActivity(), Session.SessionListener, AnkoLogger {
    private val TAG = RoomActivity::class.java.name
    private val PERMISSION_REQUEST_CODE = 123

    private var otSession : Session?= null
    private var otPub : Publisher? = null
    private var subContainerArray : Array<SubscriberData> = emptyArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room)
        fullscreen_content.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE
                .or(View.SYSTEM_UI_FLAG_FULLSCREEN)
                .or(View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
                .or(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                .or(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                .or(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
                .or(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        subContainerArray = arrayOf(
                SubscriberData(sub_container_1),
                SubscriberData(sub_container_2),
                SubscriberData(sub_container_3))

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions(this,
                    arrayOf(
                            Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
                    ),
                    PERMISSION_REQUEST_CODE
            )
        } else {
            createAndConnectSession()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE
                && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            createAndConnectSession()
        } else {
            toast("App does not have permission to record video and audio. Exiting")
            finish()
        }
    }

    private fun createAndConnectSession() {
        val sd = (intent.extras.get("sessionData") as SessionData)
        otSession = Session.Builder(this, sd.apiKey, sd.sessionId).sessionOptions(object: Session.SessionOptions() {
            override fun useTextureViews(): Boolean {
                return true
            }
        }).build()
        otSession?.setSessionListener(this)
        otSession?.connect(sd.token)

    }

    private fun findEmptySubscriberIndex() : Int? {
        subContainerArray.forEachIndexed { index, subscriberData ->
            if (subscriberData.otSubscriber == null) return index
        }
        return null
    }

    private fun findSubscriber(stream: Stream?) : Int? {
        stream?.let {
            val sub = subContainerArray.find {
                it.otSubscriber?.stream?.streamId == stream.streamId
            }
            val idx = subContainerArray.indexOf(sub)
            if (idx != -1) {
                return idx
            }
        }
        return null
    }

    private fun addVideoView(container: RelativeLayout, view: View) {
        val lp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT)
        container.addView(view, lp)
    }

    private fun removeVideoView(container: RelativeLayout, view: View) {
        container.removeView(view)
    }

    override fun onError(sess: Session?, error: OpentokError?) {
        error("Session connection error $error")
    }

    override fun onConnected(sess: Session?) {
        debug("Session Connected")
        otPub = Publisher.Builder(this).build()
        otPub?.let {
            it.publishAudio = false
            it.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                    BaseVideoRenderer.STYLE_VIDEO_FILL)
            otSession?.publish(it)
            addVideoView(publisher_container, it.view)
        }
    }

    override fun onStreamReceived(sess: Session?, stream: Stream?) {
        val idx = findEmptySubscriberIndex()
        idx?.let {
            val sub = Subscriber.Builder(this, stream).build()
            sub.subscribeToAudio = false
            sub.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                    BaseVideoRenderer.STYLE_VIDEO_FILL)
            otSession?.subscribe(sub)

            subContainerArray[it].otSubscriber = sub
            addVideoView(subContainerArray[it].containerView, sub.view)
            return
        }

        toast("Ignoring next peer, this sample only supports 3 subscribers")
    }

    override fun onStreamDropped(sess: Session?, stream: Stream?) {
        val idx = findSubscriber(stream)
        idx?.let {
            val sd = subContainerArray[it]
            sd.otSubscriber?.let {
                otSession?.unsubscribe(it)
                removeVideoView(sd.containerView, it.view)
            }
            sd.otSubscriber = null
        }
    }

    override fun onDisconnected(p0: Session?) {

    }
}
