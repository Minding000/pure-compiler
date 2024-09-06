package components.semantic_model.declarations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.semantic_model.general.File
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.FileScope
import components.semantic_model.scopes.MutableScope
import components.semantic_model.scopes.TypeScope
import components.semantic_model.types.StaticType
import components.semantic_model.types.Type
import components.semantic_model.values.Function
import components.semantic_model.values.Value
import components.semantic_model.values.VariableValue
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import errors.internal.CompilerError
import logger.issues.constant_conditions.TypeNotAssignable
import logger.issues.declaration.DeclarationMissingTypeOrValue
import logger.issues.resolution.ConversionAmbiguity
import org.bytedeco.llvm.LLVM.LLVMValueRef
import java.util.*

abstract class ValueDeclaration(override val source: SyntaxTreeNode, override val scope: MutableScope, val name: String,
								var providedType: Type? = null, value: Value? = null, val isConstant: Boolean = true,
								val isMutable: Boolean = false): SemanticModel(source, scope) {
	val effectiveType: Type? get() = providedType?.effectiveType
	private var hasDeterminedTypes = false
	open val value = value
	val usages = LinkedList<VariableValue>()
	var conversion: InitializerDefinition? = null
	lateinit var llvmLocation: LLVMValueRef

	init {
		addSemanticModels(providedType, value)
	}

	override fun declare() {
		super.declare()
		scope.addValueDeclaration(this)
	}

	fun getLinkedType(): Type? {
		val type = providedType
		if(type != null) {
			type.determineTypes()
			return type
		}
		determineTypes()
		return this.providedType
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
			if(providedType == null)
				context.addIssue(DeclarationMissingTypeOrValue(source))
			return
		}
		val targetType = providedType
		if(value.isAssignableTo(targetType)) {
			value.setInferredType(targetType)
			return
		}
		val sourceType = value.providedType ?: return
		if(targetType == null) {
			providedType = sourceType
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
		if(providedType is StaticType)
			return
		if(scope is FileScope)
			llvmLocation = constructor.declareGlobal("${name}_Global", effectiveType?.getLlvmType(constructor))
	}

	override fun compile(constructor: LlvmConstructor) {
		val value = value
		if(scope is TypeScope || providedType is StaticType) {
			if(value is Function)
				value.compile(constructor)
			return
		}
		if(scope is FileScope) {
			constructor.defineGlobal(llvmLocation, constructor.nullPointer)
		} else {
			llvmLocation = constructor.buildStackAllocation(effectiveType?.getLlvmType(constructor), "${name}_Variable")
		}
		if(value != null) {
			val llvmValue = ValueConverter.convertIfRequired(this, constructor, value.getLlvmValue(constructor), value.effectiveType,
				value.hasGenericType, effectiveType, false, conversion)
			constructor.buildStore(llvmValue, llvmLocation)
		}
	}

	override fun determineFileInitializationOrder(filesToInitialize: LinkedHashSet<File>) {
		if(hasDeterminedFileInitializationOrder)
			return
		val file = getSurrounding<File>() ?: throw CompilerError(source, "Value declaration outside of file.")
		if(requiresFileRunner()) {
			println("${javaClass.simpleName} '${name}' adds file '${file.file.name}'")
			filesToInitialize.add(file)
		} else {
			//println("${javaClass.simpleName} '${name}' is not at top level of '${file.file.name}'")
		}
		super.determineFileInitializationOrder(filesToInitialize)
		file.determineFileInitializationOrder(filesToInitialize)
		hasDeterminedFileInitializationOrder = true
	}

	open fun requiresFileRunner(): Boolean {
		//TODO what about nested bound enums and objects?
		// - enums are partly handled by instances (but isBound is not taken into consideration)
		return parent is File
	}

	data class Match(val declaration: ValueDeclaration, val whereClauseConditions: List<WhereClauseCondition>?, val type: Type?) {

		constructor(declaration: ValueDeclaration):
			this(declaration, (declaration as? ComputedPropertyDeclaration)?.whereClauseConditions, declaration.getLinkedType())

		fun withTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): Match {
			return Match(declaration, whereClauseConditions?.map { condition -> condition.withTypeSubstitutions(typeSubstitutions) },
				type?.withTypeSubstitutions(typeSubstitutions))
		}
	}
}
