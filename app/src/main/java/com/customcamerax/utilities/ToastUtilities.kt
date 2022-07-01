package com.customcamerax.utilities

import android.content.Context
import android.widget.Toast

fun Context.stringResToast(context: Context = applicationContext, message: String, toastDuration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, toastDuration).show()
}