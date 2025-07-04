package components.code_generation.llvm.models.values

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.models.declarations.ComputedPropertyDeclaration
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.values.StringLiteral
import errors.internal.CompilerError

class StringLiteral(override val model: StringLiteral, val parts: List<StringPart>): Value(model) {

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

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		val exceptionAddress = context.getExceptionParameter(constructor)
		if(parts.isEmpty())
			return context.createStringObject(constructor, "", exceptionAddress)
		if(parts.size == 1)
			return parts.first().buildLlvmValue(context, constructor)
		val firstPart = parts.first()
		var currentExpression = firstPart.buildLlvmValue(context, constructor)
		val appendFunctionAddress = context.resolveFunction(constructor, currentExpression, " + String: String")
		for(part in parts) {
			if(part === firstPart)
				continue
			var partValue = part.buildLlvmValue(context, constructor)
			if(part is StringPart.Template && part.expression.model.effectiveType != model.effectiveType) {
				//TODO optimization: stringify primitives without wrapping them
				partValue = ValueConverter.convertIfRequired(model, constructor, partValue, part.expression.model.effectiveType,
					part.expression.model.hasGenericType, model.effectiveType, false)
				val stringRepresentationDeclaration =
					context.standardLibrary.anyStringRepresentationComputedPropertyDeclaration ?: throw CompilerError(model,
						"Failed to find Any string representation computed property.")
				partValue = buildGetterCall(constructor, partValue, stringRepresentationDeclaration)
			}
			currentExpression = constructor.buildFunctionCall(context.standardLibrary.stringAppendFunctionType, appendFunctionAddress,
				listOf(exceptionAddress, currentExpression, partValue), "currentExpression")
		}
		return currentExpression
	}

	private fun buildGetterCall(constructor: LlvmConstructor, targetValue: LlvmValue,
								computedPropertyDeclaration: ComputedPropertyDeclaration): LlvmValue {
		val exceptionParameter = context.getExceptionParameter(constructor)
		val llvmValue = context.resolveFunction(constructor, targetValue, computedPropertyDeclaration.model.getterIdentifier)
		val returnValue =
			constructor.buildFunctionCall(computedPropertyDeclaration.llvmGetterType, llvmValue, listOf(exceptionParameter, targetValue),
				"_stringRepresentationGetterResult")
		context.continueRaise(constructor, model)
		return returnValue
	}
}

sealed class StringPart {
	abstract fun buildLlvmValue(context: Context, constructor: LlvmConstructor): LlvmValue

	class Segment(val value: String): StringPart() {
		override fun buildLlvmValue(context: Context, constructor: LlvmConstructor) =
			context.createStringObject(constructor, value, context.getExceptionParameter(constructor))
	}

	class Template(val expression: Value): StringPart() {
		override fun buildLlvmValue(context: Context, constructor: LlvmConstructor) = expression.getLlvmValue(constructor)
	}
}
