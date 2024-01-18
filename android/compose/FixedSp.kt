/**
 * 디바이스 폰트 크기 설정에 영향 받지 않는 sp
 */
val Int.fixedSp: TextUnit
    @Composable
    get() = (this / LocalDensity.current.fontScale).sp
