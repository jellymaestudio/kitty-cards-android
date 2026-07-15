package kittycards.kittycardsandroid.logic

import kittycards.kittycardsandroid.network.NetworkDevice

/**
 * Represents the current state of the host lobby.
 *
 * @property availableGuests guests that can still be selected
 * @property selectedGuest guest accepted for the match, or null
 * @property matchStartRequested whether the match start is currently pending
 *
 * @author JellyMae
 */
data class HostLobbyState(
    val availableGuests: List<NetworkDevice> = emptyList(),
    val selectedGuest: NetworkDevice? = null,
    val matchStartRequested: Boolean = false
) {

    /**
     * Returns whether the match can currently be started.
     */
    val canStartMatch: Boolean
        get() =
            selectedGuest != null &&
                    !matchStartRequested
}