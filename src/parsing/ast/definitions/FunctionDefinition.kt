package parsing.ast.definitions

import linter.Linter
import linter.elements.definitions.FunctionDefinition
import linter.elements.definitions.Parameter
import linter.elements.general.Unit
import linter.messages.Message
import linter.scopes.BlockScope
import linter.scopes.Scope
import parsing.ast.definitions.sections.FunctionSection
import parsing.ast.general.Element
import parsing.ast.general.StatementSection
import parsing.ast.literals.Identifier
import parsing.ast.literals.Type
import java.lang.StringBuilder
import java.util.*

class FunctionDefinition(private val identifier: Identifier, private val genericsList: GenericsList?,
						 private val parameterList: ParameterList, private val body: StatementSection?,
						 private var returnType: Type?):
	Element(identifier.start, body?.end ?: returnType?.end ?: parameterList.end) {
	lateinit var parent: FunctionSection

	override fun concretize(linter: Linter, scope: Scope): FunctionDefinition {
		val functionScope = BlockScope(scope)
		var isNative = false
		for(modifier in parent.getModifiers(linter)) {
			when(val name = modifier.getValue()) {
				"native" -> isNative = true
				else -> linter.messages.add(Message("Modifier '$name' is not applicable to functions.", Message.Type.ERROR))
			}
		}
		val genericParameters = LinkedList<Unit>()
		if(genericsList != null) {
			for(parameter in genericsList.elements) {
				//TODO continue...
			}
		}
		val parameters = LinkedList<Parameter>()
		for(parameter in parameterList.parameters)
			parameters.add(parameter.concretize(linter, functionScope))
		val functionDefinition = FunctionDefinition(this, identifier.getValue(), functionScope, genericParameters,
			parameters, body?.concretize(linter, functionScope), returnType?.concretize(linter, functionScope), isNative)
		scope.declareFunction(linter, functionDefinition)
		return functionDefinition
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