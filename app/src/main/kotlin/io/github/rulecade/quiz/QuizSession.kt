package io.github.rulecade.quiz

import kotlin.random.Random

// One question as shown to the player: the choices are shuffled so answers can't be memorized
// by position, and `order` maps each shown choice back to its index in the question.
class QuizItem internal constructor(val question: Question, private val order: List<Int>) {
    val choices: List<String> = order.map { question.choices[it] }

    val answer: Int = order.indexOf(question.answer)

    fun isCorrect(shownChoice: Int) = order[shownChoice] == question.answer
}

data class Answer(val item: QuizItem, val choice: Int, val correct: Boolean)

class QuizSession(questions: List<Question>, count: Int = DEFAULT_COUNT, random: Random = Random) {
    val items: List<QuizItem> = questions.shuffled(random).take(count).map { question ->
        QuizItem(question, question.choices.indices.shuffled(random))
    }

    private val answers = mutableListOf<Answer>()

    val current: QuizItem? get() = items.getOrNull(answers.size)

    val answered: Int get() = answers.size

    val isFinished: Boolean get() = answers.size == items.size

    val correctCount: Int get() = answers.count { it.correct }

    val accuracy: Float get() = if (answers.isEmpty()) 0f else correctCount.toFloat() / answers.size

    val results: List<Answer> get() = answers.toList()

    fun answer(choice: Int): Answer {
        val item = checkNotNull(current) { "The quiz is already finished" }
        require(choice in item.choices.indices) { "No choice at $choice" }
        return Answer(item, choice, item.isCorrect(choice)).also { answers += it }
    }

    companion object {
        const val DEFAULT_COUNT = 5
    }
}
