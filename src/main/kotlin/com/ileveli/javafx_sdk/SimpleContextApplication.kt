package com.ileveli.javafx_sdk

import com.ileveli.javafx_sdk.UI.AbstractApplication
import com.ileveli.javafx_sdk.UI.AbstractScene
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.control.Menu
import javafx.scene.control.MenuItem
import javafx.scene.layout.Pane
import javafx.scene.paint.Paint
import javafx.scene.shape.Circle
import javafx.scene.shape.StrokeType
import javafx.stage.Stage

class SimpleContextApplication : AbstractApplication() {
    override fun start(stage: Stage) {
        var pane = Pane(Circle(50.0).also {
            it.centerX = 70.0
            it.centerY =  70.0
            it.fill = Paint.valueOf("blue")
            it.stroke = Paint.valueOf("black")
            it.strokeWidth = 2.0
            it.strokeType = StrokeType.OUTSIDE

        })
        pane.padding = Insets(20.0,20.0,20.0,20.0)


        var scene = object : AbstractScene<SimpleContextApplication>(this,pane){}.also {scene->
            scene.menuBar.menus.add(Menu("Menu group 1")
                .also {
                    it.items.add(MenuItem("MenuItem 1"))
                }
            )

            scene.menuBar.menus.add(Menu("Menu group 2")
                .also {
                    it.items.add(MenuItem("MenuItem 1"))
                    it.items.add(MenuItem("MenuItem 2"))
                }
            )
            scene.menuBar.isUseSystemMenuBar = false
        }


        stage.title = "Hello!"
        stage.scene = scene
        stage.show()

//        Platform.exit()
    }
}
  
