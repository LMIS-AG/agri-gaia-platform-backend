package de.agrigaia.platform.common

import org.slf4j.LoggerFactory

interface HasLogger {
    fun getLogger() = LoggerFactory.getLogger(this.javaClass)
}