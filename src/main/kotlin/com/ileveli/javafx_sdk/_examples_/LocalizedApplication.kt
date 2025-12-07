package com.ileveli.javafx_sdk._examples_

import com.ileveli.javafx_sdk.UI.AbstractApplication
import com.ileveli.javafx_sdk.UI.AbstractFXMLScene
import javafx.stage.Stage

class LocalizedApplication : AbstractApplication(){
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
    lateinit var stage: Stage
    override fun start(primaryStage: Stage?) {
        super.start(primaryStage)
        refToSelf = this
        stage = primaryStage!!
        val scene = object : AbstractFXMLScene<LocalizedApplication, LocalizationController>(this,"localization_view.fxml", "localization_menu.fxml"
            /*,ResourceBundle.getBundle("Messages", Locale("ru"))*/){

        }
        primaryStage.scene = scene
        postCompositionAction?.invoke(this)
        primaryStage.show()

    }

}