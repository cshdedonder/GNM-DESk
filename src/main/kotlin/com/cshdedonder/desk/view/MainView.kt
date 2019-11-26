@file:Suppress("SpellCheckingInspection")

package com.cshdedonder.desk.view

import com.cshdedonder.desk.chart.makeOutputModelChart
import com.cshdedonder.desk.expression.toExpression
import com.cshdedonder.desk.expression.toFunctionIn
import com.cshdedonder.desk.pde.*
import com.jogamp.opengl.GLException
import javafx.beans.property.*
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.ToggleGroup
import javafx.scene.image.ImageView
import javafx.scene.layout.StackPane
import javafx.stage.Modality
import javafx.stage.Stage
import org.jzy3d.chart.AWTChart
import org.jzy3d.javafx.JavaFXChartFactory
import tornadofx.*
import java.io.File

class MainView : View("DESk - by Cedric De Donder") {

    private val dirichletLeftProperty: StringProperty = SimpleStringProperty("0")
    private val dirichletRightProperty: StringProperty = SimpleStringProperty("0")
    private val neumannLeftProperty: StringProperty = SimpleStringProperty("0")
    private val neumannRightProperty: StringProperty = SimpleStringProperty("0")
    private val initialFunctionProperty: StringProperty = SimpleStringProperty("sin(deg(PI*x))")
    private val numberOfMeshPointsProperty: StringProperty = SimpleStringProperty("120")
    private val relTolProperty: StringProperty = SimpleStringProperty("1e-8")
    private val absTolProperty: StringProperty = SimpleStringProperty("1e-8")
    private val endTimeProperty: StringProperty = SimpleStringProperty("0.3")
    private val wireframeProperty: BooleanProperty = SimpleBooleanProperty(true)
    private val savePlotProperty: BooleanProperty = SimpleBooleanProperty(false)
    private val saveNameProperty: StringProperty = SimpleStringProperty("plot")
    private val timeTakenProperty: StringProperty = SimpleStringProperty("")

    private val chartProperty: Property<AWTChart> = SimpleObjectProperty()

    private val boundaryConditionProperty: Property<BoundaryCondition> =
            SimpleObjectProperty(BoundaryCondition.DIRICHLET)

    private val chartFactory = JavaFXChartFactory()

    init {
        chartProperty.addListener(ChangeListener { _, _, chart ->
            if (chart != null) {
                val imageView: ImageView = chartFactory.bindImageView(chart)
                try {
                    with(Stage()) {
                        initModality(Modality.NONE)
                        val pane = StackPane()
                        scene = Scene(pane)
                        title = "Plot"
                        show()
                        pane.children.add(imageView)
                        chartFactory.addSceneSizeChangedListener(chart, scene)
                        width = 600.0
                        height = 600.0
                    }
                }catch (ignored: GLException) {}
            }
        })
    }

