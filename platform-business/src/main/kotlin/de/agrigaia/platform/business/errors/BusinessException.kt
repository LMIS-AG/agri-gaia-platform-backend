package de.agrigaia.platform.business.errors

class BusinessException(override val message: String, val errorCode: ErrorType) : RuntimeException()