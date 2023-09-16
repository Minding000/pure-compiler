package components.semantic_analysis.semantic_model.types

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmType
import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.declarations.InitializerDefinition
import components.semantic_analysis.semantic_model.declarations.MemberDeclaration
import components.semantic_analysis.semantic_model.declarations.PropertyDeclaration
import components.semantic_analysis.semantic_model.declarations.TypeDeclaration
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.InterfaceScope
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import errors.internal.CompilerError

abstract class Type(source: SyntaxTreeNode, scope: Scope, isStatic: Boolean = false): SemanticModel(source, scope) {
	val interfaceScope = InterfaceScope(isStatic)
	private var hasResolvedDeclarations = false
	var effectiveType = this
	private var cachedLlvmType: LlvmType? = null

	init {
		interfaceScope.type = this
	}

	open fun withTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): Type {
		if(typeSubstitutions.isEmpty())
			return this
		return createCopyWithTypeSubstitutions(typeSubstitutions)
	}

	protected abstract fun createCopyWithTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): Type

	abstract fun simplified(): Type

	//TODO also infer type parameters from union and plural types
	open fun inferTypeParameter(typeParameter: TypeDeclaration, sourceType: Type, inferredTypes: MutableList<Type>) {}

	open fun onNewTypeDeclaration(newTypeDeclaration: TypeDeclaration) {}

	open fun onNewInterfaceMember(newInterfaceMember: InterfaceMember) {}

	open fun onNewInitializer(newInitializer: InitializerDefinition) {}

	abstract fun getValueDeclaration(name: String): Pair<ValueDeclaration?, Type?>

	final override fun determineTypes() {
		if(hasResolvedDeclarations)
			return
		hasResolvedDeclarations = true
		resolveTypeDeclarations()
	}

	protected open fun resolveTypeDeclarations() {
		super.determineTypes()
	}

	override fun validate() {
		super.validate()
		//TODO warn if type could be simplified
		// suggestion
		//  - Type could be simplified
		//  -> Simplify to '...'
	}

	open fun isInstanceOf(specialType: SpecialType): Boolean = false

	abstract fun accepts(unresolvedSourceType: Type): Boolean

	abstract fun isAssignableTo(unresolvedTargetType: Type): Boolean

	open fun getAbstractMemberDeclarations(): List<Pair<MemberDeclaration, Map<TypeDeclaration, Type>>> =
		throw CompilerError(source, "Tried to get abstract member declarations of non-super type.")
	open fun getPropertiesToBeInitialized(): List<PropertyDeclaration> =
		throw CompilerError(source, "Tried to get properties to be initialized of non-super type.")

	open fun getConversionsFrom(sourceType: Type): List<InitializerDefinition> = emptyList()

	fun getLlvmType(constructor: LlvmConstructor): LlvmType {
		var llvmType = cachedLlvmType
		if(llvmType == null) {
			llvmType = createLlvmType(constructor)
			this.cachedLlvmType = llvmType
		}
		return llvmType
	}

	protected open fun createLlvmType(constructor: LlvmConstructor): LlvmType { //TODO use 'abstract' modifier when done
		TODO("${source.getStartString()}: '${javaClass.simpleName}.createLlvmType' is not implemented yet.")
	}
}
