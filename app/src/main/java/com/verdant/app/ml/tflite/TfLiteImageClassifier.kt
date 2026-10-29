package com.verdant.app.ml.tflite

import android.content.Context
import android.graphics.Bitmap
import com.verdant.app.ml.ImagePreprocessor
import com.verdant.app.ml.LabelUtils
import com.verdant.app.ml.ModelConfig
import com.verdant.app.ml.ModelException
import com.verdant.app.ml.Normalization
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Generic single-label image classifier backed by TensorFlow Lite.
 *
 * It reads the input shape and data type from the model itself, so any classifier with
 * an input of `[1, H, W, 3]` and an output of `[1, N]` works:
 *  - float32, uint8 and int8 inputs/outputs (quantized outputs are de-quantized),
 *  - outputs that are probabilities or raw logits (softmax is applied when needed).
 *
 * The interpreter is created lazily on first use and guarded by a mutex because
 * [Interpreter] is not thread-safe.
 */
class TfLiteImageClassifier(
    private val context: Context,
    private val modelPath: String,
    private val labelsPath: String,
    private val normalization: Normalization,
) : AutoCloseable {

    private val mutex = Mutex()
    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()

    val classCount: Int? get() = labels.size.takeIf { it > 0 }

    suspend fun classify(bitmap: Bitmap, topK: Int = ModelConfig.TOP_K): List<com.verdant.app.data.model.Prediction> =
        withContext(Dispatchers.Default) {
            mutex.withLock {
                val interpreter = interpreter ?: load().also { interpreter = it }
                try {
                    val scores = run(interpreter, bitmap)
                    LabelUtils.topK(scores, labels, topK)
                } catch (e: Exception) {
                    throw ModelException("Inference failed for $modelPath", e)
                }
            }
        }

    /** Returns every class score (useful for filtering by plant). */
    suspend fun scores(bitmap: Bitmap): Pair<FloatArray, List<String>> =
        withContext(Dispatchers.Default) {
            mutex.withLock {
                val interpreter = interpreter ?: load().also { interpreter = it }
                try {
                    run(interpreter, bitmap) to labels
                } catch (e: Exception) {
                    throw ModelException("Inference failed for $modelPath", e)
                }
            }
        }

    private fun load(): Interpreter {
        try {
            val options = Interpreter.Options().apply { setNumThreads(ModelConfig.INFERENCE_THREADS) }
            val model = Interpreter(loadModelFile(), options)
            labels = context.assets.open(labelsPath).bufferedReader().useLines { lines ->
                lines.map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toList()
            }
            val outputClasses = model.getOutputTensor(0).shape().last()
            if (labels.size != outputClasses) {
                // Keep going with generic names rather than crashing; mismatches are logged in the label.
                labels = List(outputClasses) { labels.getOrElse(it) { "class_$it" } }
            }
            return model
        } catch (e: Exception) {
            throw ModelException("Could not load model '$modelPath'", e)
        }
    }

    private fun loadModelFile(): MappedByteBuffer =
        context.assets.openFd(modelPath).use { fd ->
            FileInputStream(fd.fileDescriptor).use { stream ->
                stream.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            }
        }

    private fun run(interpreter: Interpreter, bitmap: Bitmap): FloatArray {
        val inputTensor = interpreter.getInputTensor(0)
        val shape = inputTensor.shape() // [1, H, W, 3]
        require(shape.size == 4 && shape[3] == 3) { "Expected NHWC RGB input, got ${shape.contentToString()}" }
        val height = shape[1]
        val width = shape[2]

        val resized = ImagePreprocessor.centerCropAndScale(bitmap, width, height)
        val input: ByteBuffer = when (inputTensor.dataType()) {
            DataType.FLOAT32 -> ImagePreprocessor.toFloatBuffer(resized, normalization)
            DataType.UINT8 -> ImagePreprocessor.toByteBuffer(resized, signed = false)
            DataType.INT8 -> ImagePreprocessor.toByteBuffer(resized, signed = true)
            else -> error("Unsupported input type ${inputTensor.dataType()}")
        }
        if (resized !== bitmap) resized.recycle()

        val outputTensor = interpreter.getOutputTensor(0)
        val classes = outputTensor.shape().last()
        val output = ByteBuffer.allocateDirect(outputTensor.numBytes()).order(ByteOrder.nativeOrder())
        interpreter.run(input, output)
        output.rewind()

        val raw = FloatArray(classes)
        when (outputTensor.dataType()) {
            DataType.FLOAT32 -> output.asFloatBuffer().get(raw)
            DataType.UINT8, DataType.INT8 -> {
                val q = outputTensor.quantizationParams()
                val scale = if (q.scale == 0f) 1f / 255f else q.scale
                val unsigned = outputTensor.dataType() == DataType.UINT8
                for (i in 0 until classes) {
                    val b = output.get().toInt()
                    val value = if (unsigned) b and 0xFF else b
                    raw[i] = (value - q.zeroPoint) * scale
                }
            }
            else -> error("Unsupported output type ${outputTensor.dataType()}")
        }
        return if (LabelUtils.looksLikeProbabilities(raw)) raw else LabelUtils.softmax(raw)
    }

    override fun close() {
        interpreter?.close()
        interpreter = null
    }
}
