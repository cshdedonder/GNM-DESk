@file:Suppress("SpellCheckingInspection")

package com.cshdedonder.desk.view

import com.cshdedonder.desk.chart.makeOutputModelChart
import com.cshdedonder.desk.expression.DoubleFunction
import com.cshdedonder.desk.expression.toExpression
import com.cshdedonder.desk.expression.toFunctionIn
import com.cshdedonder.desk.pde.*
import javafx.beans.property.*
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.ToggleGroup
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.StackPane
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.WindowEvent
import org.jzy3d.chart.AWTChart
import org.jzy3d.javafx.JavaFXChartFactory
import tornadofx.*
import java.io.File
import java.text.NumberFormat
import kotlin.math.min

class MainView : View("DESk - by Cedric De Donder") {

    private val dirichletLeftProperty: StringProperty = SimpleStringProperty("0")
    private val dirichletRightProperty: StringProperty = SimpleStringProperty("1")
    private val neumannLeftProperty: StringProperty = SimpleStringProperty("0")
    private val neumannRightProperty: StringProperty = SimpleStringProperty("-1")
    private val initialFunctionProperty: StringProperty = SimpleStringProperty("sin(deg(PI*x/2))")
    private val numberOfMeshPointsProperty: StringProperty = SimpleStringProperty("10")
    private val relTolProperty: StringProperty = SimpleStringProperty("1e-8")
    private val absTolProperty: StringProperty = SimpleStringProperty("1e-8")
    private val endTimeProperty: StringProperty = SimpleStringProperty("0.5")
    private val numberOfTSamplePointsProperty: StringProperty = SimpleStringProperty("80")
    private val wireframeProperty: BooleanProperty = SimpleBooleanProperty(true)

    private val timeTakenProperty: StringProperty = SimpleStringProperty("")
    private val numberStepsProperty: StringProperty = SimpleStringProperty("")
    private val averageStepProperty: StringProperty = SimpleStringProperty("")
    private val totalStepsProperty: StringProperty = SimpleStringProperty("")

    private val chartProperty: Property<AWTChart> = SimpleObjectProperty()

    private val boundaryConditionProperty: Property<BoundaryCondition> =
            SimpleObjectProperty(BoundaryCondition.DIRICHLET)

    private val chartFactory = JavaFXChartFactory()

    private val stageList: MutableList<Stage> = ArrayList()

    init {
        chartProperty.addListener(ChangeListener { _, _, chart ->
            if (chart != null) {
                val imageView: ImageView = chartFactory.bindImageView(chart)
                with(Stage()) {
                    initModality(Modality.NONE)
                    val pane = StackPane().apply {
                        // Initialise to prevent GLException
                        minWidth = 50.0
                        minHeight = 50.0
                    }
                    scene = Scene(pane)
                    title = "Plot"
                    show()
                    chartFactory.addSceneSizeChangedListener(chart, scene)
                    pane.children.add(imageView)
                    width = 600.0
                    height = 600.0
                    scene.onKeyTyped = EventHandler<KeyEvent> { event ->
                        if (event.character == "s") {
                            val files: List<File> = chooseFile(
                                    title = "Choose screenshot destination",
                                    filters = arrayOf(FileChooser.ExtensionFilter("Images", "*.png")),
                                    mode = FileChooserMode.Save
                            )
                            if (files.isNotEmpty()) {
                                chart.screenshot(files[0])
                            }
                        } else if (event.code == KeyCode.ESCAPE) {
                            close()
                        }
                    }
                    stageList += this
                    onCloseRequest = EventHandler<WindowEvent> { stageList.remove(this) }
                }
            }
        })
    }

    override val root = vbox(20) {
        val vboxDisableProperty = disableProperty()
        vboxConstraints {
            paddingAll = 40.0
        }
        label("Solving the heat equation: \u2202u\u2215\u2202t=\u2202²u\u2215\u2202x²") {
            style = "-fx-font-weight: bold"
        }
        label("with: x \u2208 [0,1], 0 \u2264 t") {
            style = "-fx-font-style: italic"
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
            label("\u2202u(0,t)\u2215\u2202x= ")
            textfield(neumannLeftProperty)
        }
        hbox {
            disableWhen(neumannRadioButtonSelectedPropert.not())
            label("\u2202u(1,t)\u2215\u2202x= ")
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
                label("Number of divisions in x (J): ")
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
                label("Number of sample points in t: ")
                textfield(numberOfTSamplePointsProperty)
            }
            row {
                checkbox("Display wireframe", wireframeProperty)
            }
        }
        hbox(30) {
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
            vbox {
                label(timeTakenProperty)
                label(numberStepsProperty)
                label(averageStepProperty)
                label(totalStepsProperty)
            }
        }
        button("Close all").action {
            stageList.forEach { it.close() }
        }
    }

    private fun handleCalculate() {
        val numberOfMeshPoints = numberOfMeshPointsProperty.value.toInt()
        val relTol = relTolProperty.value.toDouble()
        val absTol = absTolProperty.value.toDouble()
        val initialFunction =
                initialFunctionProperty.value.toExpression().toFunctionIn("x")
        val endTime = endTimeProperty.value.toDouble()
        val numberOfTSamplePoints = numberOfTSamplePointsProperty.value.toInt()
        val (left: DoubleFunction, right: DoubleFunction, equationFunction: (Options) -> HeatEquation) = when (boundaryConditionProperty.value!!) {
            BoundaryCondition.DIRICHLET ->
                Triple(
                        dirichletLeftProperty.value.toExpression().toFunctionIn("t"),
                        dirichletRightProperty.value.toExpression().toFunctionIn("t"),
                        { options -> DirichletHeatEquation(options) }
                )
            BoundaryCondition.NEUMANN ->
                Triple<DoubleFunction, DoubleFunction, (Options) -> HeatEquation>(
                        neumannLeftProperty.value.toExpression().toFunctionIn("t"),
                        neumannRightProperty.value.toExpression().toFunctionIn("t"),
                        { options -> NeumannHeatEquation(options) }
                )
        }
        val equation = equationFunction(Options(
                initialFunction, left, right, Pair(0.0, endTime), relTol, absTol, numberOfMeshPoints
        ))
        val (time: Long, model: SimpleContinuousOutputModel) = measureMillisAndReturn {
            equation.integrate()
        }
        chartProperty.value = makeOutputModelChart(
                model,
                chartFactory,
                xRange = Pair(0.0, 1.0),
                xSteps = min(80, numberOfMeshPoints),
                tRange = Pair(0.0, endTime),
                tSteps = min(80, numberOfTSamplePoints),
                wireframe = wireframeProperty.value
        )
        val nf = NumberFormat.getInstance()
        timeTakenProperty.value = "Time taken: ${nf.format(time)}ms"
        numberStepsProperty.value = "Number of steps in t: ${nf.format(model.numberOfSteps)} steps"
        averageStepProperty.value = "Average step size: %e".format(model.averageStep)
        totalStepsProperty.value = "Total grid size: ${nf.format(model.numberOfSteps * numberOfMeshPoints)} vertices"
    }
}

enum class BoundaryCondition {
    DIRICHLET, NEUMANN
}

data class TimedResult<A>(val millies: Long, val result: A)

private inline fun <A> measureMillisAndReturn(block: () -> A): TimedResult<out A> {
    val startTime = System.currentTimeMillis()
    val result = block()
    val endTime = System.currentTimeMillis()
    return TimedResult(endTime - startTime, result)
}