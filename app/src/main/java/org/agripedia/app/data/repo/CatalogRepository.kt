package org.agripedia.app.data.repo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.agripedia.app.data.seed.ContentImporter
import org.agripedia.core.engine.CalendarEngine
import org.agripedia.core.engine.RecommendationEngine
import org.agripedia.core.model.Catalog
import org.agripedia.core.search.PlantSearch

/** Everything the UI needs to consult the encyclopedia, built once from the local database. */
class Knowledge(val catalog: Catalog) {
    val calendar = CalendarEngine(catalog)
    val recommendations = RecommendationEngine(catalog)
    val search = PlantSearch(catalog)
}

sealed interface CatalogState {
    data object Loading : CatalogState
    data class Ready(val knowledge: Knowledge) : CatalogState
    data class Error(val message: String) : CatalogState
}

/**
 * Loads the encyclopedia: imports the bundled dataset into Room if needed, then
 * keeps an in-memory, read-only copy. Everything works offline.
 */
class CatalogRepository(private val importer: ContentImporter, private val scope: CoroutineScope) {
    private val _state = MutableStateFlow<CatalogState>(CatalogState.Loading)
    val state: StateFlow<CatalogState> = _state.asStateFlow()

    init { reload() }

    fun reload() {
        _state.value = CatalogState.Loading
        scope.launch {
            _state.value = try {
                val catalog = withContext(Dispatchers.IO) {
                    importer.ensureImported()
                    importer.loadFromDatabase()
                }
                CatalogState.Ready(Knowledge(catalog))
            } catch (e: Exception) {
                CatalogState.Error(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    val knowledgeOrNull: Knowledge? get() = (state.value as? CatalogState.Ready)?.knowledge
}
