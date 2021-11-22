package instructions

import objects.Instruction
import objects.Register
import objects.ValueSource
import java.util.*

class Prt(var valueSources: MutableList<ValueSource>): Instruction() {

	override fun getReadRegisters(): List<Register> {
		val registers = LinkedList<Register>()
		for(valueSource in valueSources)
			if(valueSource is Register)
				registers.add(valueSource)
		return registers
	}

	override fun replace(current: Register, new: ValueSource) {
		val iterator = valueSources.listIterator()
		while(iterator.hasNext())
			if(iterator.next() == current)
				iterator.set(new)
	}
}