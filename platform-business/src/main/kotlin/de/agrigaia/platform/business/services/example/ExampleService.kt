package de.agrigaia.platform.business.services.example

import de.agrigaia.platform.business.errors.BusinessException
import de.agrigaia.platform.business.errors.ErrorType
import de.agrigaia.platform.model.example.ExampleEntity
import de.agrigaia.platform.persistence.repository.ExampleRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class ExampleService @Autowired constructor(private val exampleRepository: ExampleRepository) {

    fun updateExample(example: ExampleEntity) {
        this.checkExistence(example.id)
        this.exampleRepository.save(example)
    }

    fun createExample(example: ExampleEntity) {
        this.exampleRepository.save(example)
    }

    fun getExamples(): List<ExampleEntity> {
        return this.exampleRepository.findAll()
    }

    fun getStaticExamples(): List<ExampleEntity> {
        val list = ArrayList<ExampleEntity>()
        val exampleDto1 = ExampleEntity("Dto 1: Line One", "Dto 1: Line 2")
        val exampleDto2 = ExampleEntity()
        exampleDto2.lineOne = "Dto 2: Line One"
        exampleDto2.lineTwo = "Dto 2: Line 2"

        list.add(exampleDto1)
        list.add(exampleDto2)
        return list
    }

    fun deleteExample(id: Long) {
        this.checkExistence(id)
        this.exampleRepository.deleteById(id)
    }

    fun throwRandomException() {
        when (Random.nextInt(2)) {
            0 -> throw BusinessException("The requested entity does not exist", ErrorType.NOT_FOUND)
            1 -> throw BusinessException("Example error", ErrorType.EXAMPLE)
            2 -> throw BusinessException("An unknown error occurred", ErrorType.UNKNOWN)
        }
    }

    private fun checkExistence(id: Long) {
        if (!this.exampleRepository.existsById(id)) {
            throw BusinessException("The entity with the id [${id}] was not found", ErrorType.NOT_FOUND)
        }
    }
}