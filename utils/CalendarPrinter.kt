/**
 * 이전 달과 다음 달의 일부 날짜까지 표시
 */

private fun printCalendar(month: Int) {
    val date = LocalDate.of(LocalDate.now().year, month, 1)
    val startDate = date.sundayOfWeek()
    val lastDate = date.lastDayOfMonth().saturdayOfWeek()

    val diff = Duration.between(startDate.atStartOfDay(), lastDate.atStartOfDay()).toDays().toInt() + 1
    val arr = IntArray(diff) {
        startDate.plusDays(it.toLong()).dayOfMonth
    }

    arr.forEachIndexed { index, i ->
        print("%2d ".format(i))

        if (index % 7 == 6) {
            println()
        }
    }


}

// 해당 달의 마지막 날의 LocalDate 리턴
private fun LocalDate.lastDayOfMonth(): LocalDate =
    with(TemporalAdjusters.lastDayOfMonth())

// 달력 상에서 봤을 때 특정 주의 첫번째 날(일요일) 리턴 
private fun LocalDate.sundayOfWeek(): LocalDate =
    with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

// 달력 상에서 봤을 때 특정 주의 마지막 날(토요일) 리턴
private fun LocalDate.saturdayOfWeek(): LocalDate =
    with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
