package com.tokbox.multipartytest

import java.io.Serializable

/**
 * Created by rpc on 03/02/16.
 */
class SessionData(val sessionId: String,
                  val token: String,
                  val apiKey: String) : Serializable
{
}