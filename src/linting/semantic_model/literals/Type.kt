package linting.semantic_model.literals

import linting.Linter
import linting.semantic_model.definitions.InitializerDefinition
import linting.semantic_model.definitions.OperatorDefinition
import linting.semantic_model.definitions.TypeAlias
import linting.semantic_model.general.Unit
import linting.semantic_model.values.TypeDefinition
import linting.semantic_model.values.VariableValueDeclaration
import linting.messages.Message
import linting.semantic_model.scopes.InterfaceScope
import org.bytedeco.llvm.LLVM.LLVMTypeRef

abstract class Type: Unit() {
	val scope = InterfaceScope(this)
	var llvmType: LLVMTypeRef? = null

	abstract fun withTypeSubstitutions(typeSubstitution: Map<ObjectType, Type>): Type

	open fun onNewType(type: TypeDefinition) {
	}

	open fun onNewValue(value: VariableValueDeclaration) {
	}

	open fun onNewInitializer(initializer: InitializerDefinition) {
	}

	open fun onNewOperator(operator: OperatorDefinition) {
	}

	abstract fun accepts(unresolvedSourceType: Type): Boolean
	abstract fun isAssignableTo(unresolvedTargetType: Type): Boolean

	open fun getKeyType(linter: Linter): Type? {
		linter.messages.add(Message("Type '$this' doesn't have a key type.", Message.Type.ERROR))
		return null
	}

	open fun getValueType(linter: Linter): Type? {
		linter.messages.add(Message("Type '$this' doesn't have a value type.", Message.Type.ERROR))
		return null
	}

	internal fun resolveTypeAlias(sourceType: Type): Type {
		if(sourceType is ObjectType) {
			(sourceType.definition as? TypeAlias)?.let { typeAlias ->
				return resolveTypeAlias(typeAlias.referenceType)
			}
		}
		return sourceType
	}

//	abstract override fun compile(context: BuildContext): LLVMTypeRef
}