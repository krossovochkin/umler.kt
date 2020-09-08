package com.krossovochkin.umler

import com.krossovochkin.umler.core.*
import javafx.application.Application
import javafx.geometry.Bounds
import javafx.geometry.VPos
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.paint.Paint
import javafx.scene.shape.Line
import javafx.scene.shape.Polygon
import javafx.scene.shape.Rectangle
import javafx.scene.text.Text
import javafx.scene.text.TextAlignment
import javafx.scene.transform.Rotate
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.lang.Math.toDegrees
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

private const val SCENE_WIDTH = 1000.0
private const val SCENE_HEIGHT = 800.0

private val FILL_BG_PAINT_EMPTY = Paint.valueOf("#FFFFFF")
private val FILL_BG_PAINT_FULL = Paint.valueOf("#000000")
private val STROKE_PAINT = Paint.valueOf("#000000")

class App : Application() {

    override fun start(primaryStage: Stage) {
        val file = FileChooser().showOpenDialog(primaryStage)

        val (elements, connections) = readUml(file)
        val elementViews = elements
            .map { element ->
                ElementView(
                    element = element,
                    initialX = SCENE_WIDTH / 2,
                    initialY = SCENE_HEIGHT / 2
                )
            }
        val connectionViews = connections
            .mapNotNull { connection ->
                val start = elementViews.find { connection.start == it.element }
                val end = elementViews.find { connection.end == it.element }

                if (start == null || end == null) {
                    println("Connection skipped, start: $start, end: $end")
                    return@mapNotNull null
                }

                ConnectionView(
                    connection = connection,

                    start = start,
                    end = end
                )
            }

        val root = Group(
            elementViews.map(ElementView::node) + connectionViews.map(
                ConnectionView::node
            )
        )

        elementViews.map { elementView ->
            elementView.node.setOnMouseDragged { event ->
                elementView.node.translateX = event.sceneX - elementView.initialX
                elementView.node.translateY = event.sceneY - elementView.initialY

                connectionViews
                    .filter { it.start == elementView || it.end == elementView }
                    .forEach(ConnectionView::update)
            }
        }

        primaryStage.scene = Scene(
            root,
            SCENE_WIDTH,
            SCENE_HEIGHT
        )
        primaryStage.show()
    }
}

data class ElementView(
    val element: Element,

    val initialX: Double,
    val initialY: Double
) {
    val node: Node = createElementNode()

    private fun createElementNode(
    ): Node {
        val text = Text(
            initialX,
            initialY,
            if (element is InterfaceElement) "<<interface>>\n${element.name}" else element.name
        ).apply {
            textOrigin = VPos.CENTER
            textAlignment = TextAlignment.CENTER
        }

        val width = text.boundsInParent.width
        val height = text.boundsInParent.height
        val padding = 16.0

        val rect = Rectangle(
            width + 2 * padding,
            height + 2 * padding
        ).apply {
            x = initialX - padding
            y = initialY - padding - height / 2

            this.stroke = STROKE_PAINT
            this.fill = FILL_BG_PAINT_EMPTY
        }

        return Group(rect, text)
    }
}

