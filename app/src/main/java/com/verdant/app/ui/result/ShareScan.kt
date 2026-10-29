package com.verdant.app.ui.result

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.verdant.app.R
import com.verdant.app.data.model.HealthStatus
import com.verdant.app.data.model.ScanRecord
import com.verdant.app.util.asPercent
import java.io.File

fun shareScan(context: Context, scan: ScanRecord) {
    val headline = if (scan.health == HealthStatus.DISEASED && scan.disease != null) {
        context.getString(R.string.result_share_diseased, scan.plant.commonName, scan.plant.scientificName, scan.disease.name)
    } else {
        context.getString(R.string.result_share_healthy, scan.plant.commonName, scan.plant.scientificName)
    }
    val text = buildString {
        appendLine(headline)
        appendLine("${scan.plantConfidence.asPercent()} ${context.getString(R.string.result_match)}")
        scan.disease?.treatment?.takeIf { scan.health == HealthStatus.DISEASED }?.forEachIndexed { i, step ->
            appendLine("${i + 1}. $step")
        }
        append(context.getString(R.string.result_share_footer))
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, text)
        val file = File(scan.imagePath)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "image/jpeg"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            type = "text/plain"
        }
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_share)))
}
