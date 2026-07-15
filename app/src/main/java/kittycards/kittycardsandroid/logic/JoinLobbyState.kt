package kittycards.kittycardsandroid.logic

import kittycards.kittycardsandroid.network.NetworkDevice

/**
 * Represents the current state of the join lobby.
 *
 * @property availableRooms rooms currently visible to the guest
 * @property selectedRoom room to which a connection is currently being established
 * @property acceptedRoom room accepted by the host
 *
 * @author JellyMae
 */
data class JoinLobbyState(
    val availableRooms: List<NetworkDevice> = emptyList(),
    val selectedRoom: NetworkDevice? = null,
    val acceptedRoom: NetworkDevice? = null
) {

    /**
     * Returns whether the guest is currently waiting for host acceptance.
     */
    val isWaitingForAcceptance: Boolean
        get() =
            selectedRoom != null &&
                    acceptedRoom == null
}