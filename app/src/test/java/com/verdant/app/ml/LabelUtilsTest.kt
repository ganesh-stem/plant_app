package com.verdant.app.ml

import com.google.common.truth.Truth.assertThat
import com.verdant.app.data.model.Prediction
import org.junit.Test

class LabelUtilsTest {

    @Test
    fun `normalize lowercases and collapses separators`() {
        assertThat(LabelUtils.normalize("Tomato___Early_blight")).isEqualTo("tomato_early_blight")
        assertThat(LabelUtils.normalize("Pepper,_bell")).isEqualTo("pepper_bell")
        assertThat(LabelUtils.normalize("  Monstera deliciosa ")).isEqualTo("monstera_deliciosa")
    }

    @Test
    fun `splitCompound handles PlantVillage labels`() {
        assertThat(LabelUtils.splitCompound("Tomato___Late_blight")).isEqualTo("Tomato" to "Late_blight")
        assertThat(LabelUtils.splitCompound("late_blight")).isNull()
        assertThat(LabelUtils.splitCompound("___x")).isNull()
        assertThat(LabelUtils.splitCompound("Tomato___")).isNull()
    }

    @Test
    fun `isHealthy recognises plain and compound labels`() {
        assertThat(LabelUtils.isHealthy("healthy")).isTrue()
        assertThat(LabelUtils.isHealthy("Apple___healthy")).isTrue()
        assertThat(LabelUtils.isHealthy("Tomato___Early_blight")).isFalse()
        assertThat(LabelUtils.isHealthy("unhealthy")).isFalse()
    }

    @Test
    fun `prettify produces readable names`() {
        assertThat(LabelUtils.prettify("Tomato___Early_blight")).isEqualTo("Early blight")
        assertThat(LabelUtils.prettify("leaf-scorch")).isEqualTo("Leaf scorch")
    }

    @Test
    fun `softmax output sums to one and keeps order`() {
        val out = LabelUtils.softmax(floatArrayOf(2f, 1f, 0.1f))
        assertThat(out.sum().toDouble()).isWithin(1e-5).of(1.0)
        assertThat(out[0]).isGreaterThan(out[1])
        assertThat(out[1]).isGreaterThan(out[2])
    }

    @Test
    fun `softmax is stable for large logits`() {
        val out = LabelUtils.softmax(floatArrayOf(1000f, 999f))
        assertThat(out.all { !it.isNaN() }).isTrue()
    }

    @Test
    fun `detects probability vectors versus logits`() {
        assertThat(LabelUtils.looksLikeProbabilities(floatArrayOf(0.7f, 0.2f, 0.1f))).isTrue()
        assertThat(LabelUtils.looksLikeProbabilities(floatArrayOf(3.2f, -1f, 0.5f))).isFalse()
        assertThat(LabelUtils.looksLikeProbabilities(floatArrayOf(0.2f, 0.2f))).isFalse()
        assertThat(LabelUtils.looksLikeProbabilities(floatArrayOf())).isFalse()
    }

    @Test
    fun `topK sorts and falls back to generic labels`() {
        val result = LabelUtils.topK(floatArrayOf(0.1f, 0.6f, 0.3f), listOf("a", "b"), 2)
        assertThat(result).containsExactly(Prediction("b", 0.6f), Prediction("class_2", 0.3f)).inOrder()
    }

    @Test
    fun `renormalize rescales subset`() {
        val result = LabelUtils.renormalize(listOf(Prediction("a", 0.2f), Prediction("b", 0.2f)))
        assertThat(result.map { it.confidence }).containsExactly(0.5f, 0.5f)
    }
}
