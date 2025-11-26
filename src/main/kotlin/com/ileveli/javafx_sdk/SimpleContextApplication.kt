package com.ileveli.javafx_sdk

import com.ileveli.javafx_sdk.UI.AbstractApplication
import com.ileveli.javafx_sdk.UI.AbstractScene
import com.ileveli.javafx_sdk.UI.AbstractSceneModel
import com.ileveli.javafx_sdk.UI.IModel
import com.ileveli.javafx_sdk.UI.Logger
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Menu
import javafx.scene.control.MenuItem
import javafx.scene.layout.Pane
import javafx.scene.paint.Paint
import javafx.scene.shape.Circle
import javafx.scene.shape.StrokeType
import javafx.stage.Stage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

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
        val model = object  : AbstractSceneModel<SimpleContextApplication, AbstractScene<SimpleContextApplication>>(){
            override fun OnAttached() {

            }

            override fun OnSceneShown() {
                modelScope.launch {
                    try {
                        while (true) {
                            pane.scaleX += Random.nextDouble(-0.2, 0.3)
                            pane.scaleY += Random.nextDouble(-0.3, 0.1)
                            delay(100)
                            Logger.warn { "Model scope Running" }
                        }
                    }catch (ce : CancellationException){
                        Logger.error (ce){"The job ${this@launch.coroutineContext} was canceled "}
                        throw ce
                    }catch (e: Throwable){
                        Logger.error (e) { "Caught exception ${e.message} "}
                    }
                }
            }

            override fun OnDetached() {

            }
        }
        scene.attachModel(model = model)

        stage.title = "Hello!"
        stage.scene = scene
        stage.show()

       var job =  appScope.async {
            withContext(Dispatchers.IO) {
                try {
                    while (true) {
                        pane.translateX += Random.nextDouble(-0.5, 0.8)
                        pane.translateY += Random.nextDouble(-0.5, 0.8)
                        delay(100)
                        Logger.warn { "Running" }
                    }
                }catch (ce : CancellationException){
                    Logger.error (ce){"The job ${this@async.coroutineContext} was canceled "}
                    throw ce
                }catch (e: Throwable){
                    Logger.error (e) { "Caught exception ${e.message} "}
                }
            }
        }
        appScope.launch {
            delay(1000)
            scene.detachModel(model::class)
            delay(1000)
            scene.attachModel(model)
//            appScope.cancel(CancellationException("Context died"))
        }


//        Platform.exit()
    }
}
  
