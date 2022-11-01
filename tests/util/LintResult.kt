package util

import components.linting.Linter
import components.linting.semantic_model.general.Program
import components.linting.semantic_model.general.Unit
import messages.Message

class LintResult(private val linter: Linter, val program: Program) {

	inline fun <reified T: Unit>find(noinline predicate: (T) -> Boolean = { true }): T? {
		val file = program.getFile(listOf("Test", "Test"))
		return file?.find(predicate)
	}

	fun assertNoMessagesOfType(type: Message.Type) {
		for(message in linter.logger.messages())
			if(message.type == type)
				throw AssertionError("Unexpected linter message '${message.description}' of type '${message.type}' has been emitted.")
	}

	fun assertMessageEmitted(expectedType: Message.Type, expectedMessage: String) {
		for(message in linter.logger.messages()) {
			if(message.description.contains(expectedMessage)) {
				if(message.type != expectedType)
					throw AssertionError("Linter message '$expectedMessage' has type '${message.type}' instead of expected type '$expectedType'.")
				return
			}
		}
		throw AssertionError("Expected linter message '$expectedMessage' hasn't been emitted.")
	}

	fun assertMessageNotEmitted(expectedType: Message.Type, expectedMessage: String) {
		for(message in linter.logger.messages()) {
			if(message.description.contains(expectedMessage)) {
				if(message.type != expectedType)
					throw AssertionError("Linter message '$expectedMessage' has type '${message.type}' instead of expected type '$expectedType'.")
				throw AssertionError("Unexpected linter message '$expectedMessage' has been emitted.")
			}
		}
	}
}
