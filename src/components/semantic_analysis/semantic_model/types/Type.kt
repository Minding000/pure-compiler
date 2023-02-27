package components.semantic_analysis.semantic_model.types

//import org.bytedeco.llvm.LLVM.LLVMTypeRef
import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.*
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.InterfaceScope
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.syntax_parser.syntax_tree.general.Element
import errors.internal.CompilerError

abstract class Type(source: Element, scope: Scope, isStatic: Boolean = false): Unit(source, scope) {
	//var llvmType: LLVMTypeRef? = null
	val interfaceScope = InterfaceScope(isStatic)

	init {
		interfaceScope.type = this
	}

	abstract fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): Type

	abstract fun simplified(): Type

	open fun inferType(genericType: TypeDefinition, sourceType: Type, inferredTypes: MutableList<Type>) {}

	open fun onNewType(type: TypeDefinition) {}

	open fun onNewValue(value: InterfaceMember) {}

	open fun onNewInitializer(initializer: InitializerDefinition) {}

	override fun validate(linter: Linter) {
		super.validate(linter)
		//TODO warn if type could be simplified
		// suggestion
		//  - Type could be simplified
		//  -> Simplify to '...'
	}

	open fun isInstanceOf(type: Linter.SpecialType): Boolean = false

	abstract fun accepts(unresolvedSourceType: Type): Boolean

	abstract fun isAssignableTo(unresolvedTargetType: Type): Boolean

	internal fun resolveTypeAlias(sourceType: Type): Type {
		if(sourceType is ObjectType) {
			(sourceType.definition as? TypeAlias)?.let { typeAlias ->
				return resolveTypeAlias(typeAlias.referenceType)
			}
		}
		return sourceType
	}

	open fun getAbstractMembers(): List<MemberDeclaration> = throw CompilerError("Tried to get abstract members of non-super type.")
	open fun getPropertiesToBeInitialized(): List<PropertyDeclaration> =
		throw CompilerError("Tried to get properties to be initialized of non-super type.")

//	abstract override fun compile(context: BuildContext): LLVMTypeRef
}
