package io.klibs.app.api

import io.klibs.app.dto.ProcessPackageIndexResultDTO

data class ProcessPackageIndexRequestsResponse(
    val totalProcessed: Int,
    val results: List<ProcessPackageIndexResultDTO>,
)