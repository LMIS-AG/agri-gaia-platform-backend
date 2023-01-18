package de.agrigaia.platform.api.agrovoc

import de.agrigaia.platform.api.BaseController
import de.agrigaia.platform.common.HasLogger
import de.agrigaia.platform.integration.agrovoc.AgrovocService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/agrovoc")
class AgrovocController @Autowired constructor(private val agrovocService: AgrovocService) : HasLogger, BaseController(){

    @GetMapping("agrovoc/keywords")
    fun getAgrovocKeywords(): ResponseEntity<List<String>> {
        val keywords: List<String> = this.agrovocService.fetchAgrovocKeywords()
        return ResponseEntity.ok(keywords)
    }
}