package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import java.io.InputStream
import java.util.Scanner
import java.util.regex.Pattern
import java.util.zip.ZipInputStream

object DocumentTextExtractor {

    /**
     * Unified extraction gateway: Tries native parsing first. If native extraction yields 
     * blank content (meaning the PDF is likely image-only or scanned), it automatically 
     * triggers on-device PDF page rendering + multimodal Gemini API OCR fallback.
     */
    suspend fun extractText(context: Context, uri: Uri): String {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: ""
        val isPdf = mimeType.contains("pdf", ignoreCase = true) || uri.path?.endsWith(".pdf", ignoreCase = true) == true

        var nativeResult = ""
        var nativeSuccess = false

        try {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                nativeResult = if (isPdf) {
                    extractTextFromPdfNative(inputStream)
                } else if (mimeType.contains("wordprocessingml", ignoreCase = true) || uri.path?.endsWith(".docx", ignoreCase = true) == true) {
                    extractTextFromDocx(inputStream)
                } else {
                    inputStream.bufferedReader().use { it.readText() }
                }
                nativeSuccess = true
            } else {
                nativeResult = "Error: Unable to open input stream."
            }
        } catch (e: Exception) {
            nativeResult = "Native Extraction Failed: ${e.localizedMessage}"
        }

        // Checks if native extraction returned zero content or a known textless warning
        val isTextless = nativeResult.isBlank() || 
                nativeResult.contains("Warning: Native text extraction yielded empty content", ignoreCase = true) ||
                nativeResult.contains("Native Extraction Failed", ignoreCase = true)

        if (isTextless && isPdf) {
            // Trigger advanced PDF page visual render and Gemini OCR
            return extractTextFromPdfWithOcr(context, uri)
        }

        return nativeResult
    }

    private fun extractTextFromPdfNative(inputStream: InputStream): String {
        val sb = StringBuilder()
        val scanner = Scanner(inputStream, "ISO-8859-1")
        while (scanner.hasNextLine()) {
            val line = scanner.nextLine()
            val matcher = Pattern.compile("\\(([^)]+)\\)\\s*(Tj|TJ)").matcher(line)
            while (matcher.find()) {
                val matched = matcher.group(1) ?: ""
                sb.append(matched).append(" ")
            }
            if (line.contains("BT") || line.contains("ET") || line.contains("T*")) {
                sb.append("\n")
            }
        }
        scanner.close()
        
        val raw = sb.toString()
            .replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\\\", "")
            
        return if (raw.isBlank()) {
            "Warning: Native text extraction yielded empty content. PDF might contain OCR images rather than raw text tags."
        } else {
            raw
        }
    }

    private fun extractTextFromDocx(inputStream: InputStream): String {
        val zipInputStream = ZipInputStream(inputStream)
        var entry = zipInputStream.nextEntry
        val sb = StringBuilder()
        
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                val bytes = zipInputStream.readBytes()
                val xmlString = String(bytes, Charsets.UTF_8)
                
                val matcher = Pattern.compile("<w:t[^>]*>(.*?)</w:t>").matcher(xmlString)
                while (matcher.find()) {
                    val txt = matcher.group(1) ?: ""
                    sb.append(txt).append(" ")
                }
                break
            }
            zipInputStream.closeEntry()
            entry = zipInputStream.nextEntry
        }
        zipInputStream.close()
        
        val raw = sb.toString().trim()
        return if (raw.isBlank()) {
            "Warning: Empty content extracted from word XML file."
        } else {
            raw
        }
    }

    /**
     * Converts a scanned PDF document's visual pages into high-resolution Bitmaps
     * and streams them with a custom extraction task directly to Gemini on a virtual background thread.
     */
    private suspend fun extractTextFromPdfWithOcr(context: Context, uri: Uri): String = kotlinx.coroutines.withContext(android.os.AsyncTask.THREAD_POOL_EXECUTOR.asCoroutineDispatcher()) {
        val parcelFD = try {
            context.contentResolver.openFileDescriptor(uri, "r")
        } catch (e: Exception) {
            null
        } ?: return@withContext "[Error: FAILED to open native ParcelFileDescriptor for OCR fallback.]"

        return@withContext try {
            val pdfRenderer = PdfRenderer(parcelFD)
            val pageCount = pdfRenderer.pageCount
            val sb = StringBuilder()
            
            // Limit OCR scanning to first 4 pages (more than enough for curriculum tables / checklists)
            val scanLimit = minOf(4, pageCount)
            
            for (i in 0 until scanLimit) {
                var page: PdfRenderer.Page? = null
                var bitmap: Bitmap? = null
                try {
                    page = pdfRenderer.openPage(i)
                    // Double density for crisp readability during model OCR
                    val width = page.width * 2
                    val height = page.height * 2
                    
                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE) // Ensure solid white background is behind the text rendering
                    
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    
                    // Trigger suspending OCR task on the active background flow
                    val speechTxt = GeminiClient.performGeminiOcr(bitmap)
                    sb.append("--- PAGE ${i+1} OCR READOUT ---\n")
                    sb.append(speechTxt).append("\n\n")
                } catch (pe: Exception) {
                    sb.append("--- PAGE ${i+1} RENDER ERROR: ${pe.localizedMessage} ---\n\n")
                } finally {
                    page?.close()
                    bitmap?.recycle()
                }
            }
            
            pdfRenderer.close()
            parcelFD.close()
            
            val finalized = sb.toString().trim()
            if (finalized.isBlank()) {
                "[Warning: Digital AI OCR pipeline completed successfully but rendered completely empty results.]"
            } else {
                finalized
            }
        } catch (e: Exception) {
            "[Exception executing PDF OCR Fallback: ${e.localizedMessage}]"
        }
    }
}

// Extension function to convert Executor to Coroutine Dispatcher
private fun java.util.concurrent.Executor.asCoroutineDispatcher(): kotlinx.coroutines.CoroutineDispatcher = 
    kotlinx.coroutines.Runnable::class.java.let { _ ->
        object : kotlinx.coroutines.CoroutineDispatcher() {
            override fun dispatch(context: kotlin.coroutines.CoroutineContext, block: Runnable) {
                execute(block)
            }
        }
    }
