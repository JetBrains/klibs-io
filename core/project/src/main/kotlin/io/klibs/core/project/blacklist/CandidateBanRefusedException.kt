package io.klibs.core.project.blacklist

class CandidateBanRefusedException(groupId: String, artifactId: String) :
    RuntimeException("Ban of $groupId:$artifactId was refused")
