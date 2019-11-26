package com.cshdedonder.desk.chart

import com.cshdedonder.desk.pde.SimpleContinuousOutputModel
import org.jzy3d.chart.AWTChart
import org.jzy3d.colors.Color
import org.jzy3d.colors.ColorMapper
import org.jzy3d.colors.colormaps.ColorMapRedAndGreen
import org.jzy3d.javafx.JavaFXChartFactory
import org.jzy3d.maths.Range
import org.jzy3d.plot3d.builder.Builder
import org.jzy3d.plot3d.builder.Mapper
import org.jzy3d.plot3d.builder.concrete.OrthonormalGrid
import org.jzy3d.plot3d.primitives.Shape
import org.jzy3d.plot3d.rendering.canvas.Quality

fun makeOutputModelChart(
        model: SimpleContinuousOutputModel,
        factory: JavaFXChartFactory,
        xRange: Pair<Double, Double>,
        tRange: Pair<Double, Double>,
        wireframe: Boolean
): AWTChart {
    val mapper = object : Mapper() {
        override fun f(x: Double, t: Double): Double = model[x, t]
    }
    val steps = 80
    val surface: Shape = Builder.buildOrthonormal(
            OrthonormalGrid(xRange.asRange(), steps, tRange.asRange(), steps),
            mapper
    )
    with(surface) {
        colorMapper = ColorMapper(ColorMapRedAndGreen(), bounds.zmin.toDouble(), bounds.zmax.toDouble())
        faceDisplayed = true
        wireframeDisplayed = wireframe
        wireframeColor = Color.BLACK
    }
    val chart: AWTChart = factory.newChart(Quality.Advanced, "offscreen") as AWTChart
    chart.scene.graph.add(surface)
    return chart
}

private fun Pair<Double, Double>.asRange(): Range = Range(first.toFloat(), second.toFloat())