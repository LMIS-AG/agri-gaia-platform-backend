package de.agrigaia.platform.api.example

import de.agrigaia.platform.api.BaseController
import de.agrigaia.platform.api.toDtos
import de.agrigaia.platform.api.toEntity
import de.agrigaia.platform.business.services.example.ExampleService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/examples")
class ExampleController @Autowired constructor(
    private val exampleService: ExampleService,
    private val mapper: ExampleMapper,
) : BaseController() {

    @GetMapping
    fun getExamples(): ResponseEntity<List<ExampleDto>> {
        val exampleDtos = this.exampleService.getExamples().toDtos(this.mapper)
        return ResponseEntity.ok(exampleDtos)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createExample(@RequestBody exampleDto: ExampleDto) {
        val exampleEntity = exampleDto.toEntity(this.mapper)
        this.exampleService.createExample(exampleEntity)
    }

    @PutMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun saveExample(@PathVariable id: Long, @RequestBody exampleDto: ExampleDto) {
        this.checkResourceIdMatchesPayloadId(id, exampleDto.id)

        val exampleEntity = exampleDto.toEntity(this.mapper)
        this.exampleService.updateExample(exampleEntity)
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteExample(@PathVariable id: Long) {
        this.exampleService.deleteExample(id)
    }

    @GetMapping("static")
    fun getStaticExamples(): ResponseEntity<List<ExampleDto>> {
        val exampleDtos = this.exampleService.getStaticExamples().toDtos(this.mapper)
        return ResponseEntity.ok(exampleDtos)
    }

    @GetMapping("true")
    fun getTrue() = true

    @GetMapping("error")
    fun getNotFound(): ResponseEntity<String> {
        this.exampleService.throwRandomException()
        return ResponseEntity.ok("should not reach here")
    }
}
