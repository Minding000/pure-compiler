package parsing.ast.definitions

import linter.Linter
import linter.elements.definitions.FunctionImplementation
import linter.scopes.BlockScope
import linter.scopes.MutableScope
import parsing.ast.definitions.sections.FunctionSection
import parsing.ast.general.Element
import parsing.ast.general.StatementSection
import parsing.ast.literals.Identifier
import parsing.ast.general.TypeElement
import parsing.tokenizer.WordAtom
import java.lang.StringBuilder

class FunctionDefinition(private val identifier: Identifier, private val genericsList: GenericsList?,
						 private val parameterList: ParameterList, private val body: StatementSection?,
						 private var returnType: TypeElement?):
	Element(identifier.start, body?.end ?: returnType?.end ?: parameterList.end) {
	lateinit var parent: FunctionSection

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.NATIVE)
	}

	override fun concretize(linter: Linter, scope: MutableScope): FunctionImplementation {
		parent.validate(linter, ALLOWED_MODIFIER_TYPES)
		val isNative = parent.containsModifier(WordAtom.NATIVE)
		val functionScope = BlockScope(scope)
		val genericParameters = genericsList?.concretizeGenerics(linter, scope) ?: listOf()
		val parameters = parameterList.concretizeParameters(linter, functionScope)
		val returnType = returnType?.concretize(linter, scope)
		val implementation = FunctionImplementation(this, functionScope, genericParameters, parameters,
			body?.concretize(linter, functionScope), returnType, isNative)
		scope.declareFunction(linter, identifier.getValue(), implementation)
		return implementation
	}

	override fun toString(): String {
		val string = StringBuilder()
		string.append("Function [ ").append(identifier)
		if(genericsList != null)
			string.append(" ")
				.append(genericsList)
		string.append(" ")
			.append(parameterList)
			.append(": ")
			.append(returnType ?: "void")
			.append(" ] { ")
			.append(body)
			.append(" }")
		return string.toString()
	}
}