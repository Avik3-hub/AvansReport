package com.example.avans

// Преобразует "Нагибин Сергей Викторович" в "Нагибин С.В."
fun String.toShortName(): String {
    val parts = this.trim().split("\\s+".toRegex())
    if (parts.isEmpty()) return ""
    
    val lastName = parts[0]
    val firstNameInitial = parts.getOrNull(1)?.firstOrNull()?.let { "$it." } ?: ""
    val patronymicInitial = parts.getOrNull(2)?.firstOrNull()?.let { "$it." } ?: ""
    
    return "$lastName $firstNameInitial$patronymicInitial".trim()
}
