/**
 * 프로퍼티가 많은 클래스를 toString했을 때 보기 힘들게 한줄로 쭉 나오는 String을 보기좋게 줄바꿈해주는 유틸 코드
 */

fun main() {
    val prefixPath = "/* 파일 경로 */"

    print("file name -> ")
    val fileName = readlnOrNull() ?: ""
    println()

    val content = File(prefixPath + fileName).readText()

    val prettyString = prettifyToString(content)

    // 파일로 생성
    val prettyFile = File(prefixPath + "pretty_$fileName")
    if (prettyFile.exists().not()) {
        prettyFile.createNewFile()
    }
    prettyFile.writeText(prettyString)
}

private const val TAB = "    "

private fun prettifyToString(str: String): String {
    val builder = StringBuilder()
    var tabCount = 0
    str.forEach {
        when(it) {
            '(', '[' -> builder.append("$it\n${TAB.repeat(++tabCount)}")
            ')', ']' -> builder.append("\n${TAB.repeat(--tabCount)}$it")
            ',' -> builder.append("$it\n${TAB.repeat(tabCount)}")
            else -> builder.append(it)
        }
    }

    val result = builder.toString()
    print(result)

    return result
}
