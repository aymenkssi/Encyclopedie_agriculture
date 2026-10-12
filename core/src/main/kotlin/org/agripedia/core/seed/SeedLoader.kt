package org.agripedia.core.seed

import kotlinx.serialization.json.Json
import org.agripedia.core.model.Catalog
import org.agripedia.core.model.GeographyData
import org.agripedia.core.model.HealthData
import org.agripedia.core.model.Plant
import org.agripedia.core.model.ReferenceData
import org.agripedia.core.model.SeedManifest

/** Abstraction over where the seed files are read from (Android assets, files, network cache). */
fun interface SeedReader {
    /** Returns the content of the file at [path] relative to the seed root. */
    fun read(path: String): String
}

/**
 * Loads the dataset. Adding a plant only requires adding a JSON file under
 * `plants/` and listing it in `manifest.json` — no code change.
 */
object SeedLoader {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        encodeDefaults = false
        explicitNulls = false
    }

    fun loadManifest(reader: SeedReader): SeedManifest = json.decodeFromString(reader.read("manifest.json"))

    fun load(reader: SeedReader): Catalog {
        val manifest = loadManifest(reader)
        val reference = json.decodeFromString<ReferenceData>(reader.read("reference.json"))
        val geography = json.decodeFromString<GeographyData>(reader.read("geography.json"))
        val health = json.decodeFromString<HealthData>(reader.read("health.json"))
        val plants = manifest.plantFiles.map { file ->
            try {
                json.decodeFromString<Plant>(reader.read("plants/$file"))
            } catch (e: Exception) {
                throw SeedException("Invalid plant file '$file': ${e.message}", e)
            }
        }
        return Catalog(manifest, reference, geography, health, plants)
    }
}

class SeedException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
