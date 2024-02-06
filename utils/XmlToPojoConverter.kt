/**
 * XML 응답을 파싱하여 Tikxml 라이브러리에서 제공하는 어노테이션을 사용하는 Pojo 데이터 클래스로 표현해주는 유틸 코드
 * 만들어진 Pojo 클래스를 print하면 data class 형태의 문자열을 표시함. 
 * 완벽하게 파싱하는게 아니라서 파싱한 결과물을 어느정도 수정 해줘야 함.
 */

fun main() {
    val prefixPath = "/*파일 경로*/"

    // xml이 써져있는 txt 파일 이름 받기
    print("file name -> ")
    val fileName = readlnOrNull() ?: ""
    println()

    val xmlString = File(prefixPath + fileName).readText()

    // Document로 만들기
    // Document로 만드는 과정에서 xml에서 허용하지 않는 문자열이 들어가면 error 발생함!
    val doc = DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(InputSource(StringReader(xmlString)))

    // 파싱한 Pojo 클래스를 프린트
    println(parseToPojo(doc.documentElement))
}

/**
 * Element인 xml을 파싱하여 Pojo 클래스로 만듬
 */
private fun parseToPojo(xml: Element): Pojo {
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
            val pojo = parseToPojo(it)
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


/**
 * 데이터 클래스의 정보를 나타내는 클래스
 * 
 * @property name 클래스 이름
 * @property properties 클래스의 프로퍼티들
 */
data class Pojo(
    val name: String,
    val properties: List<PropType>
) {
    val hasTextContentOnly: Boolean
        get() = (properties.size == 1) && (properties.first() is PropType.TextContent)

    val capitalizedName = name.replaceFirstChar { it.uppercaseChar() }
    val lowercaseName = name.replaceFirstChar { it.lowercaseChar() }

    override fun toString(): String {
        val sb = StringBuilder()
        properties.forEach {
            sb.appendLine(it.toString())
        }

        return "@Xml(name = \"$name\")\ndata class $capitalizedName(\n $sb )\n"
    }
}

/**
 * Pojo 클래스의 프로퍼티에 붙는 어노테이션들의 종류
 */
sealed interface PropType {
    /**
     * Attribute 어노테이션
     */
    data class Attribute(val name: String) : PropType {
        override fun toString(): String {
            return "\t@Attribute(name = \"${name}\")\n\tval ${name.replaceFirstChar { it.lowercaseChar() }}: String,\n"
        }
    }

    /**
     * TextContent 어노테이션
     */
    object TextContent : PropType {
        override fun toString(): String {
            return "\t@TextContent\n\tval content: String,\n"
        }
    }

    /**
     * PropertyElement 어노테이션
     */
    data class PropertyElement(val name: String) : PropType {
        override fun toString(): String {
            return "\t@PropertyElement(name = \"$name\")\n\tval $name: String,\n"
        }
    }

    /**
     * Element 어노테이션
     */
    data class Element(val pojo: Pojo) : PropType {
        override fun toString(): String {
            println(pojo.toString())

            return "\t@Element(name = \"${pojo.name}\")\n\tval ${pojo.lowercaseName}: ${pojo.capitalizedName},\n"
        }
    }

    /**
     * 중복되는 PropertyElement나 Element 프로퍼티들은 리스트 타입으로 묶음
     */
    data class ElementList(val type: PropType) : PropType {
        override fun toString(): String {
            return when (type) {
                is PropertyElement -> {
                    "\t@PropertyElement(name = \"${type.name}\")\n\tval ${type.name}List: List<String>,\n"
                }
                is Element -> {
                    println(type.pojo.toString())

                    "\t@Element(name = \"${type.pojo.name}\")\n\tval ${type.pojo.lowercaseName}List: List<${type.pojo.capitalizedName}>,\n"
                }
                else -> ""
            }
        }
    }
}
