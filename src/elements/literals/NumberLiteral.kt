package elements.literals

import code.InstructionGenerator
import elements.ValueElement
import types.NativeTypes
import value_analysis.*
import word_generation.Word

class NumberLiteral(val word: Word): ValueElement(word.start, word.end, NativeTypes.INT) {
    val raw: Int = Integer.parseInt(getValue())

    override fun generateInstructions(generator: InstructionGenerator): StaticValue {
        return StaticValue(raw)
    }

    override fun toString(): String {
        return "NumberLiteral { $raw }"
    }
}