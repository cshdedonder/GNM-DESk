@file:Suppress("SpellCheckingInspection", "DuplicatedCode")

package com.cshdedonder.desk.pde

import com.cshdedonder.desk.expression.DoubleFunction
import org.apache.commons.math3.ode.ContinuousOutputModel
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator
import org.apache.commons.math3.ode.sampling.StepInterpolator
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.ceil
import kotlin.math.floor

data class Options(
        val initialFunction: DoubleFunction,
        val leftFunction: DoubleFunction,
        val rightFunction: DoubleFunction,
        val tRange: Pair<Double, Double>,
        val relTol: Double,
        val absTol: Double,
        val numberOfMeshPoints: Int
) {
    val deltaX: Double = 1.0 / (numberOfMeshPoints - 1)
    val deltaXinv2 = 1.0 / (deltaX * deltaX)
}

abstract class HeatEquation : FirstOrderDifferentialEquations {

    protected abstract val options: Options
    protected abstract val initialState: DoubleArray
    protected abstract val modelInstance: SimpleContinuousOutputModel

    open fun integrate(): SimpleContinuousOutputModel {
        val integrator = DormandPrince853Integrator(1.0e-12, 100.0, options.absTol, options.relTol)
        with(integrator) {
            modelInstance
            addStepHandler(modelInstance)
            integrate(
                    this@HeatEquation,
                    options.tRange.first,
                    initialState,
                    options.tRange.second,
                    initialState
            )
            return modelInstance
        }
    }


}

class DirichletHeatEquation(override val options: Options) : HeatEquation() {

    override fun getDimension(): Int = options.numberOfMeshPoints - 2

    private val v = DoubleArray(dimension + 2)

    override fun computeDerivatives(t: Double, u: DoubleArray, u1: DoubleArray) {
        v[0] = options.leftFunction(t)
        v[dimension + 1] = options.rightFunction(t)
        modelInstance.boundaryValues[t] = v[0] to v.last()
        u.copyInto(v, 1)
        for (i in 0 until dimension) {
            u1[i] = (v[i] - 2.0 * v[i + 1] + v[i + 2]) * options.deltaXinv2
            // Because of the use of [v] we shift the indices by one
        }
    }

    override val initialState: DoubleArray
        get() = DoubleArray(dimension) { i -> options.initialFunction((i + 1) * options.deltaX) }

    override val modelInstance = SimpleContinuousOutputModel(options.deltaX)

}

class NeumannHeatEquation(override val options: Options) : HeatEquation() {

    override val initialState: DoubleArray
        get() = DoubleArray(dimension) { i -> options.initialFunction((i + 1) * options.deltaX) }

    override fun getDimension(): Int = options.numberOfMeshPoints - 2

    private val v = DoubleArray(dimension + 2)

    override fun computeDerivatives(t: Double, u: DoubleArray, u1: DoubleArray) {
        v[0] = (-2 * options.deltaX * options.leftFunction(t) + 4 * u[0] - u[1]) / 3
        v[dimension + 1] = (2 * options.deltaX * options.rightFunction(t) - u[dimension - 2] + 4 * u[dimension - 1]) / 3
        modelInstance.boundaryValues[t] = v[0] to v.last()
        u.copyInto(v, 1)
        for (i in 0 until dimension) {
            u1[i] = (v[i] - 2.0 * v[i + 1] + v[i + 2]) * options.deltaXinv2
            // Because of the use of [v] we shift the indices by one
        }
    }

    override val modelInstance: SimpleContinuousOutputModel = SimpleContinuousOutputModel(deltaX = options.deltaX)
}

class SimpleContinuousOutputModel(private val deltaX: Double) : ContinuousOutputModel() {

    private val times: MutableList<Double> = ArrayList()

    internal val boundaryValues: TreeMap<Double, Pair<Double, Double>> = TreeMap()

    operator fun get(t: Double): DoubleArray {
        interpolatedTime = t
        val out = DoubleArray(interpolatedState.size + 2)
        interpolatedState.copyInto(out, 1)
        val (left, right) = boundaryValues.getInterpolatedValue(t)
        out[0] = left
        out[out.size - 1] = right
        return out
    }

    operator fun get(x: Double, t: Double): Double {
        val v: DoubleArray = get(t)
        val x0: Int = floor(x / deltaX).toInt()
        val x1: Int = ceil(x / deltaX).toInt()
        if (x0 == x1) {
            return v[x0]
        }
        val w: Double = ((x / deltaX) - x0) / (x1 - x0)
        return v[x0] * (1.0 - w) + v[x1] * w
    }

    val numberOfSteps: Int
        get() = times.size

    val averageStep: Double by lazy {
        times.asSequence().zipWithNext().map { (x, y) -> y - x }.average()
    }

    override fun handleStep(interpolator: StepInterpolator, isLast: Boolean) {
        times += interpolator.currentTime
        super.handleStep(interpolator, isLast)
    }

    private fun TreeMap<Double, Pair<Double, Double>>.getInterpolatedValue(t: Double): Pair<Double, Double> {
        val low: Map.Entry<Double, Pair<Double, Double>>? = floorEntry(t)
        val high: Map.Entry<Double, Pair<Double, Double>>? = ceilingEntry(t)
        if (low == null) {
            return high?.value!!
        }
        if (high == null) {
            return low.value
        }
        //[low] and [high] smart cast to non-null
        if (low.key == high.key) {
            return low.value
        }
        val w: Double = (t - low.key) / (high.key - low.key)
        return (1.0 - w) * low.value + w * high.value
    }

    private operator fun Double.times(other: Pair<Double, Double>): Pair<Double, Double> = Pair(this * other.first, this * other.second)
    private operator fun Pair<Double, Double>.plus(other: Pair<Double, Double>) = Pair(first + other.first, second + other.second)
}
