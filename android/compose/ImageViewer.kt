// 이미지 로딩 라이브러리로 Coil 사용

@Composable
fun ImageViewer(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color.DarkGray)
    ) {
        val scope = rememberCoroutineScope()

        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset(0f, 0f)) }

        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(LocalContext.current)
                .data("https://dummyimage.com/400x600/f5c1f5/ffffff")
                .build()
        )

        var viewerSize = remember { Size.Zero }
		val density = LocalDensity.current
        
        val painterState by painter.state.collectAsState()
        val imageSize by remember(painterState) {
            derivedStateOf {
                // 화면에 표시되고 있는 이미지의 크기를 얻음
                // 이미지는 뷰어 컴포저블에 Fit 하도록 표시되고 있다고 간주함(ContentScale.Fit)

                val imageSize = Size(
                    (painter.intrinsicSize.width * density.density),
                    (painter.intrinsicSize.height * density.density)
                )

                val widthRatio = viewerSize.width / imageSize.width
                val heightRatio = viewerSize.height / imageSize.height

                if (widthRatio < heightRatio) {
                    // 화면 가로를 채운다
                    Size(
                        width = viewerSize.width,
                        height = (imageSize.height * widthRatio)
                    )
                } else {
                    // 화면 세로를 채운다
                    Size(
                        width = (imageSize.width * heightRatio),
                        height = viewerSize.height
                    )
                }
            }
        }

        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    viewerSize = it.toSize()
                }
                .pointerInput(Unit) {
                    // 위치 이동, 확대/축소 처리
                    detectTransformGestures { _, pan, zoom, _ ->
                        // 이미지 줌의 최대/최소 값을 지정
                        scale = (scale * zoom).coerceIn(0.5f, 3f)

                        offset = if (scale == 1f) {
                            // 1배수가 되면 이미지를 정위치로 되돌림
                            Offset(0f, 0f)
                        } else if (scale > 1f) {
                            // 확대한 채로 이미지 이동 시,
                            // 이미지가 화면 밖을 넘어가지 않도록 제한
                            val newOffset = offset + pan

                            val maxX = (imageSize.width * scale - viewerSize.width) / 2f
                            val maxY = (imageSize.height * scale - viewerSize.height) / 2f

                            val boundedX = if (maxX < 0f) {
                                if (newOffset.x < 0) {
                                    max(maxX, newOffset.x)
                                } else {
                                    min(maxX.absoluteValue, newOffset.x)
                                }
                            } else {
                                newOffset.x
                            }

                            val boundedY = if (maxY < 0f) {
                                if (newOffset.y < 0) {
                                    max(maxY, newOffset.y)
                                } else {
                                    min(maxY.absoluteValue, newOffset.y)
                                }
                            } else {
                                newOffset.y
                            }

                            Offset(boundedX, boundedY)
                        } else {
                            offset + pan
                        }
                    }
                }
                .pointerInput(Unit) {
                    // 손가락을 놓았을 때 처리
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Release) {
                                if (scale < 1f) {
                                    // 1배수 미만으로 작게 축소 하고 손을 뗐을 때,
                                    // 1배수로 돌아오도록 처리
                                    scope.launch {
                                        animate(
                                            initialValue = scale,
                                            targetValue = 1f
                                        ) { value, _ ->
                                            scale = value
                                        }
                                    }

                                    scope.launch {
                                        animate(
                                            typeConverter = Offset.VectorConverter,
                                            initialValue = offset,
                                            targetValue = Offset.Zero
                                        ) { value, _ ->
                                            offset = value
                                        }
                                    }
                                }

                                // 확대한 채로 이미지를 드래그 하고 손을 뗐을 때,
                                // 화면 가장자리에서 이미지가 떨어지면 위치를 되돌리는 처리
                                val maxX = max(
                                    (imageSize.width * scale - viewerSize.width) / 2f,
                                    0f
                                )
                                val maxY = max(
                                    (imageSize.height * scale - viewerSize.height) / 2f,
                                    0f
                                )

                                scope.launch {
                                    animate(
                                        typeConverter = Offset.VectorConverter,
                                        initialValue = offset,
                                        targetValue = Offset(
                                            // 이미지가 화면을 꽉채우지 않은 상태에서 이미지 드래그 시
                                            // 중앙으로 돌아오지는 않음
                                            x = if (maxX == 0f) {
                                                offset.x
                                            } else {
                                                offset.x.coerceIn(-maxX, maxX)
                                            },
                                            y = if (maxY == 0f) {
                                                offset.y
                                            } else {
                                                offset.y.coerceIn(-maxY, maxY)
                                            }
                                            
                                            // 이미지가 화면을 꽉채우지 않은 상태에서 이미지 드래그 시
                                            // 중앙으로 돌아옴
                                            //x = offset.x.coerceIn(-maxX, maxX),
                                            //y = offset.y.coerceIn(-maxY, maxY)
                                        )
                                    ) { value, _ ->
                                        offset = value
                                    }
                                }
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    // 더블 탭을 하면 크기를 1배수로, 위치를 정위치로 되돌리는 처리
                    detectTapGestures(
                        onDoubleTap = {
                            scope.launch {
                                animate(
                                    initialValue = scale,
                                    targetValue = 1f
                                ) { value, _ ->
                                    scale = value
                                }
                            }

                            scope.launch {
                                animate(
                                    typeConverter = Offset.VectorConverter,
                                    initialValue = offset,
                                    targetValue = Offset.Zero
                                ) { value, _ ->
                                    offset = value
                                }
                            }
                        }
                    )
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}
