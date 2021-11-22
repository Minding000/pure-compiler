package analysis

import objects.Instruction
import objects.Register
import java.util.*

class ValueScope(var register: Register, var writeInstruction: Instruction) {
	val occurrences = LinkedList<Occurrence>()
}