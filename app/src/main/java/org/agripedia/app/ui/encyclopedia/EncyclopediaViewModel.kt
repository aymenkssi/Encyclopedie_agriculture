package org.agripedia.app.ui.encyclopedia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import org.agripedia.app.AppContainer
import org.agripedia.app.data.repo.Knowledge
import org.agripedia.core.model.Difficulty
import org.agripedia.core.model.WaterNeed
import org.agripedia.core.search.CycleLength
import org.agripedia.core.search.PlantFilter
import org.agripedia.core.search.SearchHit
import java.time.LocalDate

data class EncyclopediaUi(
    val query: String = "",
    val filter: PlantFilter = PlantFilter(),
    val adapted: Boolean = false,
    val month: Int = LocalDate.now().monthValue,
    val regionId: String? = null,
) {
    val activeFilters: Int get() = filter.activeCount + if (adapted) 1 else 0
}

@OptIn(FlowPreview::class)
class EncyclopediaViewModel(
    private val k: Knowledge, container: AppContainer, query: String, category: String, adapted: Boolean,
) : ViewModel() {
    private val _ui = MutableStateFlow(
        EncyclopediaUi(query = query, filter = PlantFilter(categoryIds = setOfNotNull(category.ifEmpty { null })), adapted = adapted)
    )
    private val profile = container.userRepository.profile

    val ui: StateFlow<EncyclopediaUi> = combine(_ui, profile) { u, p ->
        u.copy(regionId = p.regionId?.takeIf { it in k.catalog.regionsById })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, _ui.value)

    val results: StateFlow<List<SearchHit>> = combine(_ui.debounce(150), profile) { u, p ->
        val region = if (u.adapted) p.regionId?.let { k.catalog.regionsById[it] } else null
        val filter = if (region != null) u.filter.copy(region = region, months = setOf(u.month)) else u.filter
        k.search.search(u.query, filter)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), k.search.search(query))

    fun setQuery(q: String) = _ui.update { it.copy(query = q) }
    fun setAdapted(v: Boolean) = _ui.update { it.copy(adapted = v) }
    fun clearFilters() = _ui.update { it.copy(filter = PlantFilter(), adapted = false) }
    private fun f(block: (PlantFilter) -> PlantFilter) = _ui.update { it.copy(filter = block(it.filter)) }
    private fun <T> Set<T>.toggle(v: T) = if (v in this) this - v else this + v
    fun toggleCategory(id: String) = f { it.copy(categoryIds = it.categoryIds.toggle(id)) }
    fun toggleDifficulty(d: Difficulty) = f { it.copy(difficulties = it.difficulties.toggle(d)) }
    fun toggleWater(w: WaterNeed) = f { it.copy(waterNeeds = it.waterNeeds.toggle(w)) }
    fun toggleMethod(id: String) = f { it.copy(methodIds = it.methodIds.toggle(id)) }
    fun toggleSoil(id: String) = f { it.copy(soilIds = it.soilIds.toggle(id)) }
    fun toggleZone(id: String) = f { it.copy(climateZoneIds = it.climateZoneIds.toggle(id)) }
    fun setCycle(c: CycleLength?) = f { it.copy(cycle = c) }
    fun setCountry(id: String?) = f { it.copy(countryId = id) }
}
