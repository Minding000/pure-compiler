package components.semantic_model.values

import components.code_generation.llvm.LlvmConstructor
import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.FileScope
import components.semantic_model.scopes.MutableScope
import components.semantic_model.scopes.TypeScope
import components.semantic_model.types.OptionalType
import components.semantic_model.types.StaticType
import components.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.issues.constant_conditions.TypeNotAssignable
import logger.issues.declaration.DeclarationMissingTypeOrValue
import logger.issues.resolution.ConversionAmbiguity
import org.bytedeco.llvm.LLVM.LLVMValueRef
import java.util.*

abstract class ValueDeclaration(override val source: SyntaxTreeNode, override val scope: MutableScope, val name: String,
								var type: Type? = null, value: Value? = null, val isConstant: Boolean = true,
								val isMutable: Boolean = false): SemanticModel(source, scope) {
	private var hasDeterminedTypes = false
	open val value = value
	val usages = LinkedList<VariableValue>()
	private var conversion: InitializerDefinition? = null
	lateinit var llvmLocation: LLVMValueRef

	init {
		addSemanticModels(type, value)
	}

	override fun declare() {
		super.declare()
		scope.addValueDeclaration(this)
	}

	fun getLinkedType(): Type? {
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

	override fun declare(constructor: LlvmConstructor) {
		super.declare(constructor)
		if(type is StaticType)
			return
		if(scope is FileScope)
			llvmLocation = constructor.declareGlobal("${name}_Global", type?.getLlvmType(constructor))
	}

	override fun compile(constructor: LlvmConstructor) {
		val value = value
		if(scope is TypeScope || type is StaticType) {
			if(value is Function)
				value.compile(constructor)
			return
		}
		if(scope is FileScope) {
			constructor.defineGlobal(llvmLocation, constructor.nullPointer)
		} else {
			llvmLocation = constructor.buildStackAllocation(type?.getLlvmType(constructor), "${name}_Variable")
		}
		if(value != null) {
			val llvmValue = value.getLlvmValue(constructor)
			val valueType = value.type
			if(type is OptionalType && valueType?.isLlvmPrimitive() == true) {
				val box = constructor.buildHeapAllocation(valueType.getLlvmType(constructor), "_optionalPrimitiveBox")
				constructor.buildStore(llvmValue, box)
				constructor.buildStore(box, llvmLocation)
			} else {
				constructor.buildStore(llvmValue, llvmLocation)
			}
		}
	}
}
