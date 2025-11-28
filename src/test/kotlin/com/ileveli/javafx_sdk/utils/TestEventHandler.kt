package com.ileveli.javafx_sdk.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.platform.commons.annotation.Testable
import java.lang.ref.WeakReference



@Testable
class TestEventHandler {


    @Test
    fun test0(){
        fun myFunction(a:Int){
            assert(a == 10){"Caller received wrong value"}
        }
        fun myFunction2(a:Int){
            assert(false) {"The function triggered"}
        }
        var handler = EventHandler<Int>()
        handler.addHandler(::myFunction)
        handler.fireEvent(10)
        var key = handler.addHandler(::myFunction2)
        assert(key == handler.getKey(::myFunction2)) {"Keys are different "}
        handler.removeHandler(key)
    }

    class SomeClass {
        fun foo(s:String){
//        assert(false){"The caller reached me"}
        }
    }


    @Test
    fun test1(){
        var handler = EventHandler<String>()
        var someInstance = WeakReference<SomeClass>(SomeClass())
        handler.addHandler(someInstance.get()!!::foo)
        //SHIT! It does not work
        runBlocking {
            for (i in 0..10) {
                System.gc()
                delay(100)
                handler.fireEvent("Hello")
                System.gc()
            }

        }
        System.gc()
        assert(someInstance!!.get() == null){"Instance not collected"}
        assert(false)


    }

}
