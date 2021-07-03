package org.gotson.komga.infrastructure.web

import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import java.net.URL
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.io.path.pathString
import kotlin.io.path.toPath

fun URL.toFilePath(): String =
  this.toURI().toPath().pathString

fun filePathToUrl(filePath: String): URL =
  Paths.get(filePath).toUri().toURL()

fun ResponseEntity.BodyBuilder.setCachePrivate() =
  this.cacheControl(cachePrivate)

val cachePrivate = CacheControl
  .maxAge(0, TimeUnit.SECONDS)
  .noTransform()
  .cachePrivate()
  .mustRevalidate()

fun getMediaTypeOrDefault(mediaTypeString: String?): MediaType {
  mediaTypeString?.let {
    try {
      return MediaType.parseMediaType(mediaTypeString)
    } catch (ex: Exception) {
    }
  }
  return MediaType.APPLICATION_OCTET_STREAM
}
