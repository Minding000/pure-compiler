package components.semantic_analysis.semantic_model.types

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmType
import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.definitions.MemberDeclaration
import components.semantic_analysis.semantic_model.definitions.PropertyDeclaration
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.InterfaceScope
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import errors.internal.CompilerError

abstract class Type(source: SyntaxTreeNode, scope: Scope, isStatic: Boolean = false): SemanticModel(source, scope) {
	val interfaceScope = InterfaceScope(isStatic)
	private var hasResolvedDefinitions = false
	var effectiveType = this

	init {
		interfaceScope.type = this
	}

	abstract fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): Type

	abstract fun simplified(): Type

	open fun inferType(genericType: TypeDefinition, sourceType: Type, inferredTypes: MutableList<Type>) {}

	open fun onNewType(type: TypeDefinition) {}

	open fun onNewValue(value: InterfaceMember) {}

	open fun onNewInitializer(initializer: InitializerDefinition) {}

	final override fun determineTypes() {
		if(hasResolvedDefinitions)
			return
		hasResolvedDefinitions = true
		resolveDefinitions()
	}

	protected open fun resolveDefinitions() {
		super.determineTypes()
	}

	override fun validate() {
		super.validate()
		//TODO warn if type could be simplified
		// suggestion
		//  - Type could be simplified
		//  -> Simplify to '...'
	}

	open fun isInstanceOf(type: SpecialType): Boolean = false

	abstract fun accepts(unresolvedSourceType: Type): Boolean

	abstract fun isAssignableTo(unresolvedTargetType: Type): Boolean

	open fun getAbstractMembers(): List<MemberDeclaration> =
		throw CompilerError(source, "Tried to get abstract members of non-super type.")
	open fun getPropertiesToBeInitialized(): List<PropertyDeclaration> =
		throw CompilerError(source, "Tried to get properties to be initialized of non-super type.")

	open fun getConversionsFrom(sourceType: Type): List<InitializerDefinition> = emptyList()

	open fun getLlvmReference(constructor: LlvmConstructor): LlvmType {
		TODO("'${javaClass.simpleName}.getLlvmReference' is not implemented yet.") //TODO make this function abstract
	}
}
