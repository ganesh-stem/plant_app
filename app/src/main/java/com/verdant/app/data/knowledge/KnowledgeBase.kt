package com.verdant.app.data.knowledge

import android.content.Context
import com.verdant.app.data.model.DiseaseInfo
import com.verdant.app.data.model.PlantInfo
import com.verdant.app.ml.LabelUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps model labels to human-friendly plant and disease information.
 *
 * Data lives in the JSON files under `assets/knowledge`. A label matches an entry by its `id`,
 * its scientific/common name, or any of its `aliases` (all compared after
 * [LabelUtils.normalize]), so most model label formats work without code changes.
 */
interface KnowledgeBase {
    val plants: List<PlantInfo>
    val diseases: List<DiseaseInfo>
    fun findPlant(label: String): PlantInfo?
    fun findDisease(label: String): DiseaseInfo?
}

class JsonSource(val plantsJson: String, val diseasesJson: String)

open class InMemoryKnowledgeBase(source: () -> JsonSource) : KnowledgeBase {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val data by lazy {
        val s = source()
        val plants = json.decodeFromString<List<PlantInfo>>(s.plantsJson)
        val diseases = json.decodeFromString<List<DiseaseInfo>>(s.diseasesJson)
        val plantIndex = buildMap {
            plants.forEach { p ->
                (listOf(p.id, p.commonName, p.scientificName) + p.aliases)
                    .forEach { key -> putIfAbsent(LabelUtils.normalize(key), p) }
            }
        }
        val diseaseIndex = buildMap {
            diseases.forEach { d ->
                (listOf(d.id, d.name) + d.aliases).forEach { key -> putIfAbsent(LabelUtils.normalize(key), d) }
            }
        }
        Data(plants, diseases, plantIndex, diseaseIndex)
    }

    override val plants: List<PlantInfo> get() = data.plants
    override val diseases: List<DiseaseInfo> get() = data.diseases

    override fun findPlant(label: String): PlantInfo? {
        val key = LabelUtils.normalize(label)
        data.plantIndex[key]?.let { return it }
        // "Monstera deliciosa var. albo" → try the first two words (genus + species), then genus.
        val words = key.split('_')
        if (words.size > 2) data.plantIndex[words.take(2).joinToString("_")]?.let { return it }
        return null
    }

    override fun findDisease(label: String): DiseaseInfo? {
        if (LabelUtils.isHealthy(label)) return null
        data.diseaseIndex[LabelUtils.normalize(label)]?.let { return it }
        val condition = LabelUtils.splitCompound(label)?.second ?: return null
        val key = LabelUtils.normalize(condition)
        data.diseaseIndex[key]?.let { return it }
        // "Tomato___Tomato_Yellow_Leaf_Curl_Virus" → drop a repeated plant prefix.
        return data.diseaseIndex[key.substringAfter('_', "")]
    }

    private class Data(
        val plants: List<PlantInfo>,
        val diseases: List<DiseaseInfo>,
        val plantIndex: Map<String, PlantInfo>,
        val diseaseIndex: Map<String, DiseaseInfo>,
    )
}

@Singleton
class AssetKnowledgeBase @Inject constructor(
    @ApplicationContext context: Context,
) : InMemoryKnowledgeBase({
    JsonSource(
        plantsJson = context.assets.open("knowledge/plants.json").bufferedReader().use { it.readText() },
        diseasesJson = context.assets.open("knowledge/diseases.json").bufferedReader().use { it.readText() },
    )
})
