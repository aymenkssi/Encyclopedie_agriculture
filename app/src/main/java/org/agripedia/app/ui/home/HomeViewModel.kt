package org.agripedia.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.agripedia.app.AppContainer
import org.agripedia.app.data.db.AgriculturalTaskEntity
import org.agripedia.app.data.db.UserCropEntity
import org.agripedia.app.data.repo.Knowledge
import org.agripedia.app.data.repo.UserProfile
import org.agripedia.core.engine.Months
import org.agripedia.core.engine.Recommendation
import org.agripedia.core.engine.RecommendationQuery
import org.agripedia.core.engine.Season
import org.agripedia.core.engine.CalendarEngine
import org.agripedia.core.model.Activity
import org.agripedia.core.model.Hemisphere
import org.agripedia.core.model.Plant
import org.agripedia.core.model.Region
import java.time.LocalDate

data class Period(val month: Int, val season: Season? = null, val months: Set<Int> = setOf(month))

/** Practical, data-derived seasonal advice (no generic invented text). */
sealed interface Tip {
    val months: Set<Int>
    data class Frost(override val months: Set<Int>) : Tip
    data class Dry(override val months: Set<Int>) : Tip
    data class Heat(override val months: Set<Int>) : Tip
    data class RainyStart(override val months: Set<Int>) : Tip
    data class Wet(override val months: Set<Int>) : Tip
}

data class HomeState(
    val region: Region? = null,
    val period: Period = Period(LocalDate.now().monthValue),
    val seasons: List<Season> = emptyList(),
    val recommendations: List<Recommendation> = emptyList(),
    val tasks: List<AgriculturalTaskEntity> = emptyList(),
    val cropActivities: List<Pair<Plant, List<Activity>>> = emptyList(),
    val tips: List<Tip> = emptyList(),
    val favorites: List<Plant> = emptyList(),
    val recent: List<Plant> = emptyList(),
)

class HomeViewModel(private val k: Knowledge, private val container: AppContainer) : ViewModel() {
    private val users = container.userRepository
    private val period = MutableStateFlow(Period(LocalDate.now().monthValue))

    private val userData = combine(
        users.profile, users.favoritePlantIds, users.recentPlantIds(10),
        users.openTasksUntil(LocalDate.now().plusDays(7)), users.crops,
    ) { profile, favs, recent, tasks, crops -> UserData(profile, favs, recent, tasks, crops) }

    private data class UserData(
        val profile: UserProfile, val favs: List<String>, val recent: List<String>,
        val tasks: List<AgriculturalTaskEntity>, val crops: List<UserCropEntity>,
    )

    val state: StateFlow<HomeState> = combine(userData, period) { u, p -> build(u, p) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())

    private fun build(u: UserData, p: Period): HomeState {
        val c = k.catalog
        val region = u.profile.regionId?.let { c.regionsById[it] }
        val recos = region?.let {
            k.recommendations.recommend(
                RecommendationQuery(it, p.months, u.profile.preferredMethod, u.profile.experience, u.profile.farmType)
            ).take(8)
        }.orEmpty()
        // Activities of the month for the user's crops and favorites, computed for their region.
        val ids = (u.crops.map { it.plantId } + u.favs).distinct()
        val acts = if (region == null) emptyList() else ids.mapNotNull { id ->
            val plant = c.plantsById[id] ?: return@mapNotNull null
            val method = u.crops.firstOrNull { it.plantId == id }?.methodId ?: plant.methods.first()
            val a = k.calendar.compute(plant, region, method).activitiesIn(p.month)
            if (a.isEmpty()) null else plant to a
        }
        return HomeState(
            region = region,
            period = p,
            seasons = Season.availableFor(region),
            recommendations = recos,
            tasks = u.tasks.take(6),
            cropActivities = acts,
            tips = region?.let { tips(it, p.months) }.orEmpty(),
            favorites = u.favs.mapNotNull { c.plantsById[it] },
            recent = u.recent.mapNotNull { c.plantsById[it] },
        )
    }

    private fun tips(region: Region, months: Set<Int>): List<Tip> {
        val list = mutableListOf<Tip>()
        val frost = CalendarEngine(k.catalog).frostMonths(region).intersect(months)
        if (frost.isNotEmpty()) list += Tip.Frost(frost)
        region.monthlyPrecipMm?.let { p ->
            val dry = months.filter { p[it - 1] < 20.0 }.toSet()
            if (dry.isNotEmpty()) list += Tip.Dry(dry)
            val wet = months.filter { p[it - 1] > 150.0 }.toSet()
            if (wet.isNotEmpty()) list += Tip.Wet(wet)
        }
        region.monthlyMeanTempC?.let { t ->
            val hot = months.filter { t[it - 1] >= 28.0 }.toSet()
            if (hot.isNotEmpty()) list += Tip.Heat(hot)
        }
        region.rainySeasonMonths?.takeIf { it.isNotEmpty() }?.let { rainy ->
            val starts = rainy.filter { Months.wrap(it - 1) !in rainy }.toSet()
            val upcoming = months.map { Months.wrap(it + 1) }.toSet() + months
            val s = starts.intersect(upcoming)
            if (s.isNotEmpty()) list += Tip.RainyStart(s)
        }
        return list
    }

    fun shiftMonth(delta: Int) = period.update { Period(Months.wrap(it.month + delta)) }
    fun selectMonth(m: Int) { period.value = Period(m) }

    fun selectSeason(s: Season) {
        val region = state.value.region
        val hemisphere = region?.let { k.catalog.hemisphereOf(it) } ?: Hemisphere.NORTH
        val months = Season.monthsOf(s, region, hemisphere)
        // Start the "current month" of a season at its first month (wrap-aware, e.g. winter = Dec).
        val first = Months.ranges(months).firstOrNull()?.first
        if (first != null) period.value = Period(first, s, months)
    }

    fun setRegion(r: Region) = viewModelScope.launch {
        users.saveProfile(users.profile.first().copy(countryId = r.countryId, regionId = r.id))
    }

    fun setDone(t: AgriculturalTaskEntity, done: Boolean) = viewModelScope.launch { users.setTaskDone(t, done) }
}
