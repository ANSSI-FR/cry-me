/*************************** The CRY.ME project (2023) *************************************************
 *
 *  This file is part of the CRY.ME project (https://github.com/ANSSI-FR/cry-me).
 *  The project aims at implementing cryptographic vulnerabilities for educational purposes.
 *  Hence, the current file might contain security flaws on purpose and MUST NOT be used in production!
 *  Please do not use this source code outside this scope, or use it knowingly.
 *
 *  Many files come from the Android element (https://github.com/vector-im/element-android), the
 *  Matrix SDK (https://github.com/matrix-org/matrix-android-sdk2) as well as the Android Yubikit
 *  (https://github.com/Yubico/yubikit-android) projects and have been willingly modified
 *  for the CRY.ME project purposes. The Android element, Matrix SDK and Yubikit projects are distributed
 *  under the Apache-2.0 license, and so is the CRY.ME project.
 *
 ***************************  (END OF CRY.ME HEADER)   *************************************************/

/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.html

import android.content.Context
import android.text.Spannable
import androidx.core.text.toSpannable
import im.vector.app.core.resources.ColorProvider
import im.vector.app.features.settings.VectorPreferences
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonPlugin
import io.noties.markwon.PrecomputedFutureTextSetterCompat
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.latex.JLatexMathTheme
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import org.commonmark.node.Node
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventHtmlRenderer @Inject constructor(
        htmlConfigure: MatrixHtmlPluginConfigure,
        context: Context,
        vectorPreferences: VectorPreferences
) {

    interface PostProcessor {
        fun afterRender(renderedText: Spannable)
    }

    private val builder = Markwon.builder(context)
            .usePlugin(HtmlPlugin.create(htmlConfigure))

    private val markwon = if (vectorPreferences.latexMathsIsEnabled()) {
        builder
                .usePlugin(object : AbstractMarkwonPlugin() { // Markwon expects maths to be in a specific format: https://noties.io/Markwon/docs/v4/ext-latex
                    override fun processMarkdown(markdown: String): String {
                        return markdown
                                .replace(Regex("""<span\s+data-mx-maths="([^"]*)">.*?</span>""")) {
                                    matchResult -> "$$" + matchResult.groupValues[1] + "$$"
                                }
                                .replace(Regex("""<div\s+data-mx-maths="([^"]*)">.*?</div>""")) {
                                    matchResult -> "\n$$\n" + matchResult.groupValues[1] + "\n$$\n"
                                }
                    }
                })
                .usePlugin(MarkwonInlineParserPlugin.create())
                .usePlugin(JLatexMathPlugin.create(44F) { builder ->
                    builder.inlinesEnabled(true)
                    builder.theme().inlinePadding(JLatexMathTheme.Padding.symmetric(24, 8))
                })
    } else {
        builder
    }.textSetter(PrecomputedFutureTextSetterCompat.create()).build()

    val plugins: List<MarkwonPlugin> = markwon.plugins

    fun parse(text: String): Node {
        return markwon.parse(text)
    }

    /**
     * @param text the text you want to render
     * @param postProcessors an optional array of post processor to add any span if needed
     */
    fun render(text: String, vararg postProcessors: PostProcessor): CharSequence {
        return try {
            val parsed = markwon.parse(text)
            renderAndProcess(parsed, postProcessors)
        } catch (failure: Throwable) {
            Timber.v("Fail to render $text to html")
            text
        }
    }

    /**
     * @param node the node you want to render
     * @param postProcessors an optional array of post processor to add any span if needed
     */
    fun render(node: Node, vararg postProcessors: PostProcessor): CharSequence? {
        return try {
            renderAndProcess(node, postProcessors)
        } catch (failure: Throwable) {
            Timber.v("Fail to render $node to html")
            return null
        }
    }

    private fun renderAndProcess(node: Node, postProcessors: Array<out PostProcessor>): CharSequence {
        val renderedText = markwon.render(node).toSpannable()
        postProcessors.forEach {
            it.afterRender(renderedText)
        }
        return renderedText
    }
}

class MatrixHtmlPluginConfigure @Inject constructor(private val colorProvider: ColorProvider) : HtmlPlugin.HtmlConfigure {

    override fun configureHtml(plugin: HtmlPlugin) {
        plugin
                .addHandler(FontTagHandler())
                .addHandler(MxReplyTagHandler())
                .addHandler(SpanHandler(colorProvider))
    }
}
