package parsing.ast.general

import parsing.ast.Element
import parsing.ast.literals.ForeignLanguageLiteral
import parsing.ast.literals.Identifier

class ForeignLanguageExpression(val languageParser: Identifier, val content: ForeignLanguageLiteral): Element(languageParser.start, content.end) {

	override fun toString(): String {
		return "ForeignLanguageExpression [ $languageParser ] { $content }"
	}
}