import com.google.cloud.tools.jib.api.*
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory
import org.jetbrains.amper.plugins.*
import java.nio.file.Path

@TaskAction
fun buildAndPush(
    @Input runtimeClasspath: Classpath,
    container: ContainerSettings,
    baseImage: BaseImageSettings,
    targetImage: TargetImageSettings,
) {
    jibContainerBuilder(runtimeClasspath, container, baseImage)
        .containerize(Containerizer.to(targetImage.toRegistryImages()))
}

@TaskAction
fun buildTar(
    @Input runtimeClasspath: Classpath,
    container: ContainerSettings,
    baseImage: BaseImageSettings,
    targetImage: TargetImageSettings,
    @Output outputTar: Path,
) {
    jibContainerBuilder(runtimeClasspath, container, baseImage)
        .containerize(Containerizer.to(TarImage.at(outputTar).named(targetImage.resolvedName)))
}

@TaskAction
fun buildToDockerDaemon(
    @Input runtimeClasspath: Classpath,
    container: ContainerSettings,
    baseImage: BaseImageSettings,
    targetImage: TargetImageSettings,
) {
    jibContainerBuilder(runtimeClasspath, container, baseImage)
        .containerize(Containerizer.to(DockerDaemonImage.named(ImageReference.parse(targetImage.resolvedName))))
}

private fun jibContainerBuilder(
    runtimeClasspath: Classpath,
    container: ContainerSettings,
    baseImage: BaseImageSettings,
): JibContainerBuilder = JavaContainerBuilder.from(baseImage.toRegistryImage())
    .addDependencies(runtimeClasspath.resolvedFiles)
    .addJvmFlags(container.jvmArgs)
    .setMainClass(container.mainClass)
    .toContainerBuilder()
    .apply {
        if (container.entryPoint != null) {
            setEntrypoint(container.entryPoint)
        }
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
        ?: error("Target image name is not set: configure `plugins.jib.targetImage.name` or pass it via the IMAGE environment variable")

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
