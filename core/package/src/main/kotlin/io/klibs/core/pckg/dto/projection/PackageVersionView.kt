package io.klibs.core.pckg.dto.projection

interface PackageVersionsView {
    val groupId: String
    val artifactId: String
    val versions: Set<String>
}

class Package(
    override val groupId: String,
    override val artifactId: String,
    override val versions: Set<String>
) : PackageVersionsView