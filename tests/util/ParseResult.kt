package util

import messages.Message
import components.parsing.element_generator.ElementGenerator
import components.parsing.syntax_tree.general.Program

class ParseResult(private val elementGenerator: ElementGenerator, val program: Program) {

	fun assertNoMessagesOfType(type: Message.Type) {
		for(message in elementGenerator.logger.messages())
			if(message.type == type)
				throw AssertionError("Unexpected linter message '${message.description}' of type '${message.type}' has been emitted.")
	}

	fun assertMessageEmitted(expectedType: Message.Type, expectedMessage: String) {
		for(message in elementGenerator.logger.messages()) {
			if(message.description.contains(expectedMessage)) {
				if(message.type != expectedType)
					throw AssertionError("Linter message '$expectedMessage' has type '${message.type}' instead of expected type '$expectedType'.")
				return
			}
		}
		throw AssertionError("Expected linter message '$expectedMessage' hasn't been emitted.")
	}

	fun assertMessageNotEmitted(expectedType: Message.Type, expectedMessage: String) {
		for(message in elementGenerator.logger.messages()) {
			if(message.description.contains(expectedMessage)) {
				if(message.type != expectedType)
					throw AssertionError("Linter message '$expectedMessage' has type '${message.type}' instead of expected type '$expectedType'.")
				throw AssertionError("Unexpected linter message '$expectedMessage' has been emitted.")
			}
		}
	}
}
