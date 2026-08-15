package com.kernitect.sahararesponder.model

interface MeshOutgoingPacket {
    val id: String
    val incidentId: String
    val type: String
    fun toJson(): String
}
