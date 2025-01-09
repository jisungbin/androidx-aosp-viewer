// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package thirdparty

import org.jetbrains.annotations.NonNls
import java.io.PrintWriter
import java.io.StringWriter
import java.util.ArrayList
import java.util.Collections
import java.util.logging.Level
import java.util.logging.Logger
import java.util.regex.Pattern
import kotlin.collections.toList
import kotlin.collections.toTypedArray
import kotlin.jvm.java

/** Logging for lazy people. */
public sealed class Timber {
  /** A facade for handling logging calls. Install instances via [`Timber.plant()`][.plant]. */
  public abstract class Tree {
    internal val explicitTag = ThreadLocal<String>()

    internal open val tag: String?
      get() {
        val tag = explicitTag.get()
        if (tag != null) explicitTag.remove()
        return tag
      }

    /** Log a debug message with optional format args. */
    public open fun d(message: String?, vararg args: Any?) {
      prepareLog(Level.INFO, null, message, *args)
    }

    /** Log a debug exception and a message with optional format args. */
    public open fun d(t: Throwable?, message: String?, vararg args: Any?) {
      prepareLog(Level.INFO, t, message, *args)
    }

    /** Log a debug exception. */
    public open fun d(t: Throwable?) {
      prepareLog(Level.INFO, t, null)
    }

    /** Log a warning message with optional format args. */
    public open fun w(message: String?, vararg args: Any?) {
      prepareLog(Level.WARNING, null, message, *args)
    }

    /** Log a warning exception and a message with optional format args. */
    public open fun w(t: Throwable?, message: String?, vararg args: Any?) {
      prepareLog(Level.WARNING, t, message, *args)
    }

    /** Log a warning exception. */
    public open fun w(t: Throwable?) {
      prepareLog(Level.WARNING, t, null)
    }

    /** Log an error message with optional format args. */
    public open fun e(message: String?, vararg args: Any?) {
      prepareLog(Level.SEVERE, null, message, *args)
    }

    /** Log an error exception and a message with optional format args. */
    public open fun e(t: Throwable?, message: String?, vararg args: Any?) {
      prepareLog(Level.SEVERE, t, message, *args)
    }

    /** Log an error exception. */
    public open fun e(t: Throwable?) {
      prepareLog(Level.SEVERE, t, null)
    }

    /** Return whether a message at `level` or `tag` should be logged. */
    protected open fun isLoggable(tag: String?, level: Level): Boolean = true

    private fun prepareLog(level: Level, t: Throwable?, message: String?, vararg args: Any?) {
      // Consume tag even when message is not loggable so that next message is correctly tagged.
      val tag = tag
      if (!isLoggable(tag, level)) return

      var message = message
      if (message.isNullOrEmpty()) {
        if (t == null) return // Swallow message if it's null and there's no throwable.
        message = getStackTraceString(t)
      } else {
        if (args.isNotEmpty()) message = formatMessage(message, args)
        if (t != null) message += "\n" + getStackTraceString(t)
      }

      log(level, tag, message, t)
    }

    /** Formats a log message with optional arguments. */
    protected open fun formatMessage(message: String, args: Array<out Any?>): String =
      message.format(*args)

    private fun getStackTraceString(t: Throwable): String {
      val sw = StringWriter(256)
      val pw = PrintWriter(sw, false)
      t.printStackTrace(pw)
      pw.flush()
      return sw.toString()
    }

    /**
     * Write a log message to its destination. Called for all level-specific methods by default.
     *
     * @param level Log level. See [Level] for constants.
     * @param tag Explicit or inferred tag. May be `null`.
     * @param message Formatted log message.
     * @param t Accompanying exceptions. May be `null`.
     */
    protected open fun log(level: Level, tag: String?, message: String, t: Throwable?) {
      Logger.getLogger(tag).log(level, message, t)
    }
  }

  /** A [Tree] for debug builds. Automatically infers the tag from the calling class. */
  public open class DebugTree : Tree() {
    private val fqcnIgnore = listOf(
      Timber::class.java.name,
      Forest::class.java.name,
      Tree::class.java.name,
      DebugTree::class.java.name,
    )

