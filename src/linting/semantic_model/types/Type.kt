package linting.semantic_model.types

import linting.Linter
import linting.semantic_model.definitions.InitializerDefinition
import linting.semantic_model.definitions.OperatorDefinition
import linting.semantic_model.definitions.TypeAlias
import linting.semantic_model.general.Unit
import linting.semantic_model.definitions.TypeDefinition
import linting.semantic_model.values.VariableValueDeclaration
import messages.Message
import linting.semantic_model.scopes.InterfaceScope
//import org.bytedeco.llvm.LLVM.LLVMTypeRef
import parsing.syntax_tree.general.Element

abstract class Type(source: Element): Unit(source) {
	val scope = InterfaceScope(this)
	//var llvmType: LLVMTypeRef? = null

	abstract fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): Type

	open fun inferType(genericType: TypeDefinition, sourceType: Type, inferredTypes: MutableSet<Type>) {}

	open fun onNewType(type: TypeDefinition) {}

	open fun onNewValue(value: VariableValueDeclaration) {}

	open fun onNewInitializer(initializer: InitializerDefinition) {}

	open fun onNewOperator(operator: OperatorDefinition) {}

	abstract fun accepts(unresolvedSourceType: Type): Boolean
	abstract fun isAssignableTo(unresolvedTargetType: Type): Boolean

	open fun getKeyType(linter: Linter): Type? {
		linter.addMessage("Type '$this' doesn't have a key type.", Message.Type.ERROR)
		return null
	}

	open fun getValueType(linter: Linter): Type? {
		linter.addMessage("Type '$this' doesn't have a value type.", Message.Type.ERROR)
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
