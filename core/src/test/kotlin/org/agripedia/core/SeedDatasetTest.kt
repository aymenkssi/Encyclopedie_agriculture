package org.agripedia.core

import kotlinx.serialization.decodeFromString
import org.agripedia.core.model.Catalog
import org.agripedia.core.model.Plant
import org.agripedia.core.seed.SeedLoader
import org.agripedia.core.seed.SeedValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedDatasetTest {

    @Test
    fun manifestListsEveryPlantFile() {
        val files = TestData.seedDir.resolve("plants").listFiles()!!.filter { it.extension == "json" }.map { it.name }.sorted()
        assertEquals(files, TestData.catalog.manifest.plantFiles.sorted())
    }

    @Test
    fun plantFileNameMatchesId() {
        TestData.catalog.plants.forEach { assertTrue("file for ${it.id}", TestData.seedDir.resolve("plants/${it.id}.json").exists()) }
    }

    @Test
    fun datasetHasNoValidationErrors() {
        val errors = SeedValidator.validate(TestData.catalog).filter { it.severity == SeedValidator.Severity.ERROR }
        assertTrue(errors.joinToString("\n"), errors.isEmpty())
    }

    /** Validates every file in plants/ even if not yet listed in the manifest (useful while authoring). */
    @Test
    fun everyPlantFileParsesAndValidates() {
        val c = TestData.catalog
        val plants = TestData.seedDir.resolve("plants").listFiles()!!.filter { it.extension == "json" }.map {
            try { SeedLoader.json.decodeFromString<Plant>(it.readText()) } catch (e: Exception) { throw AssertionError("${it.name}: ${e.message}", e) }
        }
        val all = Catalog(c.manifest, c.reference, c.geography, c.health, plants)
        val errors = SeedValidator.validate(all).filter { it.severity == SeedValidator.Severity.ERROR }
        assertTrue(errors.joinToString("\n"), errors.isEmpty())
    }

    @Test
    fun demoDatasetIsFlaggedAsDemo() {
        assertTrue(TestData.catalog.manifest.demo)
        assertTrue(TestData.catalog.plants.all { it.isDemo })
    }

    @Test
    fun allRegionsHaveCompleteClimateNormals() {
        TestData.catalog.regionsById.values.forEach {
            assertEquals(it.id, 12, it.monthlyMeanTempC?.size)
            assertEquals(it.id, 12, it.monthlyPrecipMm?.size)
        }
    }
}
