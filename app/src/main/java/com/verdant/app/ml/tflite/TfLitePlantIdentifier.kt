package com.verdant.app.ml.tflite

import android.content.Context
import android.graphics.Bitmap
import com.verdant.app.data.model.Prediction
import com.verdant.app.ml.ModelConfig
import com.verdant.app.ml.ModelInfo
import com.verdant.app.ml.PlantIdentifier

class TfLitePlantIdentifier(context: Context) : PlantIdentifier {

    private val classifier = TfLiteImageClassifier(
        context = context,
        modelPath = ModelConfig.PLANT_MODEL_PATH,
        labelsPath = ModelConfig.PLANT_LABELS_PATH,
        normalization = ModelConfig.PLANT_NORMALIZATION,
    )

    override val info: ModelInfo
        get() = ModelInfo(
            displayName = "Plant identifier",
            source = ModelConfig.PLANT_MODEL_PATH.substringAfterLast('/'),
            isPlaceholder = false,
            classCount = classifier.classCount,
        )

    override suspend fun identify(bitmap: Bitmap): List<Prediction> = classifier.classify(bitmap)
}
