package com.kernitect.sahararesponder.model

enum class RescueLifecycle(val rank: Int, val displayName: String) {
    NEW(0, "New"), ACCEPTED(1, "Accepted"), ON_THE_WAY(2, "On the Way"),
    NEARBY(3, "Nearby"), ARRIVED(4, "Arrived"), RESCUED(5, "Rescued");

    fun next(): RescueLifecycle? = entries.firstOrNull { it.rank == rank + 1 }

    companion object {
        fun parse(value: String): RescueLifecycle? = entries.firstOrNull { it.name == value }
        fun progresses(current: String, candidate: String): Boolean {
            val from = parse(current) ?: return false
            val to = parse(candidate) ?: return false
            return to.rank > from.rank
        }
    }
}

fun canControlLifecycle(ownership: IncidentOwnership): Boolean =
    ownership == IncidentOwnership.CLAIMED_BY_ME || ownership == IncidentOwnership.CONFLICT

data class RescueLifecycleEvent(
    val status: RescueLifecycle,
    val timestamp: Long,
    val teamId: String,
    val teamName: String,
    val callsign: String,
    val sourcePacketId: String,
)

fun recordLifecycleEvent(
    events: List<RescueLifecycleEvent>,
    event: RescueLifecycleEvent,
): List<RescueLifecycleEvent> {
    if (event.timestamp <= 0L || events.any { it.teamId == event.teamId && it.status == event.status }) return events
    val latestForTeam = events.filter { it.teamId == event.teamId }.maxByOrNull { it.status.rank }
    if (latestForTeam != null && event.status.rank <= latestForTeam.status.rank) return events
    return (events + event).sortedWith(compareBy<RescueLifecycleEvent> { it.timestamp }.thenBy { it.status.rank })
}
