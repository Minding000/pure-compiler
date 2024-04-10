package components.semantic_model.values

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.SpecialType
import components.semantic_model.scopes.Scope
import components.semantic_model.types.LiteralType
import errors.internal.CompilerError
import components.syntax_parser.syntax_tree.literals.StringLiteral as StringLiteralSyntaxTree

class StringLiteral(override val source: StringLiteralSyntaxTree, scope: Scope, val value: String): LiteralValue(source, scope) {

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
	}

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		val arrayType = context.arrayDeclarationType
		val byteArray = constructor.buildHeapAllocation(arrayType, "_byteArray")
		val arrayClassDefinitionProperty = constructor.buildGetPropertyPointer(arrayType, byteArray,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "_arrayClassDefinitionProperty")
		constructor.buildStore(context.arrayClassDefinition, arrayClassDefinitionProperty)
		val arraySizeProperty = context.resolveMember(constructor, byteArray, "size")
		constructor.buildStore(constructor.buildInt32(value.length), arraySizeProperty)

		val arrayValueProperty = constructor.buildGetPropertyPointer(arrayType, byteArray, context.arrayValueIndex,
			"_arrayValueProperty")
		val charArray = constructor.buildGlobalAsciiCharArray("_asciiStringLiteral", value, false)
		constructor.buildStore(charArray, arrayValueProperty)

		val stringAddress = constructor.buildHeapAllocation(context.stringTypeDeclaration?.llvmType, "_stringAddress")
		val stringClassDefinitionProperty = constructor.buildGetPropertyPointer(context.stringTypeDeclaration?.llvmType, stringAddress,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "_stringClassDefinitionProperty")
		val stringClassDefinition = context.stringTypeDeclaration?.llvmClassDefinition
			?: throw CompilerError(source, "Missing string type declaration.")
		constructor.buildStore(stringClassDefinition, stringClassDefinitionProperty)
		val exceptionAddress = constructor.buildStackAllocation(constructor.pointerType, "__exceptionAddress")
		val parameters = listOf(exceptionAddress, stringAddress, byteArray)
		constructor.buildFunctionCall(context.llvmStringByteArrayInitializerType, context.llvmStringByteArrayInitializer, parameters)
		return stringAddress
	}

	override fun hashCode(): Int {
		var result = super.hashCode()
		result = 31 * result + value.hashCode()
		return result
	}

	override fun equals(other: Any?): Boolean {
		if(other !is StringLiteral)
			return false
		return value == other.value
	}

	override fun toString(): String {
		return "\"$value\""
	}
}
