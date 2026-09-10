package com.armin7270.snispoof.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.armin7270.snispoof.state.ConnectionState
import com.armin7270.snispoof.state.VpnController
import com.armin7270.snispoof.state.VpnStateStore

/** Quick Settings tile: one tap connect/disconnect (UAC-style control). */
class SpoofTile : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        when (VpnStateStore.state.value) {
            ConnectionState.DISCONNECTED, ConnectionState.ERROR ->
                VpnController.start(applicationContext)
            ConnectionState.CONNECTED ->
                VpnController.stop(applicationContext)
            ConnectionState.CONNECTING, ConnectionState.DISCONNECTING -> Unit
        }
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val state = VpnStateStore.state.value
        tile.state = when (state) {
            ConnectionState.CONNECTED -> Tile.STATE_ACTIVE
            ConnectionState.CONNECTING, ConnectionState.DISCONNECTING -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.updateTile()
    }
}
