package org.amper.gitcommitid

import org.jetbrains.amper.plugins.Configurable
import java.nio.file.Path

@Configurable
interface GitCommitIdSettings {

    // TODO ability to specify the worktree directory
    // currently it is not possible because usually the worktree is a direct parent of the `gitDirectory`,
    // and the plugin system does not allow intersecting Inputs.

    /**
     * The path to the git repository, i.e. to the `.git` directory itself.
     * By default, `<PROJECT_ROOT>/.git` is used.
     */
    val gitDirectory: Path?

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
