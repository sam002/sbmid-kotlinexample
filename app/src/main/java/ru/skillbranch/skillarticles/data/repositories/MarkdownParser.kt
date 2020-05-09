package ru.skillbranch.skillarticles.data.repositories

import java.lang.StringBuilder
import java.util.regex.Pattern

object MarkdownParser {

    private val LINE_SEPARATOR = "\n"

    //group regex
    private const val UNORDERED_LIST_ITEM_GROUP = "(^[*+-]\\s.+?$)"
    private const val HEADER_GROUP = "(^#{1,6}\\s.+?$)"
    private const val QUOTE_GROUP = "(^>\\s.+?$)"
    private const val ITALIC_GROUP = "((?<!\\*)\\*[^*].*?[^*]?\\*(?!\\*)|(?<!_)_[^_].*?[^_]?_(?!_))"
    private const val BOLD_GROUP ="((?<!\\*)\\*{2}[^*].*?[^*]?\\*{2}(?!\\*)|(?<!_)_{2}[^_].*?[^_]?_{2}(?!_))"
    private const val STRIKE_GROUP = "((?<!\\~)\\~{2}[^\\~].*?[^\\~]?\\~{2}(?!\\~))"
    private const val RULE_GROUP = "(^[-_*]{3}$)"
    private const val INLINE_GROUP = "((?<!`)`[^\\s`].*?[^\\s`]?`(?!`))"
    private const val LINK_GROUP = "((?<!\\[)\\[[^\\[].*?[^\\]]?\\]\\([^\\(].*?[^\\)]?\\))"
    private const val BLOCK_CODE_GROUP = "(^`{3}[^`][^\$]+?[^`]?`{3}\$)"
    private const val ORDER_LIST_GROUP = "(^\\d+\\.\\s.+?$)"
    private const val IMAGE_GROUP = "(^!\\[[^\\[\\]]*?\\]\\(.+?\\)$)"

    //result regex
    private const val MARKDOWN_GROUPS = "$UNORDERED_LIST_ITEM_GROUP|$HEADER_GROUP|$QUOTE_GROUP" +
            "|$ITALIC_GROUP|$BOLD_GROUP|$STRIKE_GROUP|$RULE_GROUP|$INLINE_GROUP|$LINK_GROUP" +
            "|$BLOCK_CODE_GROUP|$ORDER_LIST_GROUP|$IMAGE_GROUP"// optionally

    private val elementsPattern by lazy { Pattern.compile(MARKDOWN_GROUPS, Pattern.MULTILINE) }

    /**
     * parse markdown text to elements
     */
    fun parse(string: String): List<MarkdownElement> {
        val elements = mutableListOf<Element>()
        elements.addAll(findElements(string))
        return elements.fold(mutableListOf()) { acc, element ->
            val last = acc.lastOrNull()
            when (element) {
                is Element.Image -> acc.add(
                    MarkdownElement.Image(
                        element,
                        last?.bounds?.second ?: 0
                    )
                )
                is Element.BlockCode -> acc.add(
                    MarkdownElement.Scroll(
                        element,
                        last?.bounds?.second ?: 0
                    )
                )
                else -> {
                    if (last is MarkdownElement.Text) last.elements.add(element)
                    else acc.add(
                        MarkdownElement.Text(
                            mutableListOf(element),
                            last?.bounds?.second ?: 0
                        )
                    )
                }

            }
            acc
        }
    }

    /**
     * clear markdown text to string without markdown characters
     */
//    fun clear(string: String?): String? {
//        if (string.isNullOrEmpty()) return null
//        val markdownText =
//            parse(
//                string
//            )
//        var result = ""
//        markdownText.elements.forEach {
//            result += it.text
//        }
//
//        return if(markdownText.elements.count() == 1) {
//            result
//        } else {
//            clear(
//                result
//            )
//        }
//    }

