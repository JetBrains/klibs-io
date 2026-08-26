package io.klibs.app.service

interface BlacklistService {

    fun banByGroup(groupId: String, reason: String?): Boolean

    fun banPackage(groupId: String, artifactId: String, reason: String?): Boolean

}
