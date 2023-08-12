package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.FunctionType
import components.semantic_analysis.semantic_model.types.LiteralType
import components.semantic_analysis.semantic_model.types.PluralType
import components.semantic_analysis.semantic_model.types.Type
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
	SemanticModel(source, scope) {

	init {
		addSemanticModels(collection, iteratorVariableDeclaration)
		addSemanticModels(variableDeclarations)
	}

	override fun determineTypes() {
		collection.determineTypes()
		val collectionType = collection.type
		if(collectionType is PluralType)
			setVariableTypes(collectionType)
		else if(collectionType != null)
			setVariableTypes(collectionType)
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		collection.analyseDataFlow(tracker)
		if(iteratorVariableDeclaration != null)
			tracker.declare(iteratorVariableDeclaration, true)
		for(variableDeclaration in variableDeclarations)
			tracker.declare(variableDeclaration, true)
	}

	private fun setVariableTypes(collectionType: PluralType) {
		if(iteratorVariableDeclaration != null)
			context.addIssue(PluralTypeIteratorDeclaration(iteratorVariableDeclaration.source))
		if(variableDeclarations.size > 2) {
			context.addIssue(TooManyPluralTypeVariableDeclarations(source, variableDeclarations))
		} else if(variableDeclarations.size == 2) {
			val indexVariableType = LiteralType(source, scope, SpecialType.INTEGER)
			indexVariableType.determineTypes()
			addSemanticModels(indexVariableType)
			variableDeclarations.firstOrNull()?.type = indexVariableType
		}
		variableDeclarations.lastOrNull()?.type = collectionType.baseType
	}

	private fun setVariableTypes(collectionType: Type) {
		if(!collectionType.isInstanceOf(SpecialType.ITERABLE)) {
			context.addIssue(NotIterable(collection.source))
			return
		}
		try {
			val iteratorCreationProperty = collectionType.interfaceScope.getValueDeclaration("createIterator")
			val iteratorCreationFunctionType = iteratorCreationProperty?.type as? FunctionType
			val iteratorCreationSignature = iteratorCreationFunctionType?.getSignature()
			val iteratorType = iteratorCreationSignature?.getComputedReturnType() ?: return
			iteratorVariableDeclaration?.type = iteratorType
			val availableValueTypes = LinkedList<Type?>()
			if(iteratorType.isInstanceOf(SpecialType.INDEX_ITERATOR)) {
				val indexProperty = iteratorType.interfaceScope.getValueDeclaration("currentIndex")
				availableValueTypes.add(indexProperty?.getLinkedType())
			}
			if(iteratorType.isInstanceOf(SpecialType.KEY_ITERATOR)) {
				val keyProperty = iteratorType.interfaceScope.getValueDeclaration("currentKey")
				availableValueTypes.add(keyProperty?.getLinkedType())
			}
			if(iteratorType.isInstanceOf(SpecialType.VALUE_ITERATOR)) {
				val valueProperty = iteratorType.interfaceScope.getValueDeclaration("currentValue")
				availableValueTypes.add(valueProperty?.getLinkedType())
			}
			if(variableDeclarations.size > availableValueTypes.size) {
				context.addIssue(TooManyIterableVariableDeclarations(source, variableDeclarations, availableValueTypes))
				return
			}
			for(index in variableDeclarations.indices) {
				val sourceValueIndex = availableValueTypes.size - (variableDeclarations.size - index)
				variableDeclarations[index].type = availableValueTypes[sourceValueIndex]
			}
		} catch(error: SignatureResolutionAmbiguityError) {
			error.log(source, "function", "createIterator()")
		}
	}
}
