package linter.elements.literals

import linter.Linter
import linter.elements.definitions.InitializerDefinition
import linter.elements.definitions.OperatorDefinition
import linter.elements.general.Unit
import linter.elements.values.TypeDefinition
import linter.elements.values.VariableValueDeclaration
import linter.messages.Message
import linter.scopes.InterfaceScope
import org.bytedeco.llvm.LLVM.LLVMTypeRef

abstract class Type: Unit() {
	val scope = InterfaceScope(this)
	var llvmType: LLVMTypeRef? = null

	abstract fun withTypeSubstitutions(typeSubstitution: Map<Type, Type>): Type

	open fun onNewType(type: TypeDefinition) {
	}

	open fun onNewValue(value: VariableValueDeclaration) {
	}

	open fun onNewInitializer(initializer: InitializerDefinition) {
	}

	open fun onNewOperator(operator: OperatorDefinition) {
	}

	abstract fun accepts(sourceType: Type): Boolean
	abstract fun isAssignableTo(targetType: Type): Boolean

	open fun getKeyType(linter: Linter): Type? {
		linter.messages.add(Message("Type '$this' doesn't have a key type.", Message.Type.ERROR))
		return null
	}

	open fun getValueType(linter: Linter): Type? {
		linter.messages.add(Message("Type '$this' doesn't have a value type.", Message.Type.ERROR))
		return null
	}

//	abstract override fun compile(context: BuildContext): LLVMTypeRef
}