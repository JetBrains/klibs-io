package io.klibs.app.service

class CandidateBanRefusedException(groupId: String, artifactId: String) :
    RuntimeException("Ban of $groupId:$artifactId was refused")
