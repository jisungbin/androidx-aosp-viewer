package land.sungbin.androidx.fetcher

import androidx.compose.runtime.Immutable
import okio.ByteString.Companion.decodeBase64

@Immutable
@JvmInline
public value class GitBlob(public val content: String) {
  public companion object {
    public fun decode(encoded: String): Result<GitBlob> {
      val decoded = encoded.decodeBase64()
        ?: return Result.failure(NullPointerException("Something is wrong with the provided Base64."))
      return Result.success(GitBlob(decoded.utf8()))
    }
  }
}