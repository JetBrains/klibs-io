package io.klibs.app.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Action to perform on the package indexing request")
enum class ProcessPackageIndexAction {
    REMOVE,
    MARK_NON_KMP,
}