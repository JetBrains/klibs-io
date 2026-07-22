package org.kotlintoolchain.plugin.jib

import com.google.cloud.tools.jib.api.*
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory
import org.jetbrains.amper.plugins.*
import java.nio.file.Path

@TaskAction
fun buildAndPush(
    @Input runtimeClasspath: Classpath,
    projectRootDir: String,
    container: ContainerSettings,
    baseImage: BaseImageSettings,
    targetImage: TargetImageSettings,
) {
    val containerizer = Containerizer.to(targetImage.toRegistryImages())
        .withAdditionalTags(targetImage.tags)
    jibContainerBuilder(runtimeClasspath, projectRootDir, container, baseImage)
        .containerize(containerizer)
}

@TaskAction
fun buildTar(
    @Input runtimeClasspath: Classpath,
    projectRootDir: String,
    container: ContainerSettings,
    baseImage: BaseImageSettings,
    targetImage: TargetImageSettings,
    @Output outputTar: Path,
) {
    val containerizer = Containerizer.to(
        TarImage.at(outputTar).named(targetImage.resolvedName),
    ).withAdditionalTags(targetImage.tags)
    jibContainerBuilder(runtimeClasspath, projectRootDir, container, baseImage)
        .containerize(containerizer)
}

@TaskAction
fun buildToDockerDaemon(
    @Input runtimeClasspath: Classpath,
    projectRootDir: String,
    container: ContainerSettings,
    baseImage: BaseImageSettings,
    targetImage: TargetImageSettings,
) {
    val dockerImage = DockerDaemonImage.named(ImageReference.parse(targetImage.resolvedName))
    val containerizer = Containerizer.to(dockerImage)
        .withAdditionalTags(targetImage.tags)
    jibContainerBuilder(runtimeClasspath, projectRootDir, container, baseImage)
        .containerize(containerizer)
}

private fun jibContainerBuilder(
    runtimeClasspath: Classpath,
    projectRootDir: String,
    container: ContainerSettings,
    baseImage: BaseImageSettings,
): JibContainerBuilder {
    val classpath = partitionRuntimeClasspath(runtimeClasspath.resolvedFiles, Path.of(projectRootDir))
    return JavaContainerBuilder.from(baseImage.toRegistryImage())
        .addDependencies(classpath.externalDependencies)
        .addProjectDependencies(classpath.projectDependencies)
        .addJvmFlags(container.jvmArgs)
        .setMainClass(container.mainClass)
        .toContainerBuilder()
        .apply {
            if (container.entryPoint != null) {
                setEntrypoint(container.entryPoint)
            }
        }
}

internal data class PartitionedRuntimeClasspath(
    val externalDependencies: List<Path>,
    val projectDependencies: List<Path>,
)

internal fun partitionRuntimeClasspath(
    resolvedFiles: List<Path>,
    projectRootDir: Path,
): PartitionedRuntimeClasspath {
    val normalizedProjectRoot = projectRootDir.toAbsolutePath().normalize()
    val (projectDependencies, externalDependencies) = resolvedFiles.partition { file ->
        file.toAbsolutePath().normalize().startsWith(normalizedProjectRoot)
    }
    return PartitionedRuntimeClasspath(
        externalDependencies = externalDependencies,
        projectDependencies = projectDependencies,
    )
}

internal fun Containerizer.withAdditionalTags(tags: List<String>): Containerizer = apply {
    tags.forEach { withAdditionalTag(it) }
}

private fun BaseImageSettings.toRegistryImage(): RegistryImage {
    val imageReference = ImageReference.parse(fullName)
    val registryImage = RegistryImage.named(imageReference)
    registryImage.configureCredentials(imageReference, credHelper, auth)
    return registryImage
}

private fun TargetImageSettings.toRegistryImages(): RegistryImage {
    val imageReference = ImageReference.parse(resolvedName)
    val registryImage = RegistryImage.named(imageReference)
    registryImage.configureCredentials(imageReference, credHelper, auth)
    return registryImage
}

private val TargetImageSettings.resolvedName: String
    get() = name
        ?: System.getenv("IMAGE")
        ?: error(
            "Target image name is not set: configure `plugins.jib.targetImage.name` " +
                "or pass it via the IMAGE environment variable",
        )

private fun RegistryImage.configureCredentials(
    imageReference: ImageReference,
    credHelper: String?,
    auth: Credentials?,
) {
    val credentialRetrieverFactory = CredentialRetrieverFactory.forImage(imageReference) { logEvent ->
        println("${logEvent.level} ${logEvent.message}")
    }
    addCredentialRetriever(credentialRetrieverFactory.dockerConfig())
    addCredentialRetriever(credentialRetrieverFactory.wellKnownCredentialHelpers())
    if (credHelper != null) {
        addCredentialRetriever(credentialRetrieverFactory.dockerCredentialHelper(credHelper))
    }
    if (auth != null) {
        val basicAuth = credentialRetrieverFactory.known(Credential.from(auth.username, auth.password), "basic auth")
        addCredentialRetriever(basicAuth)
    }
}
