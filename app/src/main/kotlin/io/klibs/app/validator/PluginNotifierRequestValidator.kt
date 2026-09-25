package io.klibs.app.validator

import io.klibs.app.api.PluginNotifierIndexingRequest
import io.klibs.app.dto.IndexingRequestValidationResult
import org.springframework.stereotype.Component

@Component
class PluginNotifierRequestValidator() {
    fun validatePluginNotifierIndexingRequest(
        payload: PluginNotifierIndexingRequest
    ): IndexingRequestValidationResult<PluginNotifierIndexingRequest>{
        // TODO KTL-2853 Implement a proper validator after introducing authentication (similar to webhooks)
        return IndexingRequestValidationResult.Valid(payload)
    }
}
