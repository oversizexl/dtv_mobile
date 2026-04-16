@file:Suppress("PackageDirectoryMismatch")

package javax.lang.model

/**
 * Android does not ship `javax.lang.model.SourceVersion` (Java compiler API), but Rhino references it.
 *
 * Rhino expects this to be an enum (it calls `ordinal()`), so provide a minimal enum shim plus
 * the static identifier helper methods Rhino uses.
 */
enum class SourceVersion {
  RELEASE_0,
  RELEASE_1,
  RELEASE_2,
  RELEASE_3,
  RELEASE_4,
  RELEASE_5,
  RELEASE_6,
  RELEASE_7,
  RELEASE_8,
  RELEASE_9,
  RELEASE_10,
  RELEASE_11,
  RELEASE_12,
  RELEASE_13,
  RELEASE_14,
  RELEASE_15,
  RELEASE_16,
  RELEASE_17,
  ;

  companion object {
    private val KEYWORDS: Set<String> = setOf(
      "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
      "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
      "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
      "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
      "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
      "volatile", "while",
      // reserved literals (kept for compatibility with common identifier checks)
      "true", "false", "null",
    )

    @JvmStatic
    fun latestSupported(): SourceVersion = RELEASE_17

    @JvmStatic
    fun latest(): SourceVersion = RELEASE_17

    @JvmStatic
    fun isKeyword(name: CharSequence?): Boolean {
      val s = name?.toString() ?: return false
      return KEYWORDS.contains(s)
    }

    @JvmStatic
    fun isIdentifier(name: CharSequence?): Boolean {
      if (name == null || name.isEmpty()) return false
      val s = name.toString()
      if (isKeyword(s)) return false

      val first = s[0]
      if (!Character.isJavaIdentifierStart(first)) return false
      for (i in 1 until s.length) {
        if (!Character.isJavaIdentifierPart(s[i])) return false
      }
      return true
    }

    @JvmStatic
    fun isName(name: CharSequence?): Boolean {
      val s = name?.toString() ?: return false
      if (s.isBlank()) return false
      val parts = s.split('.')
      if (parts.any { it.isEmpty() }) return false
      return parts.all { isIdentifier(it) }
    }
  }
}
