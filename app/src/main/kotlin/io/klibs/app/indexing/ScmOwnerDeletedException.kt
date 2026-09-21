package io.klibs.app.indexing

class ScmOwnerDeletedException(login: String, nativeId: Long) :
    RuntimeException("GitHub owner login=$login nativeId=$nativeId no longer resolves by login or by id")
