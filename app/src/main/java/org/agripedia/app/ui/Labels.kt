package org.agripedia.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import org.agripedia.app.R
import org.agripedia.app.data.repo.CropStage
import org.agripedia.core.engine.ExperienceLevel
import org.agripedia.core.engine.FarmType
import org.agripedia.core.engine.Months
import org.agripedia.core.engine.Note
import org.agripedia.core.engine.NoteCode
import org.agripedia.core.engine.Season
import org.agripedia.core.engine.SuitabilityLevel
import org.agripedia.core.model.Activity
import org.agripedia.core.model.Difficulty
import org.agripedia.core.model.FrostTolerance
import org.agripedia.core.model.HealthIssueType
import org.agripedia.core.model.Lang
import org.agripedia.core.model.Lifecycle
import org.agripedia.core.model.LocalizedText
import org.agripedia.core.model.SowingMode
import org.agripedia.core.model.SunExposure
import org.agripedia.core.model.TreatmentType
import org.agripedia.core.model.ValidationStatus
import org.agripedia.core.model.WaterNeed
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/** Content language derived from the app locale (fr default, ar, en). */
@Composable
@ReadOnlyComposable
fun contentLang(): String {
    val l = LocalConfiguration.current.locales[0]?.language ?: Lang.FR
    return if (l in Lang.SUPPORTED) l else Lang.FR
}

@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0] ?: Locale.FRENCH

@Composable
fun tr(text: LocalizedText?): String = Lang.pick(text, contentLang()).orEmpty()

@Composable
fun monthName(month: Int, short: Boolean = false): String =
    Month.of(month).getDisplayName(if (short) TextStyle.SHORT_STANDALONE else TextStyle.FULL_STANDALONE, currentLocale())
        .replaceFirstChar { it.titlecase(currentLocale()) }

@Composable
fun monthList(months: Collection<Int>): String {
    if (months.isEmpty()) return "—"
    val set = months.toSet()
    return Months.ranges(set).map { (s, e) -> if (s == e) monthName(s) else "${monthName(s)} – ${monthName(e)}" }.joinToString(", ")
}

@Composable
fun label(a: Activity): String = stringResource(
    when (a) {
        Activity.SOIL_PREPARATION -> R.string.act_soil_preparation
        Activity.NURSERY_SOWING -> R.string.act_nursery_sowing
        Activity.DIRECT_SOWING -> R.string.act_direct_sowing
        Activity.TRANSPLANTING -> R.string.act_transplanting
        Activity.PLANTING -> R.string.act_planting
        Activity.WATERING -> R.string.act_watering
        Activity.FERTILIZING -> R.string.act_fertilizing
        Activity.PRUNING -> R.string.act_pruning
        Activity.FLOWERING -> R.string.act_flowering
        Activity.HEALTH_MONITORING -> R.string.act_health_monitoring
        Activity.TREATMENT -> R.string.act_treatment
        Activity.HARVEST -> R.string.act_harvest
        Activity.ROTATION_PLANNING -> R.string.act_rotation_planning
    }
)

@Composable
fun label(d: Difficulty): String = stringResource(
    when (d) { Difficulty.EASY -> R.string.diff_easy; Difficulty.MEDIUM -> R.string.diff_medium; Difficulty.HARD -> R.string.diff_hard }
)

@Composable
fun label(w: WaterNeed): String = stringResource(
    when (w) { WaterNeed.LOW -> R.string.water_low; WaterNeed.MEDIUM -> R.string.water_medium; WaterNeed.HIGH -> R.string.water_high }
)

@Composable
fun label(s: SunExposure): String = stringResource(
    when (s) { SunExposure.FULL_SUN -> R.string.sun_full; SunExposure.PARTIAL_SHADE -> R.string.sun_partial; SunExposure.SHADE -> R.string.sun_shade }
)

@Composable
fun label(f: FrostTolerance): String = stringResource(
    when (f) { FrostTolerance.NONE -> R.string.frost_none; FrostTolerance.LIGHT -> R.string.frost_light; FrostTolerance.HARDY -> R.string.frost_hardy }
)

@Composable
fun label(l: Lifecycle): String = stringResource(
    when (l) { Lifecycle.ANNUAL -> R.string.life_annual; Lifecycle.BIENNIAL -> R.string.life_biennial; Lifecycle.PERENNIAL -> R.string.life_perennial }
)

@Composable
fun label(s: SowingMode): String = stringResource(
    when (s) {
        SowingMode.DIRECT -> R.string.sow_direct; SowingMode.TRANSPLANT -> R.string.sow_transplant
        SowingMode.BOTH -> R.string.sow_both; SowingMode.VEGETATIVE -> R.string.sow_vegetative
    }
)

@Composable
fun label(s: ValidationStatus): String = stringResource(
    when (s) { ValidationStatus.VERIFIED -> R.string.status_verified; ValidationStatus.INDICATIVE -> R.string.status_indicative; ValidationStatus.TO_VERIFY -> R.string.status_to_verify }
)

@Composable
fun label(s: SuitabilityLevel): String = stringResource(
    when (s) {
        SuitabilityLevel.RECOMMENDED -> R.string.suit_recommended; SuitabilityLevel.POSSIBLE -> R.string.suit_possible
        SuitabilityLevel.NOT_RECOMMENDED -> R.string.suit_not_recommended; SuitabilityLevel.UNKNOWN -> R.string.suit_unknown
    }
)