    /**
     * find markdown elements in markdown text
     */
    private fun findElements(string: CharSequence): List<Element> {
        val parents = mutableListOf<Element>()
        val matcher = elementsPattern.matcher(string)
        var lastIndex = 0

        loop@ while (matcher.find(lastIndex)) {
            val startIndex = matcher.start()
            val endIndex = matcher.end()

            if(lastIndex<startIndex) {
                parents.add(
                    Element.Text(
                        string.subSequence(lastIndex, startIndex)
                    )
                )
            }

            var text : CharSequence

            //groups range for iterate by groups (1..9) or (1..11) optionally
            val groups = 1..12

            var group = -1
            for (gr in groups) {
                if (matcher.group(gr) != null) {
                    group = gr
                    break
                }
            }
            when (group) {
                //NOT FOUND -> BREAK
                -1 -> break@loop

                //UNORDERED LIST
                1 -> {
                    //text without "*. "
                    text = string.subSequence(startIndex.plus(2), endIndex)

                    val subs =
                        findElements(
                            text
                        )
                    val element =
                        Element.UnorderedListItem(
                            text,
                            subs
                        )
                    parents.add(element)

                    lastIndex = endIndex
                }

                //HEADER
                2 -> {
                    //text without "{#} "

                    val subText = string.subSequence(startIndex, startIndex+6)
                    val rex = Regex("^#{1,6}").find(subText)
                    val level = rex!!.value.length
                    text = string.subSequence(startIndex.plus(level.inc()), endIndex)

                    val element =
                        Element.Header(
                            level,
                            text
                        )
                    parents.add(element)

                    lastIndex = endIndex
                }

                //QUOTE
                3 -> {
                    //text without "> "
                    text = string.subSequence(startIndex.plus(2), endIndex)
                    val subelements =
                        findElements(
                            text
                        )

                    val element =
                        Element.Quote(
                            text,
                            subelements
                        )
                    parents.add(element)

                    lastIndex = endIndex
                }

                //ITALIC
                4 -> {
                    //text without "*{}*||_{}_"
                    text = string.subSequence(startIndex.plus(1), endIndex.minus(1))
                    val subelements =
                        findElements(
                            text
                        )

                    val element =
                        Element.Italic(
                            text,
                            subelements
                        )
                    parents.add(element)

                    lastIndex = endIndex
                }

                //BOLD
                5 -> {
                    //text without "**{}**||__{}__"
                    text = string.subSequence(startIndex.plus(2), endIndex.minus(2))
                    val subelements =
                        findElements(
                            text
                        )

                    val element =
                        Element.Bold(
                            text,
                            subelements
                        )
                    parents.add(element)

                    lastIndex = endIndex
                }

                //STRIKE
                6 -> {
                    //text without "~~{}~~"
                    text = string.subSequence(startIndex.plus(2), endIndex.minus(2))
                    val subelements =
                        findElements(
                            text
                        )

                    val element =
                        Element.Strike(
                            text,
                            subelements
                        )
                    parents.add(element)

                    lastIndex = endIndex
                }

                //RULE
                7 -> {
                    //text without "***" insert empty character

                    val element =
                        Element.Rule(
                            " "
                        )
                    parents.add(element)

                    lastIndex = endIndex
                }

                //INLINE CODE
                8 -> {
                    //text without "`{}`"
                    text = string.subSequence(startIndex.plus(1), endIndex.minus(1))

                    val element =
                        Element.InlineCode(text)
                    parents.add(element)

                    lastIndex = endIndex
                }

                //LINK
                9 -> {
                    //full text for regex
                    val subText = string.subSequence(startIndex, endIndex)
//                    val rex = Regex("(?<!\\[)\\[([^\\[].*?[^]]?)\\]\\(([^(].*?[^)]?)\\)(?!\\))").find(subText)
//                    val groupsLink = rex!!.groups
//                    val link = groupsLink[2]!!.value
//                    title = groupsLink[1]!!.value

                    val(title:String, link:String) = Regex("\\[(.*)\\]\\((.*)\\)").find(subText)!!.destructured

                    val element =
                        Element.Link(
                            link,
                            title
                        )
                    parents.add(element)

                    lastIndex = endIndex
                }
                //10 -> BLOCK CODE - optionally
                10 -> {
                    text = string.subSequence(startIndex.plus(3), endIndex.minus(3))
                    val element = Element.BlockCode(text)
                    parents.add(element)
                    lastIndex = endIndex
                }

                //11 -> NUMERIC LIST
                11 -> {

                    val(order:String, textList:String) = Regex("^(\\d+\\.)\\s(.+)$")
                        .find(string.subSequence(startIndex, endIndex))!!.destructured
                    val subelements =
                        findElements(
                            textList
                        )

                    val element =
                        Element.OrderedListItem(
                            order,
                            textList,
                            subelements
                        )
                    parents.add(element)

                    lastIndex = endIndex
                }

                //12 -> IMAGE
                12 -> {
                    text = string.subSequence(startIndex, endIndex)

                    val (alt, url, title) = Regex("^!\\[([^\\[\\]]*?)?]\\((.*?)\\s\"(.*?)\"\\)$").find(text)!!.destructured

                    val element = Element.Image(url, if(alt.isBlank()) null else alt, title)
                    parents.add(element)

                    lastIndex = endIndex
                }
            }

        }

        if (lastIndex < string.length) {
            val text = string.subSequence(lastIndex, string.length)
            parents.add(
                Element.Text(
                    text
                )
            )
        }

        return parents
    }
}

