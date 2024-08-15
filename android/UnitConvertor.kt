// Dp -> Px 
val Float.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()
