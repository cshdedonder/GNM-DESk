@file:Suppress("SpellCheckingInspection")

package com.cshdedonder.desk.pde

import com.cshdedonder.desk.expression.DoubleFunction
import org.apache.commons.math3.ode.ContinuousOutputModel
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator
import org.apache.commons.math3.ode.sampling.StepInterpolator
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
    val deltaX: Double = 1.0 / (numberOfMeshPoints - 3)
    val deltaXinv2 = 1.0 / (deltaX * deltaX)
}

abstract class HeatEquation : FirstOrderDifferentialEquations {

    protected abstract val options: Options
    protected abstract val initialState: DoubleArray

    open fun integrate(): SimpleContinuousOutputModel {
        val integrator = DormandPrince853Integrator(1.0e-12, 100.0, options.absTol, options.relTol)
        with(integrator) {
            val model = SimpleContinuousOutputModel(options.deltaX)
            addStepHandler(model)
            integrate(
                    this@HeatEquation,
                    options.tRange.first,
                    initialState,
                    options.tRange.second,
                    initialState
            )
            return model
        }
    }
}

class DirichletHeatEquation(override val options: Options) : HeatEquation() {

    override fun getDimension(): Int = options.numberOfMeshPoints - 2

    private val v = DoubleArray(dimension + 2)

    override fun computeDerivatives(t: Double, u: DoubleArray, u1: DoubleArray) {
        v[0] = options.leftFunction(t)
        v[dimension + 1] = options.rightFunction(t)
        u.copyInto(v, 1)
        for (i in 0 until dimension) {
            u1[i] = (v[i] - 2.0 * v[i + 1] + v[i + 2]) * options.deltaXinv2
            // Because of the use of [v] we shift the indices by one
        }
    }

    override val initialState: DoubleArray
        get() = DoubleArray(dimension) { i -> options.initialFunction(i * options.deltaX) }

}

class NeumannHeatEquation(override val options: Options) : HeatEquation() {

    override val initialState: DoubleArray
        get() = DoubleArray(dimension) { i -> options.initialFunction(i * options.deltaX) }

    override fun getDimension(): Int = options.numberOfMeshPoints - 2

    private val v = DoubleArray(dimension + 2)

    override fun computeDerivatives(t: Double, u: DoubleArray, u1: DoubleArray) {
        v[0] = (-2 * options.deltaX * options.leftFunction(t) + 4 * u[0] - u[1]) / 3
        v[dimension + 1] = (2 * options.deltaX * options.rightFunction(t) - u[dimension - 2] + 4 * u[dimension - 1]) / 3
        u.copyInto(v, 1)
        for (i in 0 until dimension) {
            u1[i] = (v[i] - 2.0 * v[i + 1] + v[i + 2]) * options.deltaXinv2
            // Because of the use of [v] we shift the indices by one
        }
    }
}

class SimpleContinuousOutputModel(private val deltaX: Double) : ContinuousOutputModel() {

    private val times: MutableList<Double> = ArrayList()

    operator fun get(t: Double): DoubleArray {
        interpolatedTime = t
        return interpolatedState
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
}
