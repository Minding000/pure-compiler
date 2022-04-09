package parsing.ast.general

import parsing.ast.literals.ForeignLanguageLiteral
import parsing.ast.literals.Identifier

class ForeignLanguageExpression(val languageParser: Identifier, val content: ForeignLanguageLiteral): MetaElement(languageParser.start, content.end) {

	override fun toString(): String {
		return "ForeignLanguageExpression [ $languageParser ] { $content }"
	}
}