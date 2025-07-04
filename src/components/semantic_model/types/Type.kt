package components.semantic_model.types

import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmDebugInfoMetadata
import components.code_generation.llvm.wrapper.LlvmType
import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.*
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.InterfaceScope
import components.semantic_model.scopes.Scope
import components.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import errors.internal.CompilerError

abstract class Type(source: SyntaxTreeNode, scope: Scope, isStatic: Boolean = false): SemanticModel(source, scope) {
	val interfaceScope = InterfaceScope(isStatic)
	private var hasResolvedDeclarations = false
	/** A full resolved and simplified version of this type */
	var effectiveType = this
	private var cachedLlvmType: LlvmType? = null
	private var cachedLlvmMetadata: LlvmDebugInfoMetadata? = null

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

	final override fun determineTypes() {
		if(hasResolvedDeclarations)
			return
		hasResolvedDeclarations = true
		resolveTypeDeclarations()
	}

	protected open fun resolveTypeDeclarations() {
		super.determineTypes()
	}

	open fun getLocalType(value: Value, sourceType: Type): Type = this

	open fun isMemberAccessible(signature: FunctionSignature, requireSpecificType: Boolean = false): Boolean = false

	//TODO also infer type parameters from union and plural types
	open fun inferTypeParameter(typeParameter: TypeDeclaration, sourceType: Type, inferredTypes: MutableList<Type>) {}

	open fun getInitializers(): List<InitializerDefinition> = emptyList()
	open fun getAllInitializers(): List<InitializerDefinition> = emptyList()
	abstract fun getTypeDeclaration(name: String): TypeDeclaration?
	abstract fun getValueDeclaration(name: String): ValueDeclaration.Match?

	open fun isInstanceOf(specialType: SpecialType): Boolean = false

	abstract fun accepts(unresolvedSourceType: Type): Boolean

	abstract fun isAssignableTo(unresolvedTargetType: Type): Boolean

	open fun getPotentiallyUnimplementedAbstractMemberDeclarations(): List<Pair<MemberDeclaration, Map<TypeDeclaration, Type>>> =
		throw CompilerError(source, "Tried to get potentially unimplemented abstract member declarations of non-super type.")

	open fun getSpecificMemberDeclarations(): List<Pair<MemberDeclaration, Map<TypeDeclaration, Type>>> =
		throw CompilerError(source, "Tried to get specific member declarations of non-super type.")

	open fun implements(abstractMember: MemberDeclaration, typeSubstitutions: Map<TypeDeclaration, Type>): Boolean =
		throw CompilerError(source, "Tried to check whether a non-super type implements an abstract member.")

	open fun getPropertiesToBeInitialized(): List<PropertyDeclaration> =
		throw CompilerError(source, "Tried to get properties to be initialized of non-super type.")

	open fun getConversionsFrom(sourceType: Type): List<InitializerDefinition> = emptyList()

	fun getLlvmType(constructor: LlvmConstructor): LlvmType {
		var llvmType = cachedLlvmType
		if(llvmType == null) {
			llvmType = createLlvmType(constructor)
			if(context.primitiveCompilationTarget == null)
				this.cachedLlvmType = llvmType
		}
		return llvmType
	}

	protected open fun createLlvmType(constructor: LlvmConstructor): LlvmType { //TODO use 'abstract' modifier when done
		TODO("${source.getStartString()}: '${javaClass.simpleName}.createLlvmType' is not implemented yet.")
	}

	fun getLlvmMetadata(constructor: LlvmConstructor): LlvmDebugInfoMetadata {
		var llvmMetadata = cachedLlvmMetadata
		if(llvmMetadata == null) {
			llvmMetadata = createLlvmMetadata(constructor)
			this.cachedLlvmMetadata = llvmMetadata
		}
		return llvmMetadata
	}

	protected open fun createLlvmMetadata(constructor: LlvmConstructor): LlvmDebugInfoMetadata { //TODO use 'abstract' modifier when done
		TODO("${source.getStartString()}: '${javaClass.simpleName}.createLlvmMetadata' is not implemented yet.")
	}

	open fun isLlvmPrimitive(): Boolean {
		return SpecialType.BOOLEAN.matches(this)
			|| SpecialType.BYTE.matches(this)
			|| SpecialType.INTEGER.matches(this)
			|| SpecialType.FLOAT.matches(this)
	}
}
