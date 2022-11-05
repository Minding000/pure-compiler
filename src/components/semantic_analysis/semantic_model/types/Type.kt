package components.semantic_analysis.semantic_model.types

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.definitions.OperatorDefinition
import components.semantic_analysis.semantic_model.definitions.TypeAlias
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.values.VariableValueDeclaration
import messages.Message
import components.semantic_analysis.semantic_model.scopes.InterfaceScope
//import org.bytedeco.llvm.LLVM.LLVMTypeRef
import components.syntax_parser.syntax_tree.general.Element

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
