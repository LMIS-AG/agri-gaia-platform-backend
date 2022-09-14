package de.agrigaia.platform.api.errors

class ResourceIdMismatchException(resourceId: Long, payloadId: Long?) :
    Exception(String.format("Resource id [%d] does not match id [%d] of the payload object.",
        resourceId,
        payloadId))