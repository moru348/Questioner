package me.moru3.questioner

import java.util.*

enum class QuestionType {
    NUMBER,
    DECISION;

    override fun toString(): String {
        return super.toString().toLowerCase(Locale.ROOT)
    }
}