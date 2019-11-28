package com.cshdedonder.desk.app

import com.cshdedonder.desk.view.MainView
import tornadofx.*

class Main : App(MainView::class, Styles::class)

class Styles : Stylesheet()

fun main(args: Array<String>) {
    launch<Main>(args)
}