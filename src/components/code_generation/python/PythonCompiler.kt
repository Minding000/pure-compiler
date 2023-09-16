package components.code_generation.python

import components.code_generation.python.instructions.*
import errors.internal.CompilerError

object PythonCompiler {

    fun compile(instructions: List<Instruction>, shouldTagInstructions: Boolean = false): String {
        if(instructions.isEmpty())
            return ""
        var code = ""
        for(instruction in instructions) {
            code += when(instruction) {
                is Add -> "${instruction.output} = ${instruction.leftValueSource} ${ if(instruction.isNegative) "-" else "+" } ${instruction.rightValueSource}"
                is Copy -> "${instruction.targetDynamicValue} = ${instruction.valueSource}"
                //is Eql -> ""
                is Load -> "${instruction.targetDynamicValue} = ${instruction.heapValue}"
                is Mul -> "${instruction.output} = ${instruction.leftValueSource} ${ if(instruction.isDivision) "/" else "*" } ${instruction.rightValueSource}"
                is Exp -> "${instruction.output} = ${instruction.leftValueSource} ** ${instruction.rightValueSource}"
                is Prt -> "print(${instruction.valueSources.joinToString(",")})"
                else -> throw CompilerError("Failed to compile instruction to python: $instruction")
            }
            if(shouldTagInstructions)
                code += " #${instruction::class.simpleName}"
            code += "\n"
        }
        return code.substring(0, code.length - 1)
    }
}
