package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.general.Unit
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
	Unit(source, scope) {

	init {
		addUnits(collection, iteratorVariableDeclaration)
		addUnits(variableDeclarations)
	}

	override fun determineTypes(linter: Linter) {
		collection.determineTypes(linter)
		val collectionType = collection.type
		if(collectionType is PluralType)
			setVariableTypes(linter, collectionType)
		else if(collectionType != null)
			setVariableTypes(linter, collectionType)
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		collection.analyseDataFlow(tracker)
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
			val indexVariableType = LiteralType(source, scope, Linter.SpecialType.INTEGER)
			indexVariableType.determineTypes(linter)
			addUnits(indexVariableType)
			variableDeclarations.firstOrNull()?.type = indexVariableType
		}
		variableDeclarations.lastOrNull()?.type = collectionType.baseType
	}

	private fun setVariableTypes(linter: Linter, collectionType: Type) {
		if(!collectionType.isInstanceOf(Linter.SpecialType.ITERABLE)) {
			linter.addIssue(NotIterable(collection.source))
			return
		}
		try {
			val iteratorCreationProperty = collectionType.interfaceScope.resolveValue("createIterator")
			val iteratorCreationFunctionType = iteratorCreationProperty?.type as? FunctionType
			val iteratorCreationSignature = iteratorCreationFunctionType?.resolveSignature(linter)
			val iteratorType = iteratorCreationSignature?.returnType ?: return
			iteratorVariableDeclaration?.type = iteratorType
			val availableValueTypes = LinkedList<Type?>()
			if(iteratorType.isInstanceOf(Linter.SpecialType.INDEX_ITERATOR)) {
				val indexProperty = iteratorType.interfaceScope.resolveValue("currentIndex")
				availableValueTypes.add(indexProperty?.getType(linter))
			}
			if(iteratorType.isInstanceOf(Linter.SpecialType.KEY_ITERATOR)) {
				val keyProperty = iteratorType.interfaceScope.resolveValue("currentKey")
				availableValueTypes.add(keyProperty?.getType(linter))
			}
			if(iteratorType.isInstanceOf(Linter.SpecialType.VALUE_ITERATOR)) {
				val valueProperty = iteratorType.interfaceScope.resolveValue("currentValue")
				availableValueTypes.add(valueProperty?.getType(linter))
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
