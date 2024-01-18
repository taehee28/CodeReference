/**
 * [Spannable]을 쉽게 만들 수 있게 해주는 유틸 클래스.
 *
 * 사용 예:
 * ```
 * val spannable = SpannableBuilder.build(context) {
 *      span(text = "안녕", textColor = R.color.Red, style = R.style.title)
 *      span(text = "하세요", textColor = R.color.Bule, underline = true)
 * }
 * ```
 * 
 * 생성자를 비공개하고 build 메서드를 만든 이유는
 * 호출하는 곳에서 넘겨주는 Context를 가지는 해당 클래스의 인스턴스가
 * 계속 살아있지 못하도록 하기 위해서이다.
 */
class SpannableBuilder private constructor(
    private val context: Context,
    private val spannableBuilder: SpannableStringBuilder = SpannableStringBuilder()
) {
    fun span(
        text: String,
        @StyleRes style: Int = -1,
        @ColorRes textColor: Int = -1,
        @ColorRes backgroundColor: Int = -1,
        underline: Boolean = false
    ) {
        val startIndex = spannableBuilder.length
        val endIndex = startIndex + text.length

        spannableBuilder.append(text)

        if (style != -1) {
            spannableBuilder[startIndex..endIndex] = TextAppearanceSpan(context, style)
        }
        if (textColor != -1) {
            spannableBuilder[startIndex..endIndex] = ForegroundColorSpan(context.getColor(textColor))
        }
        if (backgroundColor != -1) {
            spannableBuilder[startIndex..endIndex] = BackgroundColorSpan(context.getColor(backgroundColor))
        }
        if (underline) {
            spannableBuilder[startIndex..endIndex] = UnderlineSpan()
        }
    }

    companion object {
        fun build(context: Context, block: SpannableBuilder.() -> Unit): Spannable {
            return SpannableBuilder(context)
                .apply { block() }
                .spannableBuilder.toSpannable()
        }
    }
}
