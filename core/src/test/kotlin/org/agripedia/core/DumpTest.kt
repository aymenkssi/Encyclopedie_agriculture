package org.agripedia.core

import org.agripedia.core.engine.CalendarEngine
import org.agripedia.core.engine.RecommendationEngine
import org.agripedia.core.engine.RecommendationQuery
import org.agripedia.core.seed.SeedValidator
import org.junit.Test

class DumpTest {
    @Test fun dump() {
        if (System.getenv("AGRI_DUMP") == null) return
        val c = TestData.catalog
        SeedValidator.validate(c).forEach { println("VALIDATOR $it") }
        val e = CalendarEngine(c)
        for (r in listOf("fr-idf", "ma-rsk", "sn-kao", "ca-qc", "br-sp", "dz-bsk")) for (p in c.plants) {
            val cal = e.compute(p, c.regionsById.getValue(r), p.methods.first())
            println("CAL $r ${p.id}: " + cal.windows.filter { it.activity.name in setOf("NURSERY_SOWING","DIRECT_SOWING","TRANSPLANTING","PLANTING","HARVEST") }
                .joinToString { "${it.activity.name.take(6)}=${it.months.sorted()}${it.origin.name.take(4)}" })
        }
        val re = RecommendationEngine(c)
        for (r in listOf("fr-idf", "ma-rsk", "sn-kao")) for (m in listOf(3, 7, 10)) println("RECO $r m$m: " +
            re.recommend(RecommendationQuery(c.regionsById.getValue(r), setOf(m)), includeAll = true).joinToString { "${it.plant.id.take(10)}:${it.level.name.take(4)}" })
    }
}
