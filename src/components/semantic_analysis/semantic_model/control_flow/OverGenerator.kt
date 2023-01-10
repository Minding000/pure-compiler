package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.LiteralType
import components.semantic_analysis.semantic_model.types.PluralType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Function
import components.semantic_analysis.semantic_model.values.LocalVariableDeclaration
import components.semantic_analysis.semantic_model.values.Value
import errors.user.SignatureResolutionAmbiguityError
import messages.Message
import java.util.*
import components.syntax_parser.syntax_tree.control_flow.OverGenerator as OverGeneratorSyntaxTree

class OverGenerator(override val source: OverGeneratorSyntaxTree, val collection: Value,
					val iteratorVariableDeclaration: LocalVariableDeclaration?, val variableDeclarations: List<LocalVariableDeclaration>):
	Unit(source) {

	init {
		addUnits(collection, iteratorVariableDeclaration)
		addUnits(variableDeclarations)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		collection.linkValues(linter, scope)
		val collectionType = collection.type
		if(collectionType is PluralType) {
			setVariableTypes(linter, collectionType)
		} else if(collectionType != null) {
			setVariableTypes(linter, collectionType)
		}
	}

	private fun setVariableTypes(linter: Linter, collectionType: PluralType) {
		if(iteratorVariableDeclaration != null)
			linter.addMessage(iteratorVariableDeclaration.source, "Plural types don't provide an iterator.", Message.Type.ERROR)
		if(variableDeclarations.size > 2) {
			linter.addMessage(source,
				"Plural types only provide index and element (2) values, but ${variableDeclarations.size} were declared.",
				Message.Type.ERROR)
		} else if(variableDeclarations.size == 2) {
			variableDeclarations.firstOrNull()?.type = LiteralType(source, Linter.SpecialType.INTEGER, linter)
		}
		variableDeclarations.lastOrNull()?.type = collectionType.baseType
	}

	private fun setVariableTypes(linter: Linter, collectionType: Type) {
		if(!collectionType.isInstanceOf(Linter.SpecialType.ITERABLE)) {
			linter.addMessage(collection.source, "The provided object is not iterable.", Message.Type.ERROR)
			return
		}
		try {
			val iteratorProperty = collectionType.scope.resolveValue("createIterator")
			val iteratorFunction = iteratorProperty?.value as? Function
			val iteratorSignature = iteratorFunction?.functionType?.resolveSignature()
			val iteratorType = iteratorSignature?.returnType ?: return
			iteratorVariableDeclaration?.type = iteratorType
			val availableValueTypes = LinkedList<Type?>()
			if(iteratorType.isInstanceOf(Linter.SpecialType.INDEX_ITERATOR)) {
				val indexProperty = iteratorType.scope.resolveValue("currentIndex")
				availableValueTypes.add(indexProperty?.type)
			}
			if(iteratorType.isInstanceOf(Linter.SpecialType.KEY_ITERATOR)) {
				val keyProperty = iteratorType.scope.resolveValue("currentKey")
				availableValueTypes.add(keyProperty?.type)
			}
			if(iteratorType.isInstanceOf(Linter.SpecialType.VALUE_ITERATOR)) {
				val valueProperty = iteratorType.scope.resolveValue("currentValue")
				availableValueTypes.add(valueProperty?.type)
			}
			if(variableDeclarations.size > availableValueTypes.size) {
				linter.addMessage(source, "The number of declared variables (${variableDeclarations.size})" +
					" is larger than the number of values provided by the iterables iterator (${availableValueTypes.size}).",
					Message.Type.ERROR)
				return
			}
			for(index in variableDeclarations.indices) {
				val sourceValueIndex = availableValueTypes.size - (variableDeclarations.size - index)
				variableDeclarations[index].type = availableValueTypes[sourceValueIndex]
			}
		} catch(error: SignatureResolutionAmbiguityError) {
			error.log(linter, source, "function", "createIterator()")
		}
	}
}
