package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Value
import messages.Message
import util.stringifyTypes
import java.util.*
import components.syntax_parser.syntax_tree.definitions.OperatorDefinition as OperatorDefinitionSyntaxTree

open class OperatorDefinition(final override val source: OperatorDefinitionSyntaxTree, val kind: Kind,
							  val scope: BlockScope, val valueParameters: List<Parameter>, val body: Unit?,
							  returnType: Type?, val isAbstract: Boolean, val isNative: Boolean,
							  val isOverriding: Boolean): Unit(source) {
	val returnType: Type

	init {
		addUnits(body)
		addUnits(valueParameters)
		var type = returnType
		if(type == null)
			type = ObjectType(source, Linter.LiteralType.NOTHING.className)
		addUnits(type)
		this.returnType = type
	}

	open fun withTypeSubstitutions(typeSubstitution: Map<TypeDefinition, Type>): OperatorDefinition {
		val specificParameters = LinkedList<Parameter>()
		for(parameter in valueParameters)
			specificParameters.add(parameter.withTypeSubstitutions(typeSubstitution))
		return OperatorDefinition(source, kind, scope, specificParameters, body,
				returnType.withTypeSubstitutions(typeSubstitution), isAbstract, isNative, isOverriding)
	}

	fun accepts(suppliedValues: List<Value>): Boolean {
		if(valueParameters.size != suppliedValues.size)
			return false
		for(parameterIndex in valueParameters.indices)
			if(!suppliedValues[parameterIndex].isAssignableTo(valueParameters[parameterIndex].type))
				return false
		return true
	}

	fun isMoreSpecificThan(otherSignature: OperatorDefinition): Boolean {
		if(otherSignature.valueParameters.size != valueParameters.size)
			return false
		var areSignaturesEqual = true
		for(parameterIndex in valueParameters.indices) {
			val parameterType = valueParameters[parameterIndex].type ?: return false
			val otherParameterType = otherSignature.valueParameters[parameterIndex].type
			if(otherParameterType == null) {
				areSignaturesEqual = false
				continue
			}
			if(otherParameterType != parameterType) {
				areSignaturesEqual = false
				if(!otherParameterType.accepts(parameterType))
					return false
			}
		}
		return !areSignaturesEqual
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		for(unit in units) {
			if(Linter.LiteralType.NOTHING.matches(unit)) {
				linter.link(Linter.LiteralType.NOTHING, unit)
				continue
			}
			unit.linkTypes(linter, this.scope)
		}
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, this.scope)
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		if(kind.returnsValue && Linter.LiteralType.NOTHING.matches(returnType))
			linter.addMessage(source, "The operator is expected to return a value.", Message.Type.WARNING)
		if(kind.isUnary) {
			if(valueParameters.size > 1 || !kind.isBinary && valueParameters.isNotEmpty())
				linter.addMessage(source, "Unary operators can't accept parameters.", Message.Type.WARNING)
		}
		if(kind.isBinary) {
			if(valueParameters.size > 1 || !kind.isUnary && valueParameters.isEmpty())
				linter.addMessage(source, "Binary operators need to accept exactly one parameter.",
					Message.Type.WARNING)
		}
	}

	override fun toString(): String {
		return "$kind(${valueParameters.stringifyTypes()})"
	}

	enum class Kind(val stringRepresentation: String, val isUnary: Boolean, val isBinary: Boolean,
					val returnsValue: Boolean) {
		BRACKETS_GET("[]", true, false, true),
		BRACKETS_SET("[]=", false, true, false),
		EXCLAMATION_MARK("!", true, false, true),
		TRIPLE_DOT("...", true, false, true),
		DOUBLE_PLUS("++", true, false, false),
		DOUBLE_MINUS("--", true, false, false),
		DOUBLE_QUESTION_MARK("??", false, true, true),
		AND("&", false, true, true),
		PIPE("|", false, true, true),
		PLUS("+", false, true, true),
		MINUS("-", true, true, true),
		STAR("*", false, true, true),
		SLASH("/", false, false, true),
		PLUS_EQUALS("+=", false, true, false),
		MINUS_EQUALS("-=", false, true, false),
		STAR_EQUALS("*=", false, true, false),
		SLASH_EQUALS("/=", false, true, false),
		SMALLER_THAN("<", false, true, true),
		GREATER_THAN(">", false, true, true),
		SMALLER_THAN_OR_EQUAL_TO("<=", false, true, true),
		GREATER_THAN_OR_EQUAL_TO(">=", false, true, true),
		EQUAL_TO("==", false, true, true),
		NOT_EQUAL_TO("!=", false, true, true);

		override fun toString(): String = stringRepresentation
	}
}
