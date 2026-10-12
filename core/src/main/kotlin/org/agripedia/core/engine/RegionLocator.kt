package org.agripedia.core.engine

import org.agripedia.core.model.Catalog
import org.agripedia.core.model.Region
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Finds the closest known region to a coordinate (used only after explicit user consent). */
object RegionLocator {
    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }

    /** Returns the nearest region and its distance, or null if none is within [maxKm]. */
    fun nearest(catalog: Catalog, lat: Double, lon: Double, maxKm: Double = 400.0): Pair<Region, Double>? =
        catalog.regionsById.values
            .map { it to haversineKm(lat, lon, it.latitude, it.longitude) }
            .minByOrNull { it.second }
            ?.takeIf { it.second <= maxKm }
}
