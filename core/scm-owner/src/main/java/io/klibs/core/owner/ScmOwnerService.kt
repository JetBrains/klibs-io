package io.klibs.core.owner

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ScmOwnerService(
    private val scmOwnerRepository: ScmOwnerRepository,
) {
    @Transactional(readOnly = true)
    fun getOwner(login: String): ScmOwner? {
        return scmOwnerRepository.findByLogin(login)?.toModel()
    }
}

private fun ScmOwnerEntity.toModel(): ScmOwner {
    return when (this.type) {
        ScmOwnerType.ORGANIZATION -> this.toOrganization()
        ScmOwnerType.AUTHOR -> this.toAuthor()
    }
}

private fun ScmOwnerEntity.toAuthor(): ScmOwnerAuthor {
    return ScmOwnerAuthor(
        id = this.idNotNull,
        login = this.login,
        avatarUrl = getAvatarUrl(),
        name = this.name,
        description = this.description,
        location = this.location,
        followers = this.followers,
        company = this.company,
        homepage = this.homepage,
        twitterHandle = this.twitterHandle,
        email = this.email
    )
}

private fun ScmOwnerEntity.toOrganization(): ScmOwnerOrganization {
    return ScmOwnerOrganization(
        id = this.idNotNull,
        login = this.login,
        avatarUrl = getAvatarUrl(),
        name = this.name,
        description = this.description,
        homepage = this.homepage,
        twitterHandle = this.twitterHandle,
        email = this.email
    )
}
