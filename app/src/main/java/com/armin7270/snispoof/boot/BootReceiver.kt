package com.armin7270.snispoof.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.armin7270.snispoof.state.PreferencesRepository
import com.armin7270.snispoof.state.VpnController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Optional auto-connect after device boot (the "Auto connect on boot"
 * setting, same behaviour as the reference app's rescue/quick reconnect).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val autoBoot = PreferencesRepository(context).current().autoStartBoot
                if (autoBoot) {
                    VpnController.start(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
