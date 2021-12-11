package elements.literals

import code.InstructionGenerator
import elements.ValueElement
import instructions.Load
import types.NativeTypes
import value_analysis.*
import word_generation.Word

class StringLiteral(word: Word): ValueElement(word.start, word.end, NativeTypes.STRING) {
    val raw: String

    init {
        val value = word.getValue()
        raw = value.substring(1, value.length - 1)
    }

    override fun generateInstructions(generator: InstructionGenerator): DynamicValue {
        val dynamicValue = DynamicValue()
        generator.instructions.add(Load(dynamicValue, HeapValue(raw)))
        return dynamicValue
    }

    override fun toString(): String {
        return "StringLiteral { \"$raw\" }"
    }
}