    override val root = vbox(20) {
        val vboxDisableProperty = disableProperty()
        vboxConstraints {
            paddingAll = 40.0
        }
        label("Solving the heat equation: du/dt=d²u/dx²") {
            style = "-fx-font-weight: bold"
        }
        val boundaryConditions = ToggleGroup()
        val dirichletRadioButtonSelectedProperty: BooleanProperty = SimpleBooleanProperty()
        radiobutton(text = "Dirichlet boundary conditions", group = boundaryConditions) {
            isSelected = true
            with(selectedProperty()) {
                dirichletRadioButtonSelectedProperty.bindBidirectional(this)
                addListener(ChangeListener { _, _, selected ->
                    if (selected) {
                        boundaryConditionProperty.value = BoundaryCondition.DIRICHLET
                    }
                })
            }
        }
        hbox {
            disableWhen(dirichletRadioButtonSelectedProperty.not())
            label("u(0,t)= ")
            textfield(dirichletLeftProperty)
        }
        hbox {
            disableWhen(dirichletRadioButtonSelectedProperty.not())
            label("u(1,t)= ")
            textfield(dirichletRightProperty)
        }
        val neumannRadioButtonSelectedPropert: BooleanProperty = SimpleBooleanProperty()
        radiobutton(text = "Von Neumann boundary conditions", group = boundaryConditions) {
            with(selectedProperty()) {
                neumannRadioButtonSelectedPropert.bindBidirectional(this)
                addListener(ChangeListener { _, _, selected ->
                    if (selected) {
                        boundaryConditionProperty.value = BoundaryCondition.NEUMANN
                    }
                })
            }
        }
        hbox {
            disableWhen(neumannRadioButtonSelectedPropert.not())
            label("du(0,t)/dx= ")
            textfield(neumannLeftProperty)
        }
        hbox {
            disableWhen(neumannRadioButtonSelectedPropert.not())
            label("du(1,t)/dx= ")
            textfield(neumannRightProperty)
        }
        gridpane {
            vgap = 20.0
            paddingRight = 30.0
            row {
                label("u(x,0)= ")
                textfield(initialFunctionProperty)
            }
            row {
                label("Number of meshpoints (J): ")
                textfield(numberOfMeshPointsProperty)
            }
            row {
                label("Relative Tolerance: ")
                textfield(relTolProperty)
            }
            row {
                label("Absolute Tolerance: ")
                textfield(absTolProperty)
            }
            row {
                label("End of integration interval: ")
                textfield(endTimeProperty)
            }
            row {
                checkbox("Display wireframe", wireframeProperty)
            }
            row {
                checkbox("Save plot", savePlotProperty)
                textfield(saveNameProperty).disableWhen(savePlotProperty.not())
            }
        }
        hbox(100) {
            button("Calculate").action {
                try {
                    vboxDisableProperty.value = true
                    handleCalculate()
                } catch (e: NumberFormatException) {
                    alert(Alert.AlertType.ERROR, header = "Invalid input",
                            content = "${e.message}, please enter a valid number.")
                } finally {
                    vboxDisableProperty.value = false
                }
            }
            label(timeTakenProperty)
        }
    }

    private fun handleCalculate() {
        val numberOfMeshPoints = numberOfMeshPointsProperty.value.toInt()
        val relTol = relTolProperty.value.toDouble()
        val absTol = absTolProperty.value.toDouble()
        val initialFunction =
                initialFunctionProperty.value.toExpression().toFunctionIn("x")
        val endTime = endTimeProperty.value.toDouble()
        val equation: HeatEquation = when (boundaryConditionProperty.value!!) {
            BoundaryCondition.DIRICHLET -> {
                val leftFunction =
                        dirichletLeftProperty.value.toExpression().toFunctionIn("t")
                val rightFunction =
                        dirichletRightProperty.value.toExpression().toFunctionIn("t")
                val options = Options(
                        initialFunction,
                        leftFunction,
                        rightFunction,
                        Pair(0.0, endTime),
                        relTol,
                        absTol,
                        numberOfMeshPoints
                )
                DirichletHeatEquation(options)
            }
            BoundaryCondition.NEUMANN -> {
                val leftFunction = neumannLeftProperty.value.toExpression().toFunctionIn("t")
                val rightFunction = neumannRightProperty.value.toExpression().toFunctionIn("t")
                val options = Options(
                        initialFunction,
                        leftFunction,
                        rightFunction,
                        Pair(0.0, endTime),
                        relTol,
                        absTol,
                        numberOfMeshPoints
                )
                NeumannHeatEquation(options)
            }
        }
        val (time, model) = measureMillisAndReturn {
            equation.integrate()
        }
        chartProperty.value = makeOutputModelChart(
                model,
                chartFactory,
                xRange = Pair(0.0, 1.0),
                tRange = Pair(0.0, endTime),
                wireframe = wireframeProperty.value
        )
        timeTakenProperty.value = "Time taken: ${time}ms"
        if(savePlotProperty.value){
            chartProperty.value.screenshot(File(saveNameProperty.value))
        }
    }
}

enum class BoundaryCondition {
    DIRICHLET, NEUMANN
}

data class TimedResult<A>(val millies: Long, val result: A)

private inline fun<A> measureMillisAndReturn(block: () -> A): TimedResult<out A> {
    val startTime = System.currentTimeMillis()
    val result = block()
    val endTime = System.currentTimeMillis()
    return TimedResult(endTime - startTime, result)
}