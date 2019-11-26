package com.cshdedonder.desk.app

import com.cshdedonder.desk.view.MainView
import tornadofx.*

class Main : App(MainView::class, Styles::class){

    init {
        Thread.setDefaultUncaughtExceptionHandler{_, _ -> } //UGLY, no idea how to fix this though
    }
}

class Styles : Stylesheet()

fun main(args: Array<String>) {
    launch<Main>(args)
}