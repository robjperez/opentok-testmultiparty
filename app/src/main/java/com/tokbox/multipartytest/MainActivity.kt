package com.tokbox.multipartytest

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.activity_main.*;
import org.jetbrains.anko.async
import org.jetbrains.anko.uiThread
import org.json.JSONObject
import java.net.URL

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button_join_room.setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                async() {
                    val sessData = getSessionData(input_room_name.text.toString())
                    uiThread {
                        val act = Intent(this@MainActivity, RoomActivity::class.java)
                        act.putExtra("sessionData", sessData)
                        startActivity(act)
                    }
                }
            }
        })
    }

    fun getSessionData(roomName: String): SessionData {
        val jsonText = URL("https://meet.tokbox.com/$roomName").readText()
        val jsonObj = JSONObject(jsonText)

        return SessionData(jsonObj.getString("sessionId"),
                jsonObj.getString("token"),
                jsonObj.getString("apiKey"))
    }
}
