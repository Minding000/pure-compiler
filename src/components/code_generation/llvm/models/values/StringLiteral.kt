package components.code_generation.llvm.models.values

import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.values.StringLiteral

class StringLiteral(override val model: StringLiteral): Value(model) {

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
		return context.createStringObject(constructor, model.value, context.getExceptionParameter(constructor))
	}
}
