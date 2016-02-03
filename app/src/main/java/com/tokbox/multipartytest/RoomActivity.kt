package com.tokbox.multipartytest

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import com.opentok.android.*
import kotlinx.android.synthetic.activity_room.*
import org.jetbrains.anko.find
import org.jetbrains.anko.toast
import java.util.*

private class SubscriberData (val containerView: RelativeLayout) {
    public var otSubscriber: Subscriber? = null
}

class RoomActivity : AppCompatActivity(), Session.SessionListener {
    private val TAG = RoomActivity::class.java.name

    private var otSession : Session?= null
    private var otPub : Publisher? = null
    private var subContainerArray : Array<SubscriberData>? = null

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

        OpenTokConfig.setAPIRootURL("https://anvil-tbdev.opentok.com", false);
        subContainerArray = arrayOf(
                SubscriberData(sub_container_1),
                SubscriberData(sub_container_2),
                SubscriberData(sub_container_3))

        val sd = (intent.extras.get("sessionData") as SessionData)!!
        otSession = Session(this, sd.apiKey, sd.sessionId)
        otSession!!.setSessionListener(this)
        otSession!!.connect(sd.token)
    }

    private fun findEmptySubscriberIndex() : Int? {
        var idx = 0
        for (s in subContainerArray!!) {
            if(s.otSubscriber == null) return idx
            idx++
        }
        return null
    }

    private fun findSubscriber(stream: Stream) : Int? {
        var idx = 0
        for (s in subContainerArray!!) {
            if (s.otSubscriber!!.stream.streamId == stream.streamId) return idx
            idx++
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

    }

    override fun onConnected(sess: Session?) {
        Log.d(TAG, "Session Connected")
        otPub = Publisher(this)
        otPub!!.publishAudio = false
        otPub!!.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL)

        otSession!!.publish(otPub!!)
        addVideoView(publisher_container, otPub!!.view)
    }

    override fun onStreamReceived(sess: Session?, stream: Stream?) {
        val idx = findEmptySubscriberIndex()
        if (idx != null) {
            val sub = Subscriber(this, stream)
            sub.subscribeToAudio = false
            sub.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                    BaseVideoRenderer.STYLE_VIDEO_FILL)
            otSession!!.subscribe(sub)

            subContainerArray!![idx].otSubscriber = sub
            addVideoView(subContainerArray!![idx].containerView, sub.view)
        } else {
            toast("Ignoring next peer, this sample only supports 3 subscribers")
        }
    }

    override fun onStreamDropped(sess: Session?, stream: Stream?) {
        val idx = findSubscriber(stream!!)
        if (idx != null) {
            val sd = subContainerArray!![idx]
            otSession!!.unsubscribe(sd.otSubscriber)
            removeVideoView(sd.containerView, sd.otSubscriber!!.view)

            sd.otSubscriber = null
        }
    }

    override fun onDisconnected(p0: Session?) {

    }
}
