package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.ErrorHandlingContext
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.Function
import components.syntax_parser.syntax_tree.general.Element

class FunctionImplementation(override val source: Element, override val parentDefinition: TypeDefinition?,
							 val scope: BlockScope, genericParameters: List<TypeDefinition>,
							 val parameters: List<Parameter>, body: ErrorHandlingContext?, returnType: Type?,
							 override val isAbstract: Boolean = false, val isMutating: Boolean = false,
							 val isNative: Boolean = false, val isOverriding: Boolean = false):
	Unit(source), MemberDeclaration {
	override lateinit var signatureString: String
	lateinit var parentFunction: Function
	val signature: FunctionSignature = FunctionSignature(source, scope, genericParameters,
		parameters.map { parameter -> parameter.type }, returnType, true)
	var superFunctionImplementation: FunctionImplementation? = null
		set(value) {
			field = value
			signature.superFunctionSignature = value?.signature
		}

	init {
		addUnits(body)
		addUnits(parameters)
	}

	fun setParent(function: Function) {
		parentFunction = function
		signatureString = "${function.name}${signature.toString(false)}"
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		super.linkTypes(linter, this.scope)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, this.scope)
	}

//	override fun compile(context: BuildContext): LLVMValueRef {
//		val argumentTypes = PointerPointer<Pointer>(parameters.size.toLong())
//		for(parameter in parameters)
//			argumentTypes.put(parameter.compile(context))
//		val returnType = returnType?.compile(context) ?: LLVMVoidType()
//		val functionType = LLVMFunctionType(returnType, argumentTypes, parameters.size, LLVMIRCompiler.LLVM_NO)
//		val function = LLVMAddFunction(context.module, name, functionType)
//		LLVMSetFunctionCallConv(function, LLVMCCallConv)
//		return function
//	}
}
