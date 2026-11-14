package io.github.rulecade.ui.quiz

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.rulecade.quiz.Answer
import io.github.rulecade.quiz.QuizItem
import io.github.rulecade.quiz.QuizSession

// A round being played, one question at a time; ranked and practice rounds share it.
open class QuizRound internal constructor(private val session: QuizSession) {
    var index by mutableIntStateOf(0)
        private set

    // The answer to the current question once it is given.
    var answer by mutableStateOf<Answer?>(null)
        private set

    var streak by mutableIntStateOf(0)
        private set

    val total: Int get() = session.items.size

    val item: QuizItem get() = session.items[index]

    val answered: Int get() = index + if (answer == null) 0 else 1

    val isFinished: Boolean get() = answered == total

    val results: List<Answer> get() = session.results

    internal fun choose(choice: Int): Answer {
        check(answer == null) { "Already answered" }
        return session.answer(choice).also {
            answer = it
            streak = if (it.correct) streak + 1 else 0
        }
    }

    internal fun next() {
        check(answer != null && !isFinished) { "Answer first" }
        index++
        answer = null
    }
}
