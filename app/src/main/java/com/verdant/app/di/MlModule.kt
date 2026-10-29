package com.verdant.app.di

import android.content.Context
import com.verdant.app.data.knowledge.KnowledgeBase
import com.verdant.app.ml.DiseaseDetector
import com.verdant.app.ml.ModelConfig
import com.verdant.app.ml.PlantIdentifier
import com.verdant.app.ml.placeholder.PlaceholderDiseaseDetector
import com.verdant.app.ml.placeholder.PlaceholderPlantIdentifier
import com.verdant.app.ml.tflite.TfLiteDiseaseDetector
import com.verdant.app.ml.tflite.TfLitePlantIdentifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Chooses real or placeholder models. If both the model and label files exist in
 * `assets/models/`, the TFLite implementation is used; otherwise the placeholder.
 * Each stage is decided independently, so you can ship one real model at a time.
 */
@Module
@InstallIn(SingletonComponent::class)
object MlModule {

    @Provides
    @Singleton
    fun providePlantIdentifier(
        @ApplicationContext context: Context,
        knowledgeBase: KnowledgeBase,
    ): PlantIdentifier =
        if (context.hasAssets(ModelConfig.PLANT_MODEL_PATH, ModelConfig.PLANT_LABELS_PATH)) {
            TfLitePlantIdentifier(context)
        } else {
            PlaceholderPlantIdentifier(knowledgeBase)
        }

    @Provides
    @Singleton
    fun provideDiseaseDetector(
        @ApplicationContext context: Context,
        knowledgeBase: KnowledgeBase,
    ): DiseaseDetector =
        if (context.hasAssets(ModelConfig.DISEASE_MODEL_PATH, ModelConfig.DISEASE_LABELS_PATH)) {
            TfLiteDiseaseDetector(context, knowledgeBase)
        } else {
            PlaceholderDiseaseDetector(knowledgeBase)
        }

    private fun Context.hasAssets(vararg paths: String): Boolean = paths.all { path ->
        runCatching { assets.open(path).close(); true }.getOrDefault(false)
    }
}