data class MarkdownText(val elements: List<Element>)

sealed class MarkdownElement() {
    abstract val offset: Int
    val bounds: Pair<Int, Int> by lazy {
        when(this) {
            is Text -> {
                val end = elements.fold(offset) { acc, el ->
                    acc + el.spread().map { it.text.length }.sum()
                }
                offset to end
            }
            is Image -> offset to image.text.length + offset
            is Scroll -> offset to blockCode.text.length + offset
        }
    }

    data class Text(
        val elements: MutableList<Element>,
        override val offset: Int = 0
    ): MarkdownElement()

    data class Image(
        val image: Element.Image,
        override val offset: Int = 0
    ): MarkdownElement()

    data class Scroll(
        val blockCode: Element.BlockCode,
        override val offset: Int = 0
    ): MarkdownElement()
}

sealed class Element() {
    abstract val text: CharSequence
    abstract val elements: List<Element>

    data class Text(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class UnorderedListItem(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Header(
        val level: Int = 1,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Quote(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Italic(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Bold(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Strike(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Rule(
        override val text: CharSequence = " ", //for insert span
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class InlineCode(
        override val text: CharSequence, //for insert span
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Link(
        val link: String,
        override val text: CharSequence, //for insert span
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class OrderedListItem(
        val order: String,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class BlockCode(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Image(
        val url: String,
        val alt: String?,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()
}

private fun Element.spread() : List<Element> {
    val elements = mutableListOf<Element>()
    if(this.elements.isNotEmpty()) elements.addAll(this.elements.spread())
    else elements.add(this)

    return elements
}

private fun List<Element>.spread(): List<Element> {
    val elements = mutableListOf<Element>()
    forEach { elements.addAll(it.spread()) }
    return elements
}

private fun Element.clearContent() : String {
    return StringBuilder().apply {
        val element = this@clearContent
        if(element.elements.isEmpty()) append(element.text)
        else element.elements.forEach{ append(it.clearContent())}
    }.toString()
}

fun List<MarkdownElement>.clearContent(): String {
    return StringBuilder().apply {
        this@clearContent.forEach {
            when(it) {
                is MarkdownElement.Text -> it.elements.forEach { el -> append(el.clearContent()) }
                is MarkdownElement.Image -> append(it.image.clearContent())
                is MarkdownElement.Scroll -> append(it.blockCode.clearContent())
            }
        }
    }.toString()
}
