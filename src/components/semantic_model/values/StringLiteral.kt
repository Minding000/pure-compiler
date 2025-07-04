package components.semantic_model.values

import components.semantic_model.context.SpecialType
import components.semantic_model.scopes.Scope
import components.semantic_model.types.LiteralType
import components.code_generation.llvm.models.values.StringLiteral as StringLiteralUnit
import components.code_generation.llvm.models.values.StringPart as StringPartUnit
import components.syntax_parser.syntax_tree.literals.StringLiteral as StringLiteralSyntaxTree

class StringLiteral(override val source: StringLiteralSyntaxTree, scope: Scope, val parts: List<StringPart>): LiteralValue(source, scope) {

	//TODO implement String literal
	// Requirements:
	// - performant substring?
	// - performant string building?
	// - performant length
	// - support for multibyte charsets
	// - not null-terminated
	// - mutable
	// - literals could share memory
	// - separate multi-byte and single-byte string classes (multi-byte by default)

	// Proposal
	// - byte array stores bytes
	// - character count is stored
	// - equality operator just compares bytes
	// - default encoding is UTF8
	// - unicode normalization is handled by a library
	// - string operations:
	//   - byteIndexOf
	//   - indexOf
	//   - byteSubstring
	//   - substring
	//   - byteAt
	//   - at

	init {
		providedType = LiteralType(source, scope, SpecialType.STRING)
		addSemanticModels(providedType)
		addSemanticModels(parts.filterIsInstance<StringPart.Template>().map(StringPart.Template::expression))
	}
	override fun hashCode(): Int {
		var result = super.hashCode()
		result = 31 * result + source.hashCode()
		result = 31 * result + parts.hashCode()
		return result
	}

	override fun equals(other: Any?): Boolean {
		if(other !is StringLiteral)
			return false
		if(other.parts.size != parts.size)
			return false
		for(index in parts.indices) {
			if(other.parts[index] != parts[index])
				return false
		}
		return true
	}

	override fun toUnit() = StringLiteralUnit(this, parts.map(StringPart::toUnit))
}

sealed class StringPart {
	abstract fun toUnit(): StringPartUnit

	class Segment(val value: String): StringPart() {
		override fun toUnit() = StringPartUnit.Segment(value)
	}

	class Template(val expression: Value): StringPart() {
		override fun toUnit() = StringPartUnit.Template(expression.toUnit())
	}
}
