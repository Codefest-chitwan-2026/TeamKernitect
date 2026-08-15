package com.kernitect.saharaandroid.mesh

class RelayManager {

    companion object {

        private const val MAX_SEEN_PACKETS =
            200
    }

    private val seenPacketIds =
        LinkedHashSet<String>()

    @Synchronized
    fun markSeen(
        packetId: String
    ): Boolean {

        if (
            seenPacketIds.contains(
                packetId
            )
        ) {

            return false
        }

        seenPacketIds.add(
            packetId
        )

        while (
            seenPacketIds.size >
            MAX_SEEN_PACKETS
        ) {

            val oldest =
                seenPacketIds
                    .firstOrNull()
                    ?: break

            seenPacketIds.remove(
                oldest
            )
        }

        return true
    }

    @Synchronized
    fun hasSeen(
        packetId: String
    ): Boolean {

        return seenPacketIds.contains(
            packetId
        )
    }

    @Synchronized
    fun clear() {

        seenPacketIds.clear()
    }
}