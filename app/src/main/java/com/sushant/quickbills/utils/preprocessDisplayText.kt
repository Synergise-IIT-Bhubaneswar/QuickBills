package com.sushant.quickbills.utils

fun preprocessDisplayText(num: Double):String
{
    val str=num.toBigDecimal().toPlainString()
    val preprocessedText=str.substring(0,1)+"."+str.substring(1,3)+"E"+(str.length-1).toBigDecimal().toPlainString()
    if(num>99999999) {
        return preprocessedText
    }
    else {
        return str
    }
}