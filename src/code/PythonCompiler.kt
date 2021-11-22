package code

import errors.CompilerError
import instructions.*
import objects.Instruction

object PythonCompiler {

    fun compile(instructions: List<Instruction>): String {
        var code = ""
        for(instruction in instructions) {
            code += when(instruction) {
                is Add -> "${instruction.outputRegister} = ${instruction.leftValueSource} ${ if(instruction.isNegative) "-" else "+" } ${instruction.rightValueSource}"
                is Copy -> "${instruction.targetRegister} = ${instruction.sourceRegister}"
                //is Eql -> ""
                is Init -> "${instruction.targetRegister} = ${instruction.value}"
                is Mul -> "${instruction.outputRegister} = ${instruction.leftValueSource} ${ if(instruction.isDivision) "/" else "*" } ${instruction.rightValueSource}"
                is Prt -> "print(${instruction.valueSources.joinToString(",")})"
                else -> throw CompilerError("Failed to compile instruction to python: $instruction")
            }
            if(Main.DEBUG)
                code += " #${instruction::class.simpleName}"
            code += "\n"
        }
        return code.substring(0, code.length - 1)
    }
}