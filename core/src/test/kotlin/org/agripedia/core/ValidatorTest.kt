package org.agripedia.core

import org.agripedia.core.model.ValidationStatus
import org.agripedia.core.seed.SeedValidator
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorTest {
    private val c = TestData.catalog
    private val tomato = c.plantsById.getValue("solanum-lycopersicum")

    private fun errorsFor(modified: org.agripedia.core.model.Plant) =
        SeedValidator.validate(c.copy(plants = c.plants.map { if (it.id == modified.id) modified else it }))
            .filter { it.severity == SeedValidator.Severity.ERROR }

    @Test
    fun detectsUnknownReferences() {
        val errors = errorsFor(tomato.copy(categoryId = "nope", diseaseIds = listOf("ghost")))
        assertTrue(errors.any { "unknown category" in it.message })
        assertTrue(errors.any { "unknown disease" in it.message })
    }

    @Test
    fun verifiedStatusRequiresSourceAndCannotBeDemo() {
        val errors = errorsFor(tomato.copy(status = ValidationStatus.VERIFIED, sourceIds = emptyList()))
        assertTrue(errors.any { "requires at least one source" in it.message })
        assertTrue(errors.any { "demo content cannot be marked verified" in it.message })
    }

    @Test
    fun detectsInconsistentTemperatures() {
        val errors = errorsFor(tomato.copy(temperature = tomato.temperature.copy(min = 30.0)))
        assertTrue(errors.any { "temperatures" in it.message })
    }
}
