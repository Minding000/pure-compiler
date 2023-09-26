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
		type = LiteralType(source, scope, SpecialType.STRING)
		addSemanticModels(type)
	}

	override fun createLlvmValue(constructor: LlvmConstructor): LlvmValue {
		val newByteArrayAddress = constructor.buildHeapAllocation(context.arrayTypeDeclaration?.llvmType, "newByteArrayAddress")
		val arrayClassDefinitionPointer = constructor.buildGetPropertyPointer(context.arrayTypeDeclaration?.llvmType, newByteArrayAddress,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "arrayClassDefinitionPointer")
		val arrayClassDefinitionAddress = context.arrayTypeDeclaration?.llvmClassDefinitionAddress
			?: throw CompilerError(source, "Missing array type declaration.")
		constructor.buildStore(arrayClassDefinitionAddress, arrayClassDefinitionPointer)

		val arrayValuePointer = constructor.buildGetPropertyPointer(context.arrayTypeDeclaration?.llvmType, newByteArrayAddress,
			context.arrayValueIndex, "arrayValuePointer")
		val stringByteArray = constructor.buildGlobalAsciiCharArray("asciiStringValue", value)
		constructor.buildStore(stringByteArray, arrayValuePointer)

		val newStringAddress = constructor.buildHeapAllocation(context.stringTypeDeclaration?.llvmType, "newStringAddress")
		val stringClassDefinitionPointer = constructor.buildGetPropertyPointer(context.stringTypeDeclaration?.llvmType, newStringAddress,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "stringClassDefinitionPointer")
		val stringClassDefinitionAddress = context.stringTypeDeclaration?.llvmClassDefinitionAddress
			?: throw CompilerError(source, "Missing string type declaration.")
		constructor.buildStore(stringClassDefinitionAddress, stringClassDefinitionPointer)
		val parameters = listOf(newStringAddress, newByteArrayAddress)
		constructor.buildFunctionCall(context.llvmStringByteArrayInitializerType, context.llvmStringByteArrayInitializer, parameters)
		return newStringAddress
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