data class ConnectionView(
    val connection: Connection,

    val start: ElementView,
    val end: ElementView
) {

    private val group: Group = Group()
    val node: Node = group

    init {
        group.children.add(createConnectionNode(start, end))
    }

    fun update() {
        group.children.clear()
        group.children.add(createConnectionNode(start, end))
    }

    private fun createConnectionNode(
        start: ElementView,
        end: ElementView
    ): Node {
        val group = Group()

        val (startPoint, endPoint) = calculateArrowPoints(start, end)
        val (startX, startY) = startPoint
        val (endX, endY) = endPoint

        val rotate = createArrowRotation(
            startX = startX,
            startY = startY,
            endX = endX,
            endY = endY
        )

        when (connection) {
            is ExtendsConnection -> {
                group.children.addAll(
                    createArrowLine(
                        startX = startX,
                        startY = startY,
                        endX = endX,
                        endY = endY,
                        rotate = rotate
                    ),
                    createTriangleArrow(
                        x = endX,
                        y = endY,
                        rotate = rotate,
                        fillPaint = FILL_BG_PAINT_EMPTY,
                        strokePaint = STROKE_PAINT
                    )
                )
            }
            is ImplementsConnection -> {
                group.children.addAll(
                    createArrowLine(
                        startX = startX,
                        startY = startY,
                        endX = endX,
                        endY = endY,
                        rotate = rotate,
                        strokeDashArray = listOf(5.0)
                    ),
                    createTriangleArrow(
                        x = endX,
                        y = endY,
                        rotate = rotate,
                        fillPaint = FILL_BG_PAINT_EMPTY,
                        strokePaint = STROKE_PAINT
                    )
                )
            }
            is AggregatesConnection -> {
                group.children.addAll(
                    createArrowLine(
                        startX = startX,
                        startY = startY,
                        endX = endX,
                        endY = endY,
                        rotate = rotate
                    ),
                    createDiamondArrow(
                        x = endX,
                        y = endY,
                        rotate = rotate,
                        fillPaint = FILL_BG_PAINT_EMPTY,
                        strokePaint = STROKE_PAINT
                    )
                )
            }
            is UsesConnection -> {
                group.children.addAll(
                    createArrowLine(
                        startX = startX,
                        startY = startY,
                        endX = endX,
                        endY = endY,
                        rotate = rotate,
                        strokeDashArray = listOf(5.0)
                    ),
                    createLineArrow(
                        x = endX,
                        y = endY,
                        rotate = rotate
                    )
                )
            }
        }

        return group
    }

    private fun calculateArrowPoints(
        start: ElementView,
        end: ElementView
    ): Pair<Pair<Double, Double>, Pair<Double, Double>> {
        val startMinX = start.extract { minX }
        val startMaxX = start.extract { maxX }
        val startMinY = start.extract { minY }
        val startMaxY = start.extract { maxY }

        val endMinX = end.extract { minX }
        val endMaxX = end.extract { maxX }
        val endMinY = end.extract { minY }
        val endMaxY = end.extract { maxY }

        val (startX, endX) = when {
            startMaxX < endMinX -> {
                startMaxX to endMinX
            }
            startMinX > endMaxX -> {
                startMinX to endMaxX
            }
            else -> {
                (start.extract { minX } + start.extract { width } / 2) to
                        (end.extract { minX } + end.extract { width } / 2)
            }
        }

        val (startY, endY) = when {
            startMaxY < endMinY -> {
                startMaxY to endMinY
            }
            startMinY > endMaxY -> {
                startMinY to endMaxY
            }
            else -> {
                (start.extract { minY } + start.extract { height } / 2) to
                        (end.extract { minY } + end.extract { height } / 2)
            }
        }

        return (startX to startY) to (endX to endY)
    }

    private inline fun ElementView.extract(block: Bounds.() -> Double): Double {
        return this.node.boundsInParent.block()
    }

    private fun createArrowRotation(
        startX: Double,
        startY: Double,
        endX: Double,
        endY: Double
    ): Rotate {
        val angleR = (endY - startY) / sqrt((endX - startX).pow(2) + (endY - startY).pow(2))
        val angleD = toDegrees(acos(angleR)) - 180.0

        return Rotate().apply {
            angle = if (endX < startX) angleD else -angleD
        }
    }

    private fun createArrowLine(
        startX: Double,
        startY: Double,
        endX: Double,
        endY: Double,
        rotate: Rotate,
        strokeDashArray: List<Double>? = null
    ): Node {
        return Line(startX, startY, endX, endY)
            .apply {
                rotate.pivotXProperty().bind(endXProperty())
                rotate.pivotYProperty().bind(endYProperty())

                if (strokeDashArray != null) {
                    this.strokeDashArray.addAll(strokeDashArray)
                }
            }
    }

    private fun createTriangleArrow(
        x: Double,
        y: Double,
        offsetX: Double = ARROW_OFFSET_X,
        offsetY: Double = ARROW_OFFSET_Y,
        rotate: Rotate,
        fillPaint: Paint,
        strokePaint: Paint
    ): Node {
        return Polygon(
            x, y,
            x - offsetX, y + offsetY,
            x + offsetX, y + offsetY
        ).apply {
            fill = fillPaint
            stroke = strokePaint
            transforms.add(rotate)
        }
    }

    private fun createDiamondArrow(
        x: Double,
        y: Double,
        offsetX: Double = DIAMOND_OFFSET_X,
        offsetY: Double = DIAMOND_OFFSET_Y,
        rotate: Rotate,
        fillPaint: Paint,
        strokePaint: Paint
    ): Node {
        return Polygon(
            x, y,
            x - offsetX, y + offsetY,
            x, y + 2 * offsetY,
            x + offsetX, y + offsetY
        ).apply {
            fill = fillPaint
            stroke = strokePaint
            transforms.add(rotate)
        }
    }

    private fun createLineArrow(
        x: Double,
        y: Double,
        offsetX: Double = ARROW_OFFSET_X,
        offsetY: Double = ARROW_OFFSET_Y,
        rotate: Rotate
    ): Node {
        return Group(
            Line(
                x, y,
                x - offsetX, y + offsetY
            ),
            Line(
                x, y,
                x + offsetX, y + offsetY
            )
        ).apply {
            transforms.add(rotate)
        }
    }

    companion object {
        private const val ARROW_OFFSET_X = 10.0
        private const val ARROW_OFFSET_Y = 10.0
        private const val DIAMOND_OFFSET_X = 10.0
        private const val DIAMOND_OFFSET_Y = 15.0
    }
}
