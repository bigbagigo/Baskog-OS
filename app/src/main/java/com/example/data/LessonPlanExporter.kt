package com.example.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.ui.FormativeQuizResult
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object LessonPlanExporter {

    private fun formatMarkdownToHtml(text: String): String {
        var processedText = text
        // Replace bold markdown **text** with <b>text</b>
        val boldRegex = """\*\*(.*?)\*\*""".toRegex()
        processedText = boldRegex.replace(processedText) { matchResult ->
            "<b>${matchResult.groupValues[1]}</b>"
        }
        // Replace italic markdown *text* with <i>text</i> if any
        val italicRegex = """\*(.*?)\*""".toRegex()
        processedText = italicRegex.replace(processedText) { matchResult ->
            "<i>${matchResult.groupValues[1]}</i>"
        }

        val lines = processedText.split("\n")
        val htmlBuilder = java.lang.StringBuilder()
        for (line in lines) {
            val trimmed = line.replace("\r", "").trim()
            if (trimmed.isEmpty()) {
                htmlBuilder.append("<p style='margin: 4px 0; min-height: 8px;'></p>")
                continue
            }
            
            if (trimmed.startsWith("-") || trimmed.startsWith("*") || trimmed.startsWith("•")) {
                val bulletChar = if (trimmed.startsWith("-")) "-" else if (trimmed.startsWith("•")) "•" else "*"
                val bulletText = trimmed.substringAfter(bulletChar).trim()
                htmlBuilder.append("<p style='margin: 4px 0 4px 20px; text-indent: -12px; font-size: 14px; color: #1E293B;'>• $bulletText</p>")
            } else if (trimmed.firstOrNull()?.isDigit() == true && trimmed.contains(".")) {
                htmlBuilder.append("<p style='margin: 10px 0 4px 0; font-weight: bold; font-size: 14px; color: #0F172A;'>$trimmed</p>")
            } else {
                htmlBuilder.append("<p style='margin: 4px 0; font-size: 14px; color: #1E293B;'>$trimmed</p>")
            }
        }
        return htmlBuilder.toString()
    }

    fun exportToHtml(context: Context, plan: LessonPlan) {
        val fileName = "ILAW_Plan_${plan.subject.replace(" ", "_")}_Grade${plan.gradeLevel}.doc"
        
        val formattedIntentions = formatMarkdownToHtml(plan.intentions)
        val formattedExperiences = formatMarkdownToHtml(plan.learningExperiences)
        val formattedAssessment = formatMarkdownToHtml(plan.assessment)
        val formattedWaysForward = formatMarkdownToHtml(plan.waysForward)

        val htmlContent = """
            <html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:w='urn:schemas-microsoft-com:office:word' xmlns='http://www.w3.org/TR/REC-html40'>
            <head>
                <meta charset="utf-8">
                <!--[if gte mso 9]>
                <xml>
                <w:WordDocument>
                <w:View>Print</w:View>
                <w:Zoom>100</w:Zoom>
                <w:DoNotOptimizeForBrowser/>
                </w:WordDocument>
                </xml>
                <![endif]-->
                <title>BASKOG OS — ILAW LESSON BLUEPRINT</title>
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #1E293B; margin: 40px; background-color: #FAFAFA; }
                    .container { max-width: 800px; margin: 0 auto; background-color: #FFFFFF; padding: 40px; border: 1px solid #E2E8F0; border-radius: 8px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); }
                    h1 { color: #3B82F6; border-bottom: 3px solid #3B82F6; padding-bottom: 10px; font-size: 26px; margin-top: 0; }
                    h2 { color: #0F172A; background-color: #F1F5F9; padding: 10px 16px; font-size: 16px; margin-top: 30px; border-left: 4px solid #3B82F6; border-radius: 4px; font-weight: bold; }
                    p { margin: 12px 0; font-size: 14px; }
                    .meta-table { width: 100%; border-collapse: collapse; margin-top: 20px; margin-bottom: 25px; }
                    .meta-table td { padding: 10px; border: 1px solid #CBD5E1; font-size: 13px; }
                    .meta-header { background-color: #F8FAFC; font-weight: bold; width: 180px; color: #475569; }
                    .footer { border-top: 1px solid #E2E8F0; margin-top: 50px; padding-top: 20px; font-size: 11px; color: #64748B; text-align: center; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>BASKOG OS — ILAW LESSON BLUEPRINT</h1>
                    <table class="meta-table">
                        <tr>
                            <td class="meta-header">Subject Title</td>
                            <td style="font-weight: bold; color: #1E293B;">${plan.subject}</td>
                        </tr>
                        <tr>
                            <td class="meta-header">Key Stage / Grade</td>
                            <td>${plan.gradeLevel} ${if (plan.specificGradeLevel.isNotEmpty()) "(${plan.specificGradeLevel})" else ""}</td>
                        </tr>
                        <tr>
                            <td class="meta-header">Term & Week</td>
                            <td>${plan.term} Block, Week ${plan.week}</td>
                        </tr>
                        <tr>
                            <td class="meta-header">Duration</td>
                            <td>${plan.durationMins} Minutes</td>
                        </tr>
                        <tr>
                            <td class="meta-header">Method of Instruction</td>
                            <td>${plan.teachingStrategy}</td>
                        </tr>
                        <tr>
                            <td class="meta-header">Language Focus</td>
                            <td>${plan.language}</td>
                        </tr>
                        <tr>
                            <td class="meta-header">Active EiE Protocol</td>
                            <td><span style="font-weight: bold; color: #EF4444;">${plan.eieLevel}</span></td>
                        </tr>
                        <tr>
                            <td class="meta-header">PPST Checklist Target</td>
                            <td>${plan.ppstChecklist}</td>
                        </tr>
                    </table>

                    <h2>Part I - INTENTIONS (OBJECTIVES & CONTENT STANDARD)</h2>
                    <div>$formattedIntentions</div>

                    <h2>Part II - LEARNING EXPERIENCES (CLASS PROCEDURES)</h2>
                    <div>$formattedExperiences</div>

                    <h2>Part III - ASSESSMENT (PACE METHOD / DEPED WEIGHTS)</h2>
                    <div>$formattedAssessment</div>

                    <h2>Part IV - WAYS FORWARD (REMEDIATION & ENRICHMENT)</h2>
                    <div>$formattedWaysForward</div>
                    
                    <div class="footer">
                        BASKOG Localized Educational Operating System • Aligned to DepEd Three-Term Year Regulations
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        try {
            saveFileToDownloads(context, fileName, "application/msword", htmlContent.toByteArray(Charsets.UTF_8))
            Toast.makeText(context, "Lesson Plan MS Word document exported to Downloads", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Word Export Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private class PdfPageRenderer(val pdfDocument: PdfDocument) {
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(612, 792, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        var yPos = 50f

        fun checkPageOverflow(neededHeight: Float = 14f) {
            if (yPos + neededHeight > 730f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(612, 792, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPos = 50f
            }
        }

        fun finish() {
            try {
                pdfDocument.finishPage(page)
            } catch (e: Exception) {
                // Already finished or error
            }
        }
    }

    fun exportToPdf(context: Context, plan: LessonPlan) {
        val fileName = "ILAW_Plan_${plan.subject.replace(" ", "_")}_Grade${plan.gradeLevel}.pdf"
        val pdfDocument = PdfDocument()
        val renderer = PdfPageRenderer(pdfDocument)
        
        val paint = Paint()
        val textPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 9.5f
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#3B82F6") // Blue prefix
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val sectionTitlePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#0F172A")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // Draw header on Page 1
        renderer.canvas.drawText("BASKOG OS — ILAW LESSON BLUEPRINT", 40f, renderer.yPos, titlePaint)
        renderer.yPos += 26f
        
        paint.color = android.graphics.Color.parseColor("#3B82F6")
        paint.strokeWidth = 1.5f
        renderer.canvas.drawLine(40f, renderer.yPos, 572f, renderer.yPos, paint)
        renderer.yPos += 20f

        val metaLines = listOf(
            "Subject: ${plan.subject}",
            "Key Stage / Grade: ${plan.gradeLevel} ${if (plan.specificGradeLevel.isNotEmpty()) "(${plan.specificGradeLevel})" else ""}",
            "Term & Week: ${plan.term}, Week ${plan.week}",
            "Duration: ${plan.durationMins} Mins | Language: ${plan.language}",
            "Strategy: ${plan.teachingStrategy} | EiE Level: ${plan.eieLevel}",
            "PPST Checklist: ${plan.ppstChecklist}"
        )

        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        for (line in metaLines) {
            renderer.checkPageOverflow(16f)
            renderer.canvas.drawText(line, 45f, renderer.yPos, textPaint)
            renderer.yPos += 16f
        }
        renderer.yPos += 15f

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 9.5f

        fun drawSection(title: String, body: String) {
            renderer.checkPageOverflow(30f)
            renderer.canvas.drawText(title, 40f, renderer.yPos, sectionTitlePaint)
            renderer.yPos += 16f
            
            // Remove asterisk markers completely for pristine presentation
            val cleanedBody = body.replace("**", "").replace("*", "")
            
            val lines = cleanedBody.split("\n")
            for (paragraphLine in lines) {
                val trimmedLine = paragraphLine.replace("\r", "").trimEnd()
                if (trimmedLine.isEmpty()) {
                    renderer.checkPageOverflow(8f)
                    renderer.yPos += 8f
                    continue
                }
                
                // Formulate margins for bullet points if they start with a symbol
                val isBullet = trimmedLine.startsWith("-") || trimmedLine.startsWith("•") || trimmedLine.startsWith("*")
                val indentX = if (isBullet) 60f else 45f
                
                val words = trimmedLine.split(" ")
                var currentLineSegment = ""
                for (word in words) {
                    val testLine = if (currentLineSegment.isEmpty()) word else "$currentLineSegment $word"
                    val width = textPaint.measureText(testLine)
                    if (width > (500f - (if (isBullet) 15f else 0f))) {
                        renderer.checkPageOverflow(14f)
                        renderer.canvas.drawText(currentLineSegment, indentX, renderer.yPos, textPaint)
                        renderer.yPos += 14f
                        currentLineSegment = word
                    } else {
                        currentLineSegment = testLine
                    }
                }
                if (currentLineSegment.isNotEmpty()) {
                    renderer.checkPageOverflow(14f)
                    renderer.canvas.drawText(currentLineSegment, indentX, renderer.yPos, textPaint)
                    renderer.yPos += 14f
                }
            }
            renderer.yPos += 12f // Margin below section
        }

        drawSection("I - INTENTIONS (OBJECTIVES & CONTENT STANDARD)", plan.intentions)
        drawSection("II - LEARNING EXPERIENCES (CLASS PROCEDURES)", plan.learningExperiences)
        drawSection("III - ASSESSMENT (PACE METHOD / DEPED WEIGHTS)", plan.assessment)
        drawSection("IV - WAYS FORWARD (REMEDIATION & ENRICHMENT)", plan.waysForward)

        renderer.finish()

        try {
            val outputStream = getDownloadOutputStream(context, fileName, "application/pdf")
            if (outputStream != null) {
                pdfDocument.writeTo(outputStream)
                outputStream.close()
                Toast.makeText(context, "PDF exported successfully to Downloads folder", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Could not open Output Stream for PDF", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "PDF Export Failed: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            pdfDocument.close()
        }
    }

    fun exportClassRecordToCsv(context: Context, record: FormativeQuizResult) {
        val fileName = "Class_Record_Quiz_${record.id}_${record.timestamp.replace(" ", "_").replace(":", "-")}.csv"
        val csvBuilder = java.lang.StringBuilder()
        
        // CSV Header
        csvBuilder.append("\"BASKOG OS — LOCALIZED CLASSROOM QUIZ RECORD\"\n")
        csvBuilder.append("\"Question ID:\",\"${record.id}\"\n")
        csvBuilder.append("\"Timestamp:\",\"${record.timestamp}\"\n")
        csvBuilder.append("\"Question:\",\"${record.questionText.replace("\"", "\"\"")}\"\n")
        csvBuilder.append("\"Type:\",\"${record.questionType}\"\n")
        csvBuilder.append("\"Correct Answer:\",\"${record.correctAnswer.replace("\"", "\"\"")}\"\n\n")
        
        csvBuilder.append("\"STUDENT COHORT ROSTER RESULTS (DEPED TTY PACE)\"\n")
        csvBuilder.append("\"Student Name\",\"Has Answered\",\"Answer Given\",\"Is Correct\",\"Score Earned (Transitional 75% Floor Applied)\"\n")
        
        for (student in record.studentsList) {
            val answered = if (student.hasAnswered) "Yes" else "No"
            val correct = if (student.isCorrect) "Yes" else "No"
            val score = student.score
            csvBuilder.append("\"${student.studentName.replace("\"", "\"\"")}\",\"$answered\",\"${student.answer.replace("\"", "\"\"")}\",\"$correct\",\"$score\"\n")
        }
        
        try {
            saveFileToDownloads(context, fileName, "text/csv", csvBuilder.toString().toByteArray(Charsets.UTF_8))
            Toast.makeText(context, "Class records CSV exported to Downloads", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Export Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun printLessonPlan(context: Context, plan: LessonPlan) {
        val mainHandler = android.os.Handler(context.mainLooper)
        mainHandler.post {
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val printAdapter = webView.createPrintDocumentAdapter("ILAW_Plan_${plan.subject.replace(" ", "_")}")
                    val jobName = "ILAW_Plan_${plan.subject.replace(" ", "_")}"
                    printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
                }
            }
            
            val formattedIntentions = formatMarkdownToHtml(plan.intentions)
            val formattedExperiences = formatMarkdownToHtml(plan.learningExperiences)
            val formattedAssessment = formatMarkdownToHtml(plan.assessment)
            val formattedWaysForward = formatMarkdownToHtml(plan.waysForward)

            val htmlContent = """
                <html>
                <head>
                    <meta charset="utf-8">
                    <style>
                        body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #1C1C1E; padding: 24px; background-color: #FFFFFF; }
                        h1 { color: #007AFF; border-bottom: 2px solid #007AFF; padding-bottom: 8px; font-size: 22px; margin-top: 0; }
                        h2 { color: #1C1C1E; background-color: #F2F2F7; padding: 8px 12px; font-size: 14px; margin-top: 24px; border-left: 4px solid #007AFF; font-weight: bold; }
                        p { margin: 8px 0; font-size: 12px; }
                        .meta-table { width: 100%; border-collapse: collapse; margin-top: 15px; margin-bottom: 15px; }
                        .meta-table td { padding: 8px 12px; border: 1px solid #E5E5EA; font-size: 11px; }
                        .meta-header { background-color: #F2F2F7; font-weight: bold; width: 150px; color: #636366; }
                        .footer { border-top: 1px solid #E5E5EA; margin-top: 40px; padding-top: 12px; font-size: 10px; color: #8E8E93; text-align: center; }
                    </style>
                </head>
                <body>
                    <h1>ILAW LESSON PLAN BLUEPRINT</h1>
                    <table class="meta-table">
                        <tr><td class="meta-header">Subject Title</td><td><b>${plan.subject}</b></td></tr>
                        <tr><td class="meta-header">Grade Level</td><td>${plan.gradeLevel} ${if (plan.specificGradeLevel.isNotEmpty()) "(${plan.specificGradeLevel})" else ""}</td></tr>
                        <tr><td class="meta-header">Term & Week</td><td>${plan.term}, Week ${plan.week}</td></tr>
                        <tr><td class="meta-header">Duration</td><td>${plan.durationMins} Minutes</td></tr>
                        <tr><td class="meta-header">Strategy</td><td>${plan.teachingStrategy}</td></tr>
                        <tr><td class="meta-header">Language</td><td>${plan.language}</td></tr>
                        <tr><td class="meta-header">EiE Protocol</td><td><span style="color:#FF3B30; font-weight:bold;">${plan.eieLevel}</span></td></tr>
                    </table>

                    <h2>Part I - INTENTIONS (OBJECTIVES & CONTENT STANDARD)</h2>
                    <div>$formattedIntentions</div>

                    <h2>Part II - LEARNING EXPERIENCES (CLASS PROCEDURES)</h2>
                    <div>$formattedExperiences</div>

                    <h2>Part III - ASSESSMENT (PACE METHOD / DEPED WEIGHTS)</h2>
                    <div>$formattedAssessment</div>

                    <h2>Part IV - WAYS FORWARD (REMEDIATION & ENRICHMENT)</h2>
                    <div>$formattedWaysForward</div>
                    
                    <div class="footer">
                        MARAGTASON OS Localized Educational Hypervisor • DepEd TTY Alignment
                    </div>
                </body>
                </html>
            """.trimIndent()
            
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
        }
    }

    private fun saveFileToDownloads(context: Context, fileName: String, mimeType: String, content: ByteArray) {
        val out = getDownloadOutputStream(context, fileName, mimeType)
        if (out != null) {
            out.write(content)
            out.close()
        } else {
            throw Exception("Could not open Output Stream")
        }
    }

    private fun getDownloadOutputStream(context: Context, fileName: String, mimeType: String): OutputStream? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) resolver.openOutputStream(uri) else null
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            FileOutputStream(file)
        }
    }
}
