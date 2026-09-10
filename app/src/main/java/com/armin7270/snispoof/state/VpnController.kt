package com.armin7270.snispoof.state

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.armin7270.snispoof.vpn.SpoofVpnService

object VpnController {
    fun start(context: Context) {
        val intent = Intent(context, SpoofVpnService::class.java)
            .setAction(SpoofVpnService.ACTION_CONNECT)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        val intent = Intent(context, SpoofVpnService::class.java)
            .setAction(SpoofVpnService.ACTION_DISCONNECT)
        context.startService(intent)
    }
}
