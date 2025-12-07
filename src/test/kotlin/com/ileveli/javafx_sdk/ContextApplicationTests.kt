package com.ileveli.javafx_sdk

import com.ileveli.javafx_sdk._examples_.SimpleContextApplication
import com.ileveli.javafx_sdk.UI.AbstractScene
import com.ileveli.javafx_sdk.UI.AbstractSceneModel
import com.ileveli.javafx_sdk.UI.Logger
import javafx.application.Platform
import javafx.scene.Parent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import org.junit.platform.commons.annotation.Testable
import kotlin.random.Random

class SimpleContextScene : AbstractScene<SimpleContextApplication>{
    constructor(
        appContext: SimpleContextApplication,
        root: Parent
    ) : super(appContext, root)
}

@Testable
class ContextApplicationTests {

    @Test
    fun simpleApplicationTest(){
        SimpleContextApplication.show()
    }
    @Test fun CoroutinesScopesTest(){

        SimpleContextApplication.show {
            val model = object  : AbstractSceneModel<SimpleContextApplication, AbstractScene<SimpleContextApplication>>(){
                override fun OnAttached() {

                }

                override fun OnSceneShown() {
                    modelScope.launch {
                        try {
                            while (true) {
                                pane.scaleX += Random.nextDouble(-0.2, 0.3)
                                pane.scaleY += Random.nextDouble(-0.3, 0.1)
                                delay(50)
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



            appScope.launch {
                withContext(Dispatchers.Main) {
                    try {
                        while (true) {
                            pane.translateX += Random.nextDouble(-0.5, 0.8)
                            pane.translateY += Random.nextDouble(-0.5, 0.8)
                            delay(50)
                            Logger.warn { "Running" }
                        }
                    }catch (ce : CancellationException){
                        Logger.info (ce){"The job ${this@launch.coroutineContext} was canceled "}
                        throw ce
                    }catch (e: Throwable){
                        Logger.error (e) { "Caught exception ${e.message} "}
                    }
                }
            }
            appScope.launch {
                delay(500)
                scene.detachModel(model::class)
                delay(100)
                pane.scaleX = 1.0
                pane.scaleY = 1.0
                delay(100)
                assert(pane.scaleX == 1.0 && pane.scaleY == 1.0){ "Model scope cancelation didn't work after detach"}

                delay(1000)
                scene.attachModel(model)
                delay(100)
                assert(pane.scaleX != 1.0 && pane.scaleY != 1.0){ "Model scope reatachment didn't work"}

                delay(100)
                GlobalScope.launch {
                    delay(100)

                    pane.scaleX = 1.0
                    pane.scaleY = 1.0
                    delay(100)
                    assert(pane.scaleX == 1.0 && pane.scaleY == 1.0){ "AppScope cancelation had no efect on ModelScope"}

                    pane.layoutX = 100.0
                    pane.layoutY = 100.0
                    assert(pane.layoutX == 100.0 && pane.layoutY == 100.0){ "AppScope cancelation didn't work"}
                    Platform.exit()
                }
                appScope.cancel(CancellationException("Context died"))
            }

        }


    }

}

