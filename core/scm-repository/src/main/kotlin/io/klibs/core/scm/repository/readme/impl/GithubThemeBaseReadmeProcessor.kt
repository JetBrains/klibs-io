package io.klibs.core.scm.repository.readme.impl

import io.klibs.core.scm.repository.readme.ReadmeProcessor

/**
 * Images in GitHub readme sometimes have a #gh-dark-mode-only or #gh-light-mode-only suffix.
 * They indicate that the image should be visible in a light or dark mode only, and hidden otherwise.
 *
 * This processor removes all the images (with corresponding <a> markers if necessary)
 * in case there is a #gh-dark-mode-only suffix in the src. In case of the #gh-light-mode-only suffix,
 * the processor removes the suffix only.
 *
 * These suffixes are marked as deprecated by GitHub, but they are still used by some projects.
 */
abstract class GithubThemeBaseReadmeProcessor : ReadmeProcessor {

    private val imgDarkPattern = Regex("""<img\s+[^>]*src\s*=\s*["'][^"']+#gh-dark-mode-only["'][^>]*>""") // img marker with #gh-dark-mode-only at the end of the src link
    private val imgLightPattern = Regex("""(<img\s+[^>]*src\s*=\s*["'][^"']+)#gh-light-mode-only(["'][^>]*>)""") // img marker with #gh-light-mode-only at the end of the src link
    private val aImgDarkPattern = Regex("""<a\s+[^>]*href\s*=\s*["']([^"']+#gh-dark-mode-only)["'][^>]*>\s*<img\s+[^>]*src\s*=\s*["'][^"']+#gh-dark-mode-only["'][^>]*>\s*</a>""") // img marker with #gh-dark-mode-only inside an <a> marker
    private val aImgLightPattern = Regex("""(<a\s+[^>]*href\s*=\s*["'][^"']+)#gh-light-mode-only(["'][^>]*>\s*<img\s+[^>]*src\s*=\s*["'][^"']+)#gh-light-mode-only(["'][^>]*>\s*</a>)""") // img marker with #gh-light-mode-only inside an <a> marker

    override fun process(
        readmeContent: String,
        readmeOwner: String,
        readmeRepositoryName: String,
        repositoryDefaultBranch: String
    ): String {
        return readmeContent
            .replace(aImgDarkPattern, "") // remove the whole marker
            .replace(aImgLightPattern, "$1$2$3") // remove both occurrences of#gh-light-mode-only
            .replace(imgDarkPattern, "") // remove the whole marker
            .replace(imgLightPattern, "$1$2") // remove #gh-light-mode-only
    }

}