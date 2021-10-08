package de.agrigaia.platform.api.errors

import de.agrigaia.platform.business.errors.BusinessException
import de.agrigaia.platform.business.errors.ErrorType
import de.agrigaia.platform.common.HasLogger
import org.springframework.beans.TypeMismatchException
import org.springframework.core.NestedRuntimeException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
open class AgriGaiaPlatformExceptionHandler : ResponseEntityExceptionHandler(), HasLogger {

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessExceptions(exception: BusinessException): ResponseEntity<ErrorDto> {
        return this.createExceptionResponse(exception, exception.errorCode)
    }

    @ExceptionHandler(ResourceIdMismatchException::class)
    fun handleResourceIdMismatchException(exception: ResourceIdMismatchException): ResponseEntity<ErrorDto> {
        return this.createExceptionResponse(exception, ErrorType.RESOURCE_ID_MISMATCH)
    }

    override fun handleHttpMessageNotReadable(
        e: HttpMessageNotReadableException,
        h: HttpHeaders,
        s: HttpStatus,
        r: WebRequest,
    )
            : ResponseEntity<Any> {
        return this.handleGenericException(e, s)
    }

    override fun handleTypeMismatch(e: TypeMismatchException, h: HttpHeaders, status: HttpStatus, r: WebRequest)
            : ResponseEntity<Any> {
        return this.handleGenericException(e, status)
    }

    private fun handleGenericException(e: NestedRuntimeException, s: HttpStatus)
            : ResponseEntity<Any> {
        val message = e.message?.substringBefore("; nested exception is")
            ?: "Unknown error occurred. No error message was provided"
        this.getLogger().warn("$s | $message")
        val errorDto = ErrorDto(message, s.name)
        return ResponseEntity.status(s).body(errorDto)
    }

    private fun createExceptionResponse(exception: Exception, errorType: ErrorType): ResponseEntity<ErrorDto> {
        val errorDto: ErrorDto = this.logMessageAndGetDto(exception.message.orEmpty(), errorType.name)
        return ResponseEntity.status(this.getStatus(errorType)).body(errorDto)
    }

    private fun logMessageAndGetDto(message: String, errorType: String): ErrorDto {
        this.getLogger().warn(message)
        return ErrorDto(message, errorType)
    }

    private fun getStatus(errorType: ErrorType): HttpStatus {
        return when (errorType) {
            ErrorType.EXAMPLE, ErrorType.RESOURCE_ID_MISMATCH -> HttpStatus.BAD_REQUEST
            ErrorType.NOT_FOUND -> HttpStatus.NOT_FOUND
            ErrorType.UNKNOWN -> HttpStatus.INTERNAL_SERVER_ERROR
        }
    }
}
