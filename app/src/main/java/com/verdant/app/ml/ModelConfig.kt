package com.verdant.app.ml

/**
 * Central place for model file names and decision thresholds.
 *
 * To plug in real models, drop the files below into `app/src/main/assets/models/`.
 * The app detects them at startup and switches from the placeholder automatically.
 */
object ModelConfig {
    const val PLANT_MODEL_PATH = "models/plant_identifier.tflite"
    const val PLANT_LABELS_PATH = "models/plant_labels.txt"
    val PLANT_NORMALIZATION = Normalization.ZERO_TO_ONE

    const val DISEASE_MODEL_PATH = "models/disease_detector.tflite"
    const val DISEASE_LABELS_PATH = "models/disease_labels.txt"
    val DISEASE_NORMALIZATION = Normalization.ZERO_TO_ONE

    /** Below this, stage 1 reports "no plant found" instead of guessing. */
    const val MIN_PLANT_CONFIDENCE = 0.30f

    /** Labels the plant model may use for "not a plant" / background classes. */
    val NON_PLANT_LABELS = setOf("background", "not_a_plant", "non_plant", "other", "unknown")

    /** Below this, a disease result is shown as "possible" rather than certain. */
    const val CONFIDENT_DISEASE_THRESHOLD = 0.55f

    const val TOP_K = 5

    /** Longest edge of photos stored on disk. */
    const val STORED_IMAGE_MAX_EDGE = 1600
    const val INFERENCE_THREADS = 4
}

/** How pixel values are scaled for float models. Ignored for quantized models. */
enum class Normalization(val mean: Float, val std: Float) {
    /** pixel / 255 → [0, 1] (most Keras / TF Hub image models). */
    ZERO_TO_ONE(0f, 255f),
    /** (pixel - 127.5) / 127.5 → [-1, 1] (MobileNet v2/v3 style). */
    MINUS_ONE_TO_ONE(127.5f, 127.5f),
    /** Raw 0..255 values (models with a built-in rescaling layer). */
    NONE(0f, 1f),
}
