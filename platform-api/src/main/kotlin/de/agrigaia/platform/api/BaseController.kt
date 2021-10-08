package de.agrigaia.platform.api

import de.agrigaia.platform.api.errors.ResourceIdMismatchException

abstract class BaseController {

    protected fun checkResourceIdMatchesPayloadId(resourceId: Long, payloadId: Long?) {
        if (resourceId != payloadId) {
            throw ResourceIdMismatchException(resourceId, payloadId)
        }
    }
}