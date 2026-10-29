# Plugging in the AI models

Verdant runs two on-device TensorFlow Lite models in sequence:

| Stage | Purpose | Model file | Labels file |
|---|---|---|---|
| 1 | Identify the plant species | `assets/models/plant_identifier.tflite` | `assets/models/plant_labels.txt` |
| 2 | Detect disease / pests / deficiency | `assets/models/disease_detector.tflite` | `assets/models/disease_labels.txt` |

Until these files exist, **placeholder models** produce simulated, deterministic results
(the same photo always gives the same answer) and every result shows a "Demo mode" banner.

## How switching works

`di/MlModule.kt` checks for the model **and** labels file at startup. If both exist, the
TFLite implementation is used; otherwise the placeholder. Each stage is decided separately,
so you can ship a real plant model first and add the disease model later.
**Settings → AI models** shows which one is active and how many classes it has.

## Model contract

Both models are generic single-label image classifiers (`ml/tflite/TfLiteImageClassifier.kt`):

- **Input:** `[1, H, W, 3]` RGB, NHWC. `H` and `W` are read from the model, so any size works (224, 299, 384...).
  - `float32`: pixels are scaled per `ModelConfig.PLANT_NORMALIZATION` / `DISEASE_NORMALIZATION`:
    - `ZERO_TO_ONE` → `pixel / 255` (most Keras / TF Hub models)
    - `MINUS_ONE_TO_ONE` → `(pixel - 127.5) / 127.5` (MobileNet style)
    - `NONE` → raw 0..255 (models with a built-in `Rescaling` layer)
  - `uint8` / `int8` (quantized): fed raw; normalization is ignored.
- **Output:** `[1, N]` scores. Probabilities **or** logits; softmax is applied automatically when needed.
  Quantized outputs are de-quantized with the tensor's scale and zero point.
- **Labels file:** one label per line, in output order. Blank lines and lines starting with `#` are skipped.
  If the count doesn't match `N`, missing labels become `class_<i>` instead of crashing.

The photo is center-cropped to a square before resizing, so train with similar crops.

### Plant model labels

- Use ids, scientific names or common names: `monstera_deliciosa`, `Monstera deliciosa` and `Monstera` all work.
- Add a `background` (or `not_a_plant`) class if you can. When it wins, the user sees "We couldn't find a plant".
- If the top score is below `ModelConfig.MIN_PLANT_CONFIDENCE` (0.30), the app also reports no plant instead of guessing.

### Disease model labels

Two formats are supported:

1. **Plain conditions:** `healthy`, `early_blight`, `powdery_mildew`, ...
2. **PlantVillage style:** `Tomato___Early_blight`, `Tomato___healthy`, `Pepper,_bell___Bacterial_spot`, ...

With format 2, results are **restricted to the plant found in stage 1** and re-normalised,
so the app never suggests e.g. apple scab on a tomato. If the model has no classes for that
plant, all classes are used.

Any label whose condition part is `healthy` counts as healthy. Disease results under
`ModelConfig.CONFIDENT_DISEASE_THRESHOLD` (0.55) are shown as "Possible ...".

## Knowledge base (names, care guides, treatments)

Labels are mapped to human-friendly content in:

- `assets/knowledge/plants.json`: names, family, description, care guide, toxicity, fun fact
- `assets/knowledge/diseases.json`: pathogen type, severity, contagious, symptoms, treatment, prevention

Matching compares the label, after normalising it (lowercase, non-alphanumerics → `_`),
against each entry's `id`, names and `aliases`. To support a new model, add entries (or aliases)
for its labels; no code changes are needed. Labels without an entry still work: the UI shows a
prettified label and a note that detailed info isn't available.

`KnowledgeBaseTest` checks the JSON (unique ids, valid hosts, required fields), so run the unit tests after editing it.

## Converting a model

```python
import tensorflow as tf
model = tf.keras.models.load_model("plant_identifier.keras")
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]   # optional: smaller and faster
open("plant_identifier.tflite", "wb").write(converter.convert())
```

PyTorch models can be exported with [ai-edge-torch](https://github.com/google-ai-edge/ai-edge-torch).

Good starting datasets: **PlantVillage** (38 crop-disease classes, PlantVillage label format),
**PlantDoc** (real-world disease photos), **PlantNet-300K** and **iNaturalist** (species).

## Performance notes

- Models are memory-mapped from uncompressed assets (`noCompress += "tflite"` in Gradle).
- Interpreters load lazily on first scan and use 4 CPU threads with XNNPACK.
- For GPU / NNAPI acceleration, add the delegate in `TfLiteImageClassifier.load()`.