    override val tag: String?
      get() =
        super.tag
          ?: Throwable().stackTrace
            .first { it.className !in fqcnIgnore }
            .let(::createStackElementTag)

    /**
     * Extract the tag which should be used for the message from the `element`. By default
     * this will use the class name without any anonymous class suffixes (e.g., `Foo$1`
     * becomes `Foo`).
     *
     * Note: This will not be called if a [manual tag][.tag] was specified.
     */
    protected open fun createStackElementTag(element: StackTraceElement): String? {
      var tag = element.className.substringAfterLast('.')
      val m = ANONYMOUS_CLASS.matcher(tag)
      if (m.find()) tag = m.replaceAll("")
      return tag
    }

    public companion object {
      private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")
    }
  }

  public companion object Forest : Tree() {
    // Both fields guarded by 'trees'.
    private val trees = ArrayList<Tree>()
    @Volatile private var treeArray = emptyArray<Tree>()

    public val treeCount: Int get() = treeArray.size

    /** Log a debug message with optional format args. */
    override fun d(@NonNls message: String?, vararg args: Any?) {
      treeArray.forEach { it.d(message, *args) }
    }

    /** Log a debug exception and a message with optional format args. */
    override fun d(t: Throwable?, @NonNls message: String?, vararg args: Any?) {
      treeArray.forEach { it.d(t, message, *args) }
    }

    /** Log a debug exception. */
    override fun d(t: Throwable?) {
      treeArray.forEach { it.d(t) }
    }

    /** Log a warning message with optional format args. */
    override fun w(@NonNls message: String?, vararg args: Any?) {
      treeArray.forEach { it.w(message, *args) }
    }

    /** Log a warning exception and a message with optional format args. */
    override fun w(t: Throwable?, @NonNls message: String?, vararg args: Any?) {
      treeArray.forEach { it.w(t, message, *args) }
    }

    /** Log a warning exception. */
    override fun w(t: Throwable?) {
      treeArray.forEach { it.w(t) }
    }

    /** Log an error message with optional format args. */
    override fun e(@NonNls message: String?, vararg args: Any?) {
      treeArray.forEach { it.e(message, *args) }
    }

    /** Log an error exception and a message with optional format args. */
    override fun e(t: Throwable?, @NonNls message: String?, vararg args: Any?) {
      treeArray.forEach { it.e(t, message, *args) }
    }

    /** Log an error exception. */
    override fun e(t: Throwable?) {
      treeArray.forEach { it.e(t) }
    }

    /** Set a one-time tag for use on the next logging call. */
    public fun tag(tag: String): Tree {
      for (tree in treeArray) {
        tree.explicitTag.set(tag)
      }
      return this
    }

    /** Add a new logging tree. */
    public fun plant(tree: Tree) {
      require(tree !== this) { "Cannot plant Timber into itself." }
      synchronized(trees) {
        trees.add(tree)
        treeArray = trees.toTypedArray()
      }
    }

    /** Adds new logging trees. */
    public fun plant(vararg trees: Tree) {
      for (tree in trees) {
        requireNotNull(tree) { "trees contained null" }
        require(tree !== this) { "Cannot plant Timber into itself." }
      }
      synchronized(this.trees) {
        Collections.addAll(this.trees, *trees)
        treeArray = this.trees.toTypedArray()
      }
    }

    /** Remove a planted tree. */
    public fun uproot(tree: Tree) {
      synchronized(trees) {
        require(trees.remove(tree)) { "Cannot uproot tree which is not planted: $tree" }
        treeArray = trees.toTypedArray()
      }
    }

    /** Remove all planted trees. */
    public fun uprootAll() {
      synchronized(trees) {
        trees.clear()
        treeArray = emptyArray()
      }
    }

    /** Return a copy of all planted [trees][Tree]. */
    public fun forest(): List<Tree> =
      synchronized(trees) { Collections.unmodifiableList(trees.toList()) }
  }
}
