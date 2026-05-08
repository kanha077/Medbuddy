package com.yash.medbuddy.model

data class Medicine(
    val id: Int = 0,
    val name: String = "",
    val dosage: String = "",
    val instruction: String = "",
    val time: String = "",
    val date: String = "",
    var isTaken: Int = 0
)