@file:OptIn(ExperimentalTestApi::class)

package br.com.colman.petals

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.AndroidComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runAndroidComposeUiTest
import br.com.colman.kotest.FunSpec
import br.com.colman.petals.use.io.UseImporter
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FileDataPart
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale

val locales = listOf(
  "es" to "ES",
  "de" to "DE",
  "fr" to "FR",
  "it" to "IT",
  "nl" to "NL",
  "no" to "NO",
  "pt" to "BR",
  "ru" to "RU"
)

class ScreenshotTakerTest : FunSpec({

  test("ScreenShot 1") {
    runAndroidComposeUiTest<MainActivity> {
      activity!!.importDemoUses()

      locales.forEach { (lang, country) ->
        activity?.setLocale(Locale(lang, country))

        takeScreenshot("1.png", lang, country)
      }
    }
  }
})

private fun MainActivity.importDemoUses() {
  val useImporter by koin.inject<UseImporter>()

  val millisFromLastUseToToday = ChronoUnit.MILLIS.between(
    LocalDateTime.parse("2024-05-31T22:36:48.184"),
    LocalDateTime.now().minusMinutes(273)
  )

  useImporter.import(assets.open("HeavyUse.csv").readAllBytes().decodeToString().split("\n")) {
    it.copy(date = it.date.plus(millisFromLastUseToToday, ChronoUnit.MILLIS))
  }.getOrThrow()
}

private fun MainActivity.setLocale(locale: Locale) {
  val resources = baseContext.resources
  Locale.setDefault(locale)
  val config = resources.configuration
  config.setLocale(locale)
  resources.updateConfiguration(config, resources.displayMetrics)
  runOnUiThread { recreate() }
}

private fun AndroidComposeUiTest<*>.takeScreenshot(file: String, lang: String, country: String) {
  val bitmap = onRoot()
    .captureToImage()
    .asAndroidBitmap()

  uploadScreenshot(bitmap, file, lang, country)
}

private fun uploadScreenshot(bitmap: Bitmap, fileName: String, lang: String, country: String) {
  val byteArrayOutputStream = ByteArrayOutputStream()
  bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
  val byteArray = byteArrayOutputStream.toByteArray()

  // Save to a temporary file
  val tempFile = File.createTempFile(fileName, null)
  tempFile.writeBytes(byteArray)

  println("$lang$country")
  Fuel.upload("http://10.0.2.2:8080/upload?country=$country&lang=$lang")
    .add(FileDataPart(tempFile, name = "file", filename = fileName))
    .response { _, _, result ->
      result.fold(
        { println("File uploaded successfully.") },
        { error -> println("Failed to upload file: ${error.message}") }
      )
    }
}
