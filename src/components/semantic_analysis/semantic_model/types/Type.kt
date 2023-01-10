package components.semantic_analysis.semantic_model.types

//import org.bytedeco.llvm.LLVM.LLVMTypeRef
import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.definitions.MemberDeclaration
import components.semantic_analysis.semantic_model.definitions.TypeAlias
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.InterfaceScope
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.syntax_parser.syntax_tree.general.Element
import java.util.*

abstract class Type(source: Element): Unit(source) {
	val scope = InterfaceScope(this)
	//var llvmType: LLVMTypeRef? = null

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

	open fun getAbstractMembers(): List<MemberDeclaration> = LinkedList()

//	abstract override fun compile(context: BuildContext): LLVMTypeRef
}
