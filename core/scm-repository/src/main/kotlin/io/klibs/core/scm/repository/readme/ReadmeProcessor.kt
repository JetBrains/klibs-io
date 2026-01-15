package io.klibs.core.scm.repository.readme

/**
 * This is the main interface for all beans, that are connected with readme post-processing.
 * It is expected that each readme goes through the chain of these implementations,
 * where each processor is checked to be applied and if yes applies.
 */
interface ReadmeProcessor {

    /**
     * Processes the provided README content according to a series of processing steps.
     * The processing steps may include formatting, placeholders substitution, etc.
     *
     * @param readmeContent the content of the README to be processed
     * @param readmeOwner the owner of the repository containing the README
     * @param readmeRepositoryName the name of the repository containing the README
     * @param repositoryDefaultBranch the default branch of the repository
     * @return the processed README content
     */
    fun process(
        readmeContent: String,
        readmeOwner: String,
        readmeRepositoryName: String,
        repositoryDefaultBranch: String
    ): String;


    /**
     * Checks if the processor is applicable for the given type of README.
     *
     * @param type the type of README
     * @return true if the processor can handle the specified README type, false otherwise
     */
    fun isApplicable(type: ReadmeType): Boolean;
}