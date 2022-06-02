package parsing.ast.general

import linter.Linter
import linter.elements.general.ForeignLanguageExpression
import linter.scopes.Scope
import parsing.ast.literals.ForeignLanguageLiteral
import parsing.ast.literals.Identifier

class ForeignLanguageExpression(private val foreignParser: Identifier, private val content: ForeignLanguageLiteral):
	ValueElement(foreignParser.start, content.end) {

	override fun concretize(linter: Linter, scope: Scope): ForeignLanguageExpression {
		return ForeignLanguageExpression(this, foreignParser.concretize(linter, scope), content.getValue())
	}

	override fun toString(): String {
		return "ForeignLanguageExpression [ $foreignParser ] { $content }"
	}
}