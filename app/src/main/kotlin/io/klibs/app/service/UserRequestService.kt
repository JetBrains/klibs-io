package io.klibs.app.service

import io.klibs.core.pckg.dto.UserIndexingRequestDto

interface UserRequestService {
    fun processRequest(userIndexingRequestDto: UserIndexingRequestDto)
}
