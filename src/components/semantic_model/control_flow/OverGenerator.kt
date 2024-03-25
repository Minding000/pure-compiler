package components.semantic_model.control_flow

import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.semantic_model.declarations.LocalVariableDeclaration
import components.semantic_model.declarations.ValueDeclaration
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import components.semantic_model.types.FunctionType
import components.semantic_model.types.LiteralType
import components.semantic_model.types.PluralType
import components.semantic_model.types.Type
import components.semantic_model.values.Value
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.loops.NotIterable
import logger.issues.loops.PluralTypeIteratorDeclaration
import logger.issues.loops.TooManyIterableVariableDeclarations
import logger.issues.loops.TooManyPluralTypeVariableDeclarations
import java.util.*
import components.syntax_parser.syntax_tree.control_flow.OverGenerator as OverGeneratorSyntaxTree

class OverGenerator(override val source: OverGeneratorSyntaxTree, scope: Scope, val iterable: Value,
					val iteratorVariableDeclaration: LocalVariableDeclaration?, val variableDeclarations: List<LocalVariableDeclaration>):
	SemanticModel(source, scope) {
	var currentIndexVariable: ValueDeclaration? = null
	var currentKeyVariable: ValueDeclaration? = null
	var currentValueVariable: ValueDeclaration? = null

	init {
		addSemanticModels(iterable, iteratorVariableDeclaration)
		addSemanticModels(variableDeclarations)
	}

	override fun determineTypes() {
		iterable.determineTypes()
		val iterableType = iterable.providedType
		if(iterableType is PluralType)
			setVariableTypes(iterableType)
		else if(iterableType != null)
			setVariableTypes(iterableType)
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		iterable.analyseDataFlow(tracker)
		if(iteratorVariableDeclaration != null)
			tracker.declare(iteratorVariableDeclaration, true)
		for(variableDeclaration in variableDeclarations)
			tracker.declare(variableDeclaration, true)
	}

	private fun setVariableTypes(iterableType: PluralType) {
		if(iteratorVariableDeclaration != null)
			context.addIssue(PluralTypeIteratorDeclaration(iteratorVariableDeclaration.source))
		if(variableDeclarations.size > 2) {
			context.addIssue(TooManyPluralTypeVariableDeclarations(source, variableDeclarations))
		} else if(variableDeclarations.size == 2) {
			val indexVariableType = LiteralType(source, scope, SpecialType.INTEGER)
			indexVariableType.determineTypes()
			addSemanticModels(indexVariableType)
			currentIndexVariable = variableDeclarations.firstOrNull()
			currentIndexVariable?.type = indexVariableType
		}
		currentValueVariable = variableDeclarations.lastOrNull()
		currentValueVariable?.type = iterableType.baseType
	}

	private fun setVariableTypes(iterableType: Type) {
		if(!iterableType.isInstanceOf(SpecialType.ITERABLE)) {
			context.addIssue(NotIterable(iterable.source))
			return
		}
		try {
			val iteratorCreationPropertyType = iterableType.interfaceScope.getValueDeclaration("createIterator")?.type
			val iteratorCreationFunctionType = iteratorCreationPropertyType as? FunctionType
			val iteratorType = iteratorCreationFunctionType?.getSignature()?.returnType ?: return
			iteratorVariableDeclaration?.type = iteratorType
			var variableIndex = variableDeclarations.size - 1
			val availableValueTypes = LinkedList<Type?>()
			if(iteratorType.isInstanceOf(SpecialType.VALUE_ITERATOR)) {
				val valuePropertyType = iteratorType.interfaceScope.getValueDeclaration("currentValue")?.type
				currentValueVariable = variableDeclarations.getOrNull(variableIndex--)
				currentValueVariable?.type = valuePropertyType
				availableValueTypes.add(valuePropertyType)
			}
			if(iteratorType.isInstanceOf(SpecialType.KEY_ITERATOR)) {
				val keyPropertyType = iteratorType.interfaceScope.getValueDeclaration("currentKey")?.type
				currentKeyVariable = variableDeclarations.getOrNull(variableIndex--)
				currentKeyVariable?.type = keyPropertyType
				availableValueTypes.add(keyPropertyType)
			}
			if(iteratorType.isInstanceOf(SpecialType.INDEX_ITERATOR)) {
				val indexPropertyType = iteratorType.interfaceScope.getValueDeclaration("currentIndex")?.type
				currentIndexVariable = variableDeclarations.getOrNull(variableIndex)
				currentIndexVariable?.type = indexPropertyType
				availableValueTypes.add(indexPropertyType)
			}
			if(variableDeclarations.size > availableValueTypes.size)
				context.addIssue(TooManyIterableVariableDeclarations(source, variableDeclarations, availableValueTypes))
		} catch(error: SignatureResolutionAmbiguityError) {
			error.log(source, "function", "createIterator()")
		}
	}
}
