package com.verdant.app.ml

import com.verdant.app.data.model.Prediction
import kotlin.math.exp

object LabelUtils {

    /** "Tomato___Early_blight" → "tomato_early_blight"; "Pepper, bell" → "pepper_bell". */
    fun normalize(label: String): String =
        label.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')

    /** Splits PlantVillage-style labels ("Plant___Condition") into (plant, condition). */
    fun splitCompound(label: String): Pair<String, String>? {
        val idx = label.indexOf("___")
        if (idx <= 0 || idx + 3 >= label.length) return null
        return label.substring(0, idx) to label.substring(idx + 3)
    }

    fun isHealthy(label: String): Boolean {
        val condition = splitCompound(label)?.second ?: label
        return normalize(condition) == "healthy"
    }

    /** "early_blight" → "Early blight". Used when a label has no knowledge-base entry. */
    fun prettify(label: String): String {
        val condition = splitCompound(label)?.second ?: label
        val words = condition.replace(Regex("[_\\-]+"), " ").replace(Regex("\\s+"), " ").trim()
        return words.replaceFirstChar { it.uppercase() }
    }

    /** Returns true when scores already look like a probability distribution. */
    fun looksLikeProbabilities(scores: FloatArray): Boolean {
        if (scores.isEmpty()) return false
        if (scores.any { it < -1e-4f || it > 1.0001f }) return false
        val sum = scores.sum()
        return sum in 0.98f..1.02f
    }

    fun softmax(logits: FloatArray): FloatArray {
        if (logits.isEmpty()) return logits
        val max = logits.max()
        val exps = FloatArray(logits.size) { exp((logits[it] - max).toDouble()).toFloat() }
        val sum = exps.sum()
        return FloatArray(exps.size) { exps[it] / sum }
    }

    fun topK(scores: FloatArray, labels: List<String>, k: Int): List<Prediction> =
        scores.indices
            .sortedByDescending { scores[it] }
            .take(k)
            .map { Prediction(labels.getOrElse(it) { "class_$it" }, scores[it].coerceIn(0f, 1f)) }

    /** Re-scales a subset of predictions so they sum to 1. */
    fun renormalize(predictions: List<Prediction>): List<Prediction> {
        val sum = predictions.sumOf { it.confidence.toDouble() }.toFloat()
        if (sum <= 0f) return predictions
        return predictions.map { it.copy(confidence = it.confidence / sum) }
    }
}
