package com.ileveli.javafx_sdk.utils

import javafx.scene.Node
import javafx.scene.paint.Color

private inline fun colorToInt(color: Double) = (255*color).toInt()
val Color.r get() = colorToInt(this.red)
val Color.g get() = colorToInt(this.green)
val Color.b get() = colorToInt(this.blue)
val Color.a get() = colorToInt(this.opacity)

fun Color.toRgbaString(withAlpha: Boolean = false) = "rgba(${this.r}, ${this.g}, ${this.b}" +
         if(withAlpha) ", ${this.opacity})" else ")"
fun Color.toHexString(withAlpha: Boolean = false) =
    if(withAlpha) String.format("#%02X%02X%02X%02X", this.r, this.g, this.b,this.a)
    else String.format("#%02X%02X%02X", this.r, this.g, this.b)


fun Node.Style( strokeColor: Color = Color.BLACK
                , strokeWidth: Int = 2
                , fillColor: Color = Color.TRANSPARENT
){
    this.style =  """
        -fx-stroke: ${strokeColor.toHexString(true)};
        -fx-stroke-width: ${strokeWidth}px;
        -fx-fill: ${fillColor.toHexString(true)};
    """
}