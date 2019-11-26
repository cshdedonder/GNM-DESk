package com.cshdedonder.desk.expression

import com.udojava.evalex.Expression
import java.math.BigDecimal

typealias DoubleFunction = (Double) -> Double

fun Expression.toFunctionIn(v: String): DoubleFunction = { x -> setPrecision(24).with(v, BigDecimal(x)).eval().toDouble() }

fun String.toExpression() = Expression(this)