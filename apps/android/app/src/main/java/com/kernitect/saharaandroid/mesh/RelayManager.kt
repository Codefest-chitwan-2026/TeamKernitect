package com.kernitect.saharaandroid.mesh

class RelayManager(
    private val maxRememberedPackets: Int = 200
) {
    private val seenPacketIds = LinkedHashSet<String>()

    @Synchronized
    fun markSeen(
        packetId: String
    ): Boolean {
        if (seenPacketIds.contains(packetId)) {
            return false
        }

        seenPacketIds.add(packetId)

        if (
            seenPacketIds.size > maxRememberedPackets
        ) {
            val oldest = seenPacketIds.firstOrNull()

            if (oldest != null) {
                seenPacketIds.remove(oldest)
            }
        }

        return true
    }

    @Synchronized
    fun hasSeen(
        packedId: String
    ): Boolean {

        return seenPacketIds.contains(
            packedId
        )
    }

    @Synchronized
    fun clear() {
        seenPacketIds.clear()
    }
}