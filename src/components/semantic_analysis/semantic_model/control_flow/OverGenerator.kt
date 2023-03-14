package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.LiteralType
import components.semantic_analysis.semantic_model.types.PluralType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Function
import components.semantic_analysis.semantic_model.values.LocalVariableDeclaration
import components.semantic_analysis.semantic_model.values.Value
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.loops.NotIterable
import logger.issues.loops.PluralTypeIteratorDeclaration
import logger.issues.loops.TooManyIterableVariableDeclarations
import logger.issues.loops.TooManyPluralTypeVariableDeclarations
import java.util.*
import components.syntax_parser.syntax_tree.control_flow.OverGenerator as OverGeneratorSyntaxTree

class OverGenerator(override val source: OverGeneratorSyntaxTree, scope: Scope, val collection: Value,
					val iteratorVariableDeclaration: LocalVariableDeclaration?, val variableDeclarations: List<LocalVariableDeclaration>):
	Unit(source, scope) {

	init {
		addUnits(collection, iteratorVariableDeclaration)
		addUnits(variableDeclarations)
	}

	override fun linkValues(linter: Linter) {
		collection.linkValues(linter)
		val collectionType = collection.type
		if(collectionType is PluralType) {
			setVariableTypes(linter, collectionType)
		} else if(collectionType != null) {
			setVariableTypes(linter, collectionType)
		}
	}

	override fun analyseDataFlow(linter: Linter, tracker: VariableTracker) {
		collection.analyseDataFlow(linter, tracker)
		if(iteratorVariableDeclaration != null)
			tracker.declare(iteratorVariableDeclaration, true)
		for(variableDeclaration in variableDeclarations)
			tracker.declare(variableDeclaration, true)
	}

	private fun setVariableTypes(linter: Linter, collectionType: PluralType) {
		if(iteratorVariableDeclaration != null)
			linter.addIssue(PluralTypeIteratorDeclaration(iteratorVariableDeclaration.source))
		if(variableDeclarations.size > 2) {
			linter.addIssue(TooManyPluralTypeVariableDeclarations(source, variableDeclarations))
		} else if(variableDeclarations.size == 2) {
			variableDeclarations.firstOrNull()?.type = LiteralType(source, scope, Linter.SpecialType.INTEGER, linter)
		}
		variableDeclarations.lastOrNull()?.type = collectionType.baseType
	}

	private fun setVariableTypes(linter: Linter, collectionType: Type) {
		if(!collectionType.isInstanceOf(Linter.SpecialType.ITERABLE)) {
			linter.addIssue(NotIterable(collection.source))
			return
		}
		try {
			val iteratorProperty = collectionType.interfaceScope.resolveValue("createIterator")
			val iteratorFunction = iteratorProperty?.value as? Function
			val iteratorSignature = iteratorFunction?.functionType?.resolveSignature()
			val iteratorType = iteratorSignature?.returnType ?: return
			iteratorVariableDeclaration?.type = iteratorType
			val availableValueTypes = LinkedList<Type?>()
			if(iteratorType.isInstanceOf(Linter.SpecialType.INDEX_ITERATOR)) {
				val indexProperty = iteratorType.interfaceScope.resolveValue("currentIndex")
				availableValueTypes.add(indexProperty?.type)
			}
			if(iteratorType.isInstanceOf(Linter.SpecialType.KEY_ITERATOR)) {
				val keyProperty = iteratorType.interfaceScope.resolveValue("currentKey")
				availableValueTypes.add(keyProperty?.type)
			}
			if(iteratorType.isInstanceOf(Linter.SpecialType.VALUE_ITERATOR)) {
				val valueProperty = iteratorType.interfaceScope.resolveValue("currentValue")
				availableValueTypes.add(valueProperty?.type)
			}
			if(variableDeclarations.size > availableValueTypes.size) {
				linter.addIssue(TooManyIterableVariableDeclarations(source, variableDeclarations, availableValueTypes))
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
