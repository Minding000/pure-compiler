package components.semantic_analysis.semantic_model.values

import components.compiler.targets.llvm.LlvmConstructor
import components.semantic_analysis.semantic_model.declarations.InitializerDefinition
import components.semantic_analysis.semantic_model.declarations.TypeDeclaration
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.FileScope
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.issues.constant_conditions.TypeNotAssignable
import logger.issues.declaration.DeclarationMissingTypeOrValue
import logger.issues.resolution.ConversionAmbiguity
import org.bytedeco.llvm.LLVM.LLVMValueRef
import java.util.*

abstract class ValueDeclaration(override val source: SyntaxTreeNode, override val scope: MutableScope, val name: String,
								var type: Type? = null, value: Value? = null, val isConstant: Boolean = true,
								val isMutable: Boolean = false, val isSpecificCopy: Boolean = false): SemanticModel(source, scope) {
	private var hasDeterminedTypes = isSpecificCopy
	open val value = value
	val usages = LinkedList<VariableValue>()
	private var conversion: InitializerDefinition? = null
	lateinit var llvmLocation: LLVMValueRef

	init {
		addSemanticModels(type, value)
	}

	abstract fun withTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): ValueDeclaration

	override fun declare() {
		super.declare()
		scope.addValueDeclaration(this)
	}

	open fun getLinkedType(): Type? {
		val type = type
		if(type != null) {
			type.determineTypes()
			return type
		}
		determineTypes()
		return this.type
	}

	override fun determineTypes() {
		if(hasDeterminedTypes)
			return
		hasDeterminedTypes = true
		determineType()
	}

	protected open fun determineType() {
		super.determineTypes()
		val value = value
		if(value == null) {
			if(type == null)
				context.addIssue(DeclarationMissingTypeOrValue(source))
			return
		}
		val targetType = type
		if(value.isAssignableTo(targetType)) {
			value.setInferredType(targetType)
			return
		}
		val sourceType = value.type ?: return
		if(targetType == null) {
			type = sourceType
			return
		}
		val conversions = targetType.getConversionsFrom(sourceType)
		if(conversions.isNotEmpty()) {
			if(conversions.size > 1) {
				context.addIssue(ConversionAmbiguity(source, sourceType, targetType, conversions))
				return
			}
			conversion = conversions.first()
			return
		}
		context.addIssue(TypeNotAssignable(source, sourceType, targetType))
	}

	override fun compile(constructor: LlvmConstructor) {
		if(scope is TypeScope || type is StaticType) {
			super.compile(constructor)
			return
		}
		val llvmType = type?.getLlvmType(constructor)
		if(scope is FileScope) {
			llvmLocation = constructor.buildGlobal("${name}_Global", llvmType, constructor.nullPointer)
		} else {
			val currentBlock = constructor.getCurrentBlock()
			val function = constructor.getParentFunction(currentBlock)
			val entryBlock = constructor.getEntryBlock(function)
			constructor.select(entryBlock)
			llvmLocation = constructor.buildStackAllocation(llvmType, "${name}_Variable")
			constructor.select(currentBlock)
		}
		val value = value
		if(value != null)
			constructor.buildStore(value.getLlvmValue(constructor), llvmLocation)
	}
}
