package parsing.ast.definitions

import linter.Linter
import linter.elements.definitions.InitializerDefinition
import linter.elements.definitions.Parameter as LinterParameter
import linter.scopes.BlockScope
import linter.scopes.Scope
import parsing.ast.definitions.sections.ModifierSection
import parsing.ast.definitions.sections.ModifierSectionChild
import parsing.ast.general.Element
import parsing.ast.general.StatementSection
import parsing.tokenizer.WordAtom
import source_structure.Position
import java.util.*

class InitializerDefinition(start: Position, private val parameterList: ParameterList?,
							private val body: StatementSection?, end: Position):
	Element(start, end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.NATIVE)
	}

	override fun concretize(linter: Linter, scope: Scope): InitializerDefinition {
		parent?.validate(linter, ALLOWED_MODIFIER_TYPES)
		val isNative = parent?.containsModifier(WordAtom.NATIVE) ?: false
		val initializerScope = BlockScope(scope)
		val parameters = LinkedList<LinterParameter>()
		if(parameterList != null) {
			for(parameter in parameterList.parameters)
				parameters.add(parameter.concretize(linter, initializerScope))
		}
		return InitializerDefinition(this, initializerScope, parameters,
			body?.concretize(linter, initializerScope), isNative)
	}

	override fun toString(): String {
		return "Initializer [ $parameterList ] { ${body ?: ""} }"
	}
}