package components.syntax_parser.syntax_tree.definitions

import components.semantic_model.scopes.BlockScope
import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.sections.FunctionSection
import components.syntax_parser.syntax_tree.general.StatementSection
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import components.syntax_parser.syntax_tree.literals.Identifier
import components.tokenizer.WordAtom
import components.semantic_model.declarations.FunctionImplementation as SemanticFunctionImplementationModel

class FunctionDefinition(private val identifier: Identifier, private val parameterList: ParameterList,
						 private var returnType: TypeSyntaxTreeNode?, private val whereClause: WhereClause?,
						 private val body: StatementSection?):
	SyntaxTreeNode(identifier.start, body?.end ?: returnType?.end ?: parameterList.end) {
	lateinit var parent: FunctionSection

	companion object {
		val ALLOWED_MODIFIERS = listOf(WordAtom.ABSTRACT, WordAtom.MUTATING, WordAtom.NATIVE, WordAtom.OVERRIDING, WordAtom.SPECIFIC,
			WordAtom.MONOMORPHIC)
	}

	override fun toSemanticModel(scope: MutableScope): SemanticFunctionImplementationModel {
		parent.validate(ALLOWED_MODIFIERS)
		val isAbstract = parent.containsModifier(WordAtom.ABSTRACT)
		val isMutating = parent.containsModifier(WordAtom.MUTATING)
		val isNative = parent.containsModifier(WordAtom.NATIVE)
		val isOverriding = parent.containsModifier(WordAtom.OVERRIDING)
		val isSpecific = parent.containsModifier(WordAtom.SPECIFIC)
		val isMonomorphic = parent.containsModifier(WordAtom.MONOMORPHIC)
		val functionScope = BlockScope(scope)
		val localTypeParameters = parameterList.getSemanticGenericParameterModels(functionScope) ?: emptyList()
		val parameters = parameterList.getSemanticParameterModels(functionScope)
		val returnType = returnType?.toSemanticModel(functionScope)
		val whereClauseConditions = whereClause?.toWhereClauseConditionSemanticModels(functionScope) ?: emptyList()
		return SemanticFunctionImplementationModel(this, functionScope, localTypeParameters, parameters,
			body?.toSemanticModel(functionScope), returnType, whereClauseConditions, isAbstract, isMutating, isNative, isOverriding,
			isSpecific, isMonomorphic)
	}

	fun getName(): String = identifier.getValue()

	override fun toString(): String {
		val stringRepresentation = StringBuilder()
			.append("Function [ ")
			.append(identifier)
			.append(" ")
			.append(parameterList)
			.append(": ")
			.append(returnType ?: "void")
		if(whereClause != null)
			stringRepresentation.append(" $whereClause")
		stringRepresentation.append(" ]")
		if(body != null)
			stringRepresentation
				.append(" { ")
				.append(body)
				.append(" }")
		return stringRepresentation.toString()
	}
}
