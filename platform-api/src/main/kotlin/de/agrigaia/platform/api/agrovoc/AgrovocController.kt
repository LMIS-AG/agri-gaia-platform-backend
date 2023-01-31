package de.agrigaia.platform.api.agrovoc

import de.agrigaia.platform.api.BaseController
import de.agrigaia.platform.common.HasLogger
import de.agrigaia.platform.integration.agrovoc.AgrovocService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/agrovoc")
class AgrovocController @Autowired constructor(private val agrovocService: AgrovocService) : HasLogger,
    BaseController() {

    @GetMapping("label/{keyword}")
    fun getConceptUriFromKeyword(@PathVariable keyword: String): ResponseEntity<String> {
        val labelUri: String = this.agrovocService.getLabelUriFromKeyword(keyword)
        val conceptUri: String = this.agrovocService.getConceptUriFromLabelUri(labelUri)
        return ResponseEntity.ok(conceptUri)
    }

}