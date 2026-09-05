package com.example.lottoinsight.feature.ui.analysis

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.example.lottoinsight.core.model.AnalysisResult
import java.io.File

internal fun copyAnalysisResultAsImage(context: Context, result: AnalysisResult): Boolean {
    var bitmap: Bitmap? = null

    return try {
        val renderedBitmap = AnalysisImageRenderer.render(result)
        bitmap = renderedBitmap

        val imageFile = File(
            context.cacheDir,
            "lotto_analysis_result/lotto_analysis_result.png"
        )
        imageFile.parentFile?.mkdirs()
        imageFile.outputStream().use { output ->
            check(renderedBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "Failed to encode lotto result image"
            }
        }
        val imageUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newUri(context.contentResolver, "추천 번호 결과 이미지", imageUri)
        )
        true
    } catch (_: Exception) {
        false
    } finally {
        bitmap?.takeUnless { it.isRecycled }?.recycle()
    }
}
