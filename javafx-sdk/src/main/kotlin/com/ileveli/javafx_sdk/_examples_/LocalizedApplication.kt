package com.ileveli.javafx_sdk._examples_

import com.ileveli.javafx_sdk.UI.AbstractApplication
import com.ileveli.javafx_sdk.UI.AbstractFXMLScene
import com.ileveli.javafx_sdk.UI.AbstractScene
import javafx.scene.Scene
import javafx.scene.layout.VBox
import javafx.stage.Stage
import java.util.Locale
import java.util.ResourceBundle

class LocalizedApplication : AbstractApplication(){
    override val packageName: String
        get() = ""

    companion object{
        lateinit var refToSelf: LocalizedApplication
        private var postCompositionAction: (LocalizedApplication.()-> Unit )? = {
            this.stage.show()
        }

        fun show(postAction: (LocalizedApplication.()-> Unit)? = null){
            postAction?.let {
                postCompositionAction = postAction
            }
            launch(LocalizedApplication::class.java)


        }
    }

    override fun mainSceneResolver(stage: Stage): Scene? {
        val scene = object : AbstractFXMLScene<LocalizedApplication, LocalizationController>(this
            ,"/com/ileveli/javafx_sdk/_examples_/localization_view.fxml"
            ,""
            ,ResourceBundle.getBundle("test/Messages", Locale("ru"))
            ){

        }
        return scene
        return object : AbstractScene<LocalizedApplication>(this, VBox()){}

    }
    lateinit var stage: Stage
    override fun start(primaryStage: Stage) {
        super.start(primaryStage)
        refToSelf = this
        stage = primaryStage

        postCompositionAction?.invoke(this)
        primaryStage.show()

    }

}