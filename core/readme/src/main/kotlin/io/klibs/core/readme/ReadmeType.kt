package io.klibs.core.readme

enum class ReadmeType {
    RAW, // readme fetched from github or androidx documentation, without any processing
    HTML,
    MARKDOWN,
    MINIMIZED_MARKDOWN, // it is a markdown without links, examples, etc.
}
