package code

import errors.CompilerError
import instructions.*
import objects.Instruction

object PythonCompiler {

    fun compile(instructions: List<Instruction>): String {
        var code = ""
        for(instruction in instructions) {
            code += when(instruction) {
                is Add -> "${instruction.outputRegister} = ${instruction.leftRegister} ${ if(instruction.isNegative) "-" else "+" } ${instruction.rightRegister}"
                is Copy -> "${instruction.targetRegister} = ${instruction.sourceRegister}"
                //is Eql -> ""
                is Init -> "${instruction.register} = ${ if(instruction.value is String) "\"${instruction.value}\"" else instruction.value }"
                is Mul -> "${instruction.outputRegister} = ${instruction.leftRegister} ${ if(instruction.isDivision) "/" else "*" } ${instruction.rightRegister}"
                is Prt -> "print(${instruction.registers.joinToString(",")})"
                else -> throw CompilerError("Failed to compile instruction to python: $instruction")
            }
            code += "\n"
        }
        return code.substring(0, code.length - 1)
    }
}