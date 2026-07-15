package org.kotlintoolchain.plugin.gitcommitid

import org.jetbrains.amper.plugins.Configurable
import java.nio.file.Path

@Configurable
interface GitCommitIdSettings {

    /**
     * The path to the git repository, i.e. to the `.git` directory itself.
     * By default, the repository is discovered from the consumer module root. This also supports linked worktrees.
     */
    val gitDirectory: Path?

    /**
     * The build version written to `git.build.version`.
     */
    val buildVersion: String get() = "unspecified"

    /**
     * The name of the generated properties file.
     * By default, equals to "git.properties".
     */
    val propertiesFile: String get() = "git.properties"

    /**
     * The length of the abbreviated commit ID.
     * By default, equals to 7.
     */
    val abbrevLength: Int get() = 7

}
