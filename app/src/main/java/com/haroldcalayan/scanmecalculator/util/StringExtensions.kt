package com.haroldcalayan.scanmecalculator.util

fun String.containsLetter(): Boolean {
    var contains = false
    for (element in this) {
        if (element.isLetter()) {
            contains = true
            break
        }
    }
    return contains
}