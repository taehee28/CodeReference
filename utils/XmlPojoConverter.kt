package parser

import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

object XmlPojoConverter {
    private const val TAB = "    "

    fun get(xmlString: String): String {
        runCatching {
            DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(InputSource(StringReader(xmlString)))
        }.getOrElse {
            return "Parsing error!\n${it.message}"
        }.also {
            return get(it.documentElement).toString()
        }
    }

    private fun get(xml: Element): Pojo {
        val name = xml.tagName
        val properties = mutableListOf<PropType>()

        // Attribute 찾기
        val attrs = xml.attributes
        for (i in 0 ..< attrs.length) {
            properties.add(PropType.Attribute(attrs.item(i).nodeName))
        }

        // Element와 PropertyElement 찾기
        val child = xml.childNodes
        val propertyElements = mutableListOf<PropType.PropertyElement>()
        val elements = mutableListOf<PropType.Element>()
        for (i in 0 ..< child.length) {
            (child.item(i) as? Element)?.also {
                val pojo = get(it)
                if (pojo.hasTextContentOnly) {
                    propertyElements.add(PropType.PropertyElement(pojo.name))
                } else {
                    elements.add(PropType.Element(pojo))
                }
            }
        }

        // 태그의 이름으로 중복인 PropertyElement들을 찾아냄
        propertyElements.groupBy { it.name }.values.forEach {
            if (it.size == 1) {
                properties.add(it.single())
            } else {
                properties.add(PropType.ElementList(it.first()))
            }
        }
        // pojo 클래스의 이름으로 중복인 Element들을 찾아냄
        elements.groupBy { it.pojo.name }.values.forEach {
            if (it.size == 1) {
                properties.add(it.single())
            } else {
                properties.add(PropType.ElementList(it.first()))
            }
        }

        // TextContent 찾기
        if (xml.textContent.isNullOrBlank().not()) {
            properties.add(PropType.TextContent)
        }

        return Pojo(
            name = name,
            properties = properties
        )
    }

    sealed interface PropType {
        fun String.toCamelWithCap(): String = this.split("_")
            .joinToString("") { s ->
                s.replaceFirstChar { it.uppercaseChar() }
            }

        fun String.toCamelWithNoCap(): String = this.split("_")
            .mapIndexed { index, s ->
                when (index) {
                    0 -> s.replaceFirstChar { it.lowercaseChar() }
                    else -> s.replaceFirstChar { it.uppercaseChar() }
                }
            }
            .joinToString("")

        data class Attribute(val name: String) : PropType {
            override fun toString(): String {
                return "${TAB}@Attribute(name = \"${name}\")\n${TAB}val ${name.toCamelWithNoCap()}: String?,\n"
            }
        }
        object TextContent : PropType {
            override fun toString(): String {
                return "${TAB}@TextContent\n${TAB}val content: String?,\n"
            }
        }
        data class PropertyElement(val name: String) : PropType {
            override fun toString(): String {
                return "${TAB}@PropertyElement(name = \"$name\")\n${TAB}val ${name.toCamelWithNoCap()}: String?,\n"
            }
        }
        data class Element(val pojo: Pojo) : PropType {
            override fun toString(): String {
                return "${TAB}@Element(name = \"${pojo.name}\")\n${TAB}val ${pojo.lowercaseName}: ${pojo.capitalizedName}?,\n"
            }
        }
        data class ElementList(val type: PropType) : PropType {
            override fun toString(): String {
                return when (type) {
                    is PropertyElement -> {
                        "${TAB}@PropertyElement(name = \"${type.name}\")\n${TAB}val ${type.name.toCamelWithNoCap()}List: List<String>?,\n"
                    }
                    is Element -> {
                        "${TAB}@Element(name = \"${type.pojo.name}\")\n${TAB}val ${type.pojo.lowercaseName}List: List<${type.pojo.capitalizedName}>?,\n"
                    }
                    else -> ""
                }
            }
        }
    }

    data class Pojo(
        val name: String,
        val properties: List<PropType>
    ) {
        val hasTextContentOnly: Boolean
            get() = (properties.size == 1) && (properties.first() is PropType.TextContent)

        private val camelCaseName: String = name.split("_")
            .mapIndexed { index, s ->
                s.replaceFirstChar { it.uppercaseChar() }
            }
            .joinToString("")

        val capitalizedName = camelCaseName.replaceFirstChar { it.uppercaseChar() }
        val lowercaseName = camelCaseName.replaceFirstChar { it.lowercaseChar() }

        override fun toString(): String {
            val sb = StringBuilder()

            val propertiesString = properties.joinToString("\n") { it.toString() }
            sb.appendLine("@Xml(name = \"$name\")")
            sb.appendLine("data class $capitalizedName(")
            sb.append(propertiesString)
            sb.appendLine(")")
            sb.appendLine()

            properties.forEach {
                when (it) {
                    is PropType.Element -> sb.append(it.pojo.toString())
                    is PropType.ElementList -> {
                        if (it.type is PropType.Element) {
                            sb.appendLine(it.type.pojo.toString())
                        }
                    }
                    else -> {}
                }
            }

            return sb.toString()
        }
    }
}

