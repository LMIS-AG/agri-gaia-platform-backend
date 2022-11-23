package de.agrigaia.platform.integration.assets

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AssetsService @Autowired constructor(){
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun publishAssets() {
        logger.info("Test AssetsService") // TODO remove
    }
}