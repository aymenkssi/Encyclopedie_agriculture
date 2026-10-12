package org.agripedia.app.ui.plant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.agripedia.app.AppContainer
import org.agripedia.app.data.db.UserNoteEntity
import org.agripedia.app.data.repo.Knowledge
import org.agripedia.core.engine.PlantCalendar
import org.agripedia.core.engine.Recommendation
import org.agripedia.core.engine.RecommendationQuery
import org.agripedia.core.model.Plant
import org.agripedia.core.model.Region
import java.time.LocalDate

data class PlantDetailState(
    val region: Region? = null,
    val methodId: String = "",
    val varietyId: String? = null,
    val calendar: PlantCalendar? = null,
    val recommendation: Recommendation? = null,
    val favorite: Boolean = false,
    val notes: List<UserNoteEntity> = emptyList(),
)

class PlantDetailViewModel(private val k: Knowledge, private val container: AppContainer, val plant: Plant) : ViewModel() {
    private val users = container.userRepository
    private val method = MutableStateFlow<String?>(null)
    private val variety = MutableStateFlow<String?>(null)

    init { viewModelScope.launch { users.markViewed(plant.id) } }

    val state: StateFlow<PlantDetailState> = combine(
        users.profile, method, variety, users.favoritePlantIds, users.notesOfPlant(plant.id),
    ) { profile, m, v, favs, notes ->
        val region = profile.regionId?.let { k.catalog.regionsById[it] }
        val methodId = m ?: profile.preferredMethod.takeIf { it in plant.methods } ?: plant.methods.first()
        val cal = region?.let { k.calendar.compute(plant, it, methodId, v) }
        val reco = region?.let {
            k.recommendations.evaluate(plant, RecommendationQuery(it, setOf(LocalDate.now().monthValue), methodId, profile.experience, profile.farmType))
        }
        PlantDetailState(region, methodId, v, cal, reco, plant.id in favs, notes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlantDetailState(methodId = plant.methods.first()))

    fun setMethod(id: String) { method.value = id }
    fun setVariety(id: String?) { variety.value = id }
    fun toggleFavorite() = viewModelScope.launch { users.toggleFavorite(plant.id) }
    fun saveNote(existing: UserNoteEntity?, title: String, body: String) = viewModelScope.launch { users.saveNote(existing, title, body, plant.id, null) }
    fun deleteNote(n: UserNoteEntity) = viewModelScope.launch { users.deleteNote(n) }
}