@Composable
fun label(s: Season): String = stringResource(
    when (s) {
        Season.SPRING -> R.string.season_spring; Season.SUMMER -> R.string.season_summer; Season.AUTUMN -> R.string.season_autumn
        Season.WINTER -> R.string.season_winter; Season.RAINY -> R.string.season_rainy; Season.DRY -> R.string.season_dry
    }
)

@Composable
fun label(e: ExperienceLevel): String = stringResource(
    when (e) { ExperienceLevel.BEGINNER -> R.string.exp_beginner; ExperienceLevel.INTERMEDIATE -> R.string.exp_intermediate; ExperienceLevel.EXPERT -> R.string.exp_expert }
)

@Composable
fun label(f: FarmType): String = stringResource(
    when (f) {
        FarmType.BALCONY -> R.string.farm_balcony; FarmType.GARDEN -> R.string.farm_garden; FarmType.SMALL_FARM -> R.string.farm_small
        FarmType.MEDIUM_FARM -> R.string.farm_medium; FarmType.LARGE_FARM -> R.string.farm_large
    }
)

@Composable
fun label(s: CropStage): String = stringResource(
    when (s) {
        CropStage.PLANNED -> R.string.stage_planned; CropStage.SOWN -> R.string.stage_sown; CropStage.GROWING -> R.string.stage_growing
        CropStage.FLOWERING -> R.string.stage_flowering; CropStage.HARVESTING -> R.string.stage_harvesting; CropStage.FINISHED -> R.string.stage_finished
    }
)

@Composable
fun label(t: HealthIssueType): String = stringResource(
    when (t) {
        HealthIssueType.FUNGAL -> R.string.health_fungal; HealthIssueType.OOMYCETE -> R.string.health_oomycete
        HealthIssueType.BACTERIAL -> R.string.health_bacterial; HealthIssueType.VIRAL -> R.string.health_viral
        HealthIssueType.PHYSIOLOGICAL -> R.string.health_physiological; HealthIssueType.INSECT -> R.string.health_insect
        HealthIssueType.MITE -> R.string.health_mite; HealthIssueType.MOLLUSC -> R.string.health_mollusc
        HealthIssueType.NEMATODE -> R.string.health_nematode; HealthIssueType.OTHER -> R.string.health_other
    }
)

@Composable
fun label(t: TreatmentType): String = stringResource(
    when (t) {
        TreatmentType.CULTURAL -> R.string.treat_cultural; TreatmentType.MECHANICAL -> R.string.treat_mechanical
        TreatmentType.BIOLOGICAL -> R.string.treat_biological; TreatmentType.CHEMICAL -> R.string.treat_chemical
    }
)

/** Human explanation of an engine note, in the UI language. */
@Composable
fun noteText(n: Note): String {
    val months = monthList(n.months)
    val v = n.value?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: ""
    return when (n.code) {
        NoteCode.NO_CLIMATE_DATA -> stringResource(R.string.note_no_climate)
        NoteCode.NO_CYCLE_DATA -> stringResource(R.string.note_no_cycle)
        NoteCode.GREENHOUSE_OFFSET_ASSUMED -> stringResource(R.string.note_greenhouse, v)
        NoteCode.CONTROLLED_ENVIRONMENT -> stringResource(R.string.note_controlled)
        NoteCode.NURSERY_SHELTERED_ASSUMED -> stringResource(R.string.note_nursery)
        NoteCode.FROST_RISK_MONTHS -> stringResource(R.string.note_frost, months)
        NoteCode.IRRIGATION_REQUIRED -> stringResource(R.string.note_irrigation, months)
        NoteCode.RAINY_SEASON_ALIGNED -> stringResource(R.string.note_rainy, months)
        NoteCode.NO_WINTER_CHILL -> stringResource(R.string.note_no_chill, v)
        NoteCode.WINTER_TOO_COLD -> stringResource(R.string.note_winter_cold, v)
        NoteCode.SUMMER_TOO_COOL -> stringResource(R.string.note_summer_cool, v)
        NoteCode.TOO_HOT_MONTHS -> stringResource(R.string.note_too_hot, months, v)
        NoteCode.SOUTHERN_HEMISPHERE_SHIFT -> stringResource(R.string.note_south)
        NoteCode.TROPICAL_PERENNIAL_VARIABLE -> stringResource(R.string.note_tropical_perennial)
        NoteCode.NO_WINDOW_FOUND -> stringResource(R.string.note_no_window)
        NoteCode.METHOD_NOT_SUITED -> stringResource(R.string.note_method)
        NoteCode.CLIMATE_ZONE_NOT_LISTED -> stringResource(R.string.note_zone)
        NoteCode.DIFFICULT_FOR_BEGINNER -> stringResource(R.string.note_difficult)
        NoteCode.EXPLICIT_DATA_USED -> stringResource(R.string.note_explicit)
        NoteCode.INDICATIVE_ONLY -> stringResource(R.string.note_indicative)
    }
}

@Composable
fun daysText(n: Int): String = pluralStringResource(R.plurals.days, n, n)
