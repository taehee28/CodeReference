fun Modifier.noRippleClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    this.clickable(
        onClick = onClick,
        enabled = enabled,
        interactionSource = remember {
            MutableInteractionSource()
        },
        indication = null
    )
}
