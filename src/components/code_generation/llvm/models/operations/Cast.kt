package components.code_generation.llvm.models.operations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.models.declarations.ValueDeclaration
import components.code_generation.llvm.models.values.Value
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.operations.Cast
import components.semantic_model.types.ObjectType
import components.semantic_model.types.SelfType
import errors.internal.CompilerError

class Cast(override val model: Cast, val subject: Value, val variableDeclaration: ValueDeclaration?):
	Value(model, listOfNotNull(subject, variableDeclaration)) {

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {

		//TODO implement special cases:
		// - Cast from optional primitive to primitive: unbox and check
		// - Cast from primitive to optional primitive: box
		// - Cast from primitive to pointer type: construct wrapper
		// - Cast from pointer type to primitive: destruct wrapper
		// -
		// - Cast from primitive to primitive: LLVM cast (could be combined with casts above)
		// - Cast null value (no type info)


		val subjectValue = subject.getLlvmValue(constructor)
		when(model.operator) {
			Cast.Operator.SAFE_CAST -> {
				return ValueConverter.convertIfRequired(model, constructor, subjectValue, subject.model.providedType, model.referenceType)
			}
			Cast.Operator.OPTIONAL_CAST -> {
				//TODO check if value can be converted
				// else: return null pointer
				return ValueConverter.convertIfRequired(model, constructor, subjectValue, subject.model.providedType, model.referenceType)
			}
			Cast.Operator.RAISING_CAST -> {
				//TODO check if value can be converted
				// else: raise
				return ValueConverter.convertIfRequired(model, constructor, subjectValue, subject.model.providedType, model.referenceType)
			}
			Cast.Operator.CAST_CONDITION, Cast.Operator.NEGATED_CAST_CONDITION -> {
				//TODO check if value can be converted
				// - get specified type (is class definition address)
				// - get value
				// - check if values class definition matches specified class definition
				//   - if yes: result = true
				// - else:
				//   - get parent class definitions => need to be part of definition
				//   - recursively check is match
				//     - if yes for any: result = true
				// - fallback: result = false
				// => advanced feature: support complex types

				//TODO if subject is primitive:
				// - skip comparison
				// - statically determine result

				val subjectClassDefinition = context.getClassDefinition(constructor, subjectValue)
				val referenceTypeDeclaration = when(model.referenceType) {
					is ObjectType -> model.referenceType.getTypeDeclaration()
					is SelfType -> model.referenceType.typeDeclaration
					else -> throw CompilerError(model.referenceType,
						"Conditional casts do not support complex types at the moment. Provided type: ${model.referenceType}")
				}
				val referenceClassDefinition = referenceTypeDeclaration?.unit?.llvmClassDefinition
					?: throw CompilerError(model.referenceType, "Missing class definition for type '${model.referenceType}'.")
				if(variableDeclaration != null) {
					variableDeclaration.compile(constructor)
					constructor.buildStore(subjectValue, variableDeclaration.llvmLocation)
				}
				val resultName = "_castConditionResult"
				return if(model.operator == Cast.Operator.CAST_CONDITION)
					constructor.buildPointerEqualTo(subjectClassDefinition, referenceClassDefinition, resultName)
				else
					constructor.buildPointerNotEqualTo(subjectClassDefinition, referenceClassDefinition, resultName)
			}
		}
	}
}
