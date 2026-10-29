package com.verdant.app.data.repository

import com.verdant.app.data.image.ImageStorage
import com.verdant.app.data.knowledge.KnowledgeBase
import com.verdant.app.data.local.ScanDao
import com.verdant.app.data.local.ScanEntity
import com.verdant.app.data.model.HealthStatus
import com.verdant.app.data.model.PlantInfo
import com.verdant.app.data.model.PlantMatch
import com.verdant.app.data.model.ScanRecord
import com.verdant.app.data.model.ScanStats
import com.verdant.app.ml.LabelUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanRepository @Inject constructor(
    private val dao: ScanDao,
    private val knowledgeBase: KnowledgeBase,
    private val imageStorage: ImageStorage,
) {
    fun observeAll(): Flow<List<ScanRecord>> =
        dao.observeAll().map { list -> list.map(::toRecord) }.flowOn(Dispatchers.Default)

    fun observeRecent(limit: Int = 10): Flow<List<ScanRecord>> =
        dao.observeRecent(limit).map { list -> list.map(::toRecord) }.flowOn(Dispatchers.Default)

    fun observe(id: Long): Flow<ScanRecord?> =
        dao.observeById(id).map { it?.let(::toRecord) }.flowOn(Dispatchers.Default)

    fun observeStats(): Flow<ScanStats> = dao.observeAll().map { list ->
        ScanStats(
            total = list.size,
            healthy = list.count { it.healthStatus == HealthStatus.HEALTHY },
            needsAttention = list.count { it.healthStatus == HealthStatus.DISEASED },
        )
    }

    suspend fun save(entity: ScanEntity): Long = dao.upsert(entity)

    suspend fun getEntity(id: Long): ScanEntity? = dao.getById(id)

    suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)

    /** Removes the row only; call [purgeImage] once an undo window has passed. */
    suspend fun deleteRow(id: Long) = dao.delete(id)

    suspend fun purgeImage(path: String) = imageStorage.delete(path)

    suspend fun delete(id: Long) {
        val entity = dao.getById(id) ?: return
        dao.delete(id)
        imageStorage.delete(entity.imagePath)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
        imageStorage.deleteAll()
    }

    suspend fun cleanUpOrphanImages() {
        imageStorage.deleteOrphans(dao.allImagePaths().toSet())
    }

    private fun toRecord(entity: ScanEntity): ScanRecord {
        val plant = knowledgeBase.findPlant(entity.plantLabel)
        val disease = entity.diseaseLabel?.let { knowledgeBase.findDisease(it) }
        return ScanRecord(
            id = entity.id,
            imagePath = entity.imagePath,
            createdAt = entity.createdAt,
            plantLabel = entity.plantLabel,
            plant = plant ?: fallbackPlant(entity.plantLabel),
            hasPlantDetails = plant != null,
            plantConfidence = entity.plantConfidence,
            alternatives = entity.plantAlternatives.map { p ->
                val info = knowledgeBase.findPlant(p.label)
                PlantMatch(
                    label = p.label,
                    commonName = info?.commonName ?: LabelUtils.prettify(p.label),
                    scientificName = info?.scientificName,
                    confidence = p.confidence,
                )
            },
            health = entity.healthStatus,
            diseaseLabel = entity.diseaseLabel,
            disease = disease ?: entity.diseaseLabel?.takeIf { entity.healthStatus == HealthStatus.DISEASED }
                ?.let { label ->
                    com.verdant.app.data.model.DiseaseInfo(
                        id = label,
                        name = LabelUtils.prettify(label),
                        description = "Detailed guidance for this condition isn't available yet. " +
                            "Isolate the plant and consult a local nursery or extension service.",
                    )
                },
            diseaseConfidence = entity.diseaseConfidence,
            isDemo = entity.isDemo,
            isFavorite = entity.isFavorite,
        )
    }

    private fun fallbackPlant(label: String) = PlantInfo(
        id = label,
        commonName = LabelUtils.prettify(label),
        scientificName = "",
        family = "Unknown family",
        description = "We recognised this plant, but don't have a care profile for it yet.",
    )
}
