package com.ileveli.javafx_sdk

import com.ileveli.javafx_sdk.UI.AbstractScene
import javafx.application.Application
import javafx.scene.Parent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.platform.commons.annotation.Testable

class SimpleContextScene : AbstractScene<SimpleContextApplication>{
    constructor(
        appContext: SimpleContextApplication,
        root: Parent
    ) : super(appContext, root)
}

@Testable
class ContextApplicationTests {

    @Test
    public fun simpleApplicationTest(){
        Application.launch(SimpleContextApplication::class.java)


    }

}

