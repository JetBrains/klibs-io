package io.klibs.core.project.blacklist

/**
 * Repository interface for managing blacklisted packages.
 */
interface BlacklistRepository {

    /**
     * Checks if a package with the given group ID and artifact ID is banned.
     *
     * @param groupId the group ID of the package to check
     * @param artifactId the artifact ID of the package to check
     * @return true if the package is banned, false otherwise
     */
    fun checkPackageBanned(groupId: String, artifactId: String): Boolean

    /**
     * Checks if a package exists in the database.
     * @param groupId the group ID of the package
     * @param artifactId the artifact ID of the package
     * @return true if the package exists, false otherwise
     */
    fun checkPackageExists(groupId: String, artifactId: String): Boolean
    
    /**
     * Adds a package to the banned packages list.
     * @param groupId the group ID of the package
     * @param artifactId the artifact ID of the package
     */
    fun addToBannedPackages(groupId: String, artifactId: String?, reason: String?)
    
    /**
     * Removes all banned packages from the packages table and related tables by groupId artifactId.
     * If artifactId == null, removes all by groupId.
     * @param groupId the group ID of the package
     * @param artifactId the artifact ID of the package
     */
    fun removeBannedPackages(groupId: String, artifactId: String?)

    /**
     * Removes all banned packages from the packages table and related tables.
     */
    fun removeBannedPackages()
}