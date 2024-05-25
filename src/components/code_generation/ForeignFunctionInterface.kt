package components.code_generation

import errors.internal.CompilerError
import org.bytedeco.javacpp.Pointer
import org.bytedeco.libffi.ffi_cif
import org.bytedeco.libffi.ffi_type
import org.bytedeco.libffi.global.ffi.*
import util.toLlvmList

class ForeignFunctionInterface {
	private val callInterface = ffi_cif()
	private var isSignatureSet = false
	private var parameterCount = 0

	companion object {
		val booleanType = ffi_type_uint8()
		val signedByteType = ffi_type_sint8()
		val signedIntegerType = ffi_type_sint()
		val floatType = ffi_type_float()
		val voidType = ffi_type_void()
	}

	fun setSignature(parameterTypes: List<ForeignFunctionType>, returnType: ForeignFunctionType) {
		if(isSignatureSet)
			throw CompilerError("Foreign function signature has already been set.")
		parameterCount = parameterTypes.size
		val status = ffi_prep_cif(callInterface, FFI_DEFAULT_ABI(), parameterCount, returnType, parameterTypes.toLlvmList())
		if(status != FFI_OK)
			throw CompilerError("Failed to prepare the libffi call interface.")
		isSignatureSet = true
	}

	/**
	 * Use this overload when the return type is 'void'.
	 */
	fun call(address: Pointer, parameterValues: List<ForeignFunctionValue> = emptyList()) {
		val ignoredPointer = Pointer()
		call(address, parameterValues, ignoredPointer)
	}

	fun call(address: Pointer, parameterValues: List<ForeignFunctionValue>, returnValuePointer: ForeignFunctionValue) {
		if(!isSignatureSet)
			throw CompilerError("Foreign function signature hasn't been set yet.")
		if(parameterValues.size != parameterCount)
			throw CompilerError(
				"Foreign function called with wrong number of parameters (supplied ${parameterValues.size}, but expected ${parameterCount}).")
		ffi_call(callInterface, address, returnValuePointer, parameterValues.toLlvmList())
	}
}

typealias ForeignFunctionValue = Pointer
typealias ForeignFunctionType = ffi_type
