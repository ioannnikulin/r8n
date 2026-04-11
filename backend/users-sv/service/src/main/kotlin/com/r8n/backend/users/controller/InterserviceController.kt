package com.r8n.backend.users.controller

import com.r8n.backend.security.Authority
import com.r8n.backend.users.integration.api.UsersInternalApi
import com.r8n.backend.users.service.UserService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class InterserviceController(
    private val userService: UserService,
) : UsersInternalApi {
    @PreAuthorize(Authority.IS_SERVICE)
    override fun getUserName(id: UUID) = userService.getName(id)
}