package app.projectzero.domain

class DomainInvariantException(
    val reasonCode: ReasonCode,
    message: String,
) : IllegalArgumentException("${reasonCode.wireValue}: $message")
