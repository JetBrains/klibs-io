package io.klibs.core.scm.repository.readme.impl

import io.klibs.core.scm.repository.readme.ReadmeProcessor
import io.klibs.core.scm.repository.readme.ReadmeType
import org.intellij.markdown.MarkdownElementTypes.CODE_BLOCK
import org.intellij.markdown.MarkdownElementTypes.CODE_FENCE
import org.intellij.markdown.MarkdownElementTypes.IMAGE
import org.intellij.markdown.MarkdownElementTypes.INLINE_LINK
import org.intellij.markdown.MarkdownElementTypes.LINK_DESTINATION
import org.intellij.markdown.MarkdownElementTypes.LINK_TEXT
import org.intellij.markdown.MarkdownTokenTypes.Companion.TEXT
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import org.springframework.stereotype.Service

private const val SIZE_OF_MINIMIZED_README = 1000

@Service
class ReadmeMinimizationProcessor : ReadmeProcessor {

    override fun process(
        readmeContent: String,
        readmeOwner: String,
        readmeRepositoryName: String,
        repositoryDefaultBranch: String
    ): String {
        val parsedTree =
            MarkdownParser(GFMFlavourDescriptor())
                .buildMarkdownTreeFromString(readmeContent)

        val plainTextBuilder = StringBuilder()
        extractPlainText(parsedTree, plainTextBuilder, readmeContent)
        return plainTextBuilder.toString()
            .replace(Regex("\\s+"), " ") // Replace multiple spaces with a single space
            .trim()
            .take(SIZE_OF_MINIMIZED_README)
    }

    override fun isApplicable(type: ReadmeType): Boolean {
        return ReadmeType.MINIMIZED_MARKDOWN == type
    }

    private fun extractPlainText(node: ASTNode, plainText: StringBuilder, content: String) {

        when {

            TEXT == node.type && LINK_DESTINATION != node.parent?.type ->  {
                plainText.append(content.substring(node.startOffset, node.endOffset))
                plainText.append(" ")
            }

            node.type in setOf(CODE_BLOCK, CODE_FENCE, IMAGE) -> {
                return
            }

            else -> {
                for (child in node.children) {
                    extractPlainText(child, plainText, content)
                }
            }
        }
    }
}
