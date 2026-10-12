package org.agripedia.core

import org.agripedia.core.model.Catalog
import org.agripedia.core.seed.SeedLoader
import org.agripedia.core.seed.SeedReader
import java.io.File

object TestData {
    val seedDir: File = File(System.getProperty("agripedia.seedDir") ?: "../data/seed")
    val reader = SeedReader { path -> File(seedDir, path).readText() }
    val catalog: Catalog by lazy { SeedLoader.load(reader) }
}
