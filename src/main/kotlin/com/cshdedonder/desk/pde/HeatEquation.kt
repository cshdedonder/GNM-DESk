@file:Suppress("SpellCheckingInspection")

package com.cshdedonder.desk.pde

import com.cshdedonder.desk.expression.DoubleFunction
import org.apache.commons.math3.ode.ContinuousOutputModel
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator
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
            val model = SimpleContinuousOutputModel(options)
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
            u1[i] = (v(i) - 2.0 * v(i + 1) + v(i + 2)) * options.deltaXinv2
            // Because of the use of [v] we shift the indices by one
        }
    }

    /**
     * Misuse of invoke to implement 'safe get'
     */
    private operator fun DoubleArray.invoke(index: Int): Double = if (index < 0 || index >= size) 0.0 else get(index)

    override val initialState: DoubleArray
        get() = DoubleArray(dimension) { i -> options.initialFunction(i * options.deltaX) }

}

open class SimpleContinuousOutputModel(private val options: Options) : ContinuousOutputModel() {
    open operator fun get(t: Double): DoubleArray {
        interpolatedTime = t
        return interpolatedState
    }

    private val v = DoubleArray(options.numberOfMeshPoints)

    open operator fun get(x: Double, t: Double): Double {
        get(t).copyInto(v, 1)
        v[0] = options.leftFunction(t)
        v[options.numberOfMeshPoints - 1] = options.rightFunction(t)
        val x0: Int = floor(x / options.deltaX).toInt()
        val x1: Int = ceil(x / options.deltaX).toInt()
        if (x0 == x1) {
            return v[x0]
        }
        val w: Double = ((x / options.deltaX) - x0) / (x1 - x0)
        return v[x0] * (1.0 - w) + v[x1] * w
    }
}

class NeumannHeatEquation(override val options: Options) : HeatEquation() {

    override val initialState: DoubleArray
        get() = DoubleArray(dimension) { i -> options.initialFunction(i * options.deltaX) }

    override fun getDimension(): Int = options.numberOfMeshPoints - 2

    private val v = DoubleArray(dimension + 2)

    override fun computeDerivatives(t: Double, u: DoubleArray, u1: DoubleArray) {
        v[0] = (-2 * options.deltaX * options.leftFunction(t) + 4 * u[0] - u[1]) / 3
        v[dimension + 1] = (2 * options.deltaX * options.rightFunction(t) - u[dimension - 2] - u[dimension - 1]) / 3 //FIXME
        u.copyInto(v, 1)
        for (i in 0 until dimension) {
            u1[i] = (v(i) - 2.0 * v(i + 1) + v(i + 2)) * options.deltaXinv2
            // Because of the use of [v] we shift the indices by one
        }
    }

    /**
     * Misuse of invoke to implement 'safe get'
     */
    private operator fun DoubleArray.invoke(index: Int): Double = if (index < 0 || index >= size) 0.0 else get(index)
}
