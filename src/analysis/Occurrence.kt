package analysis

import objects.Instruction
import objects.Register

class Occurrence(var instruction: Instruction, val register: Register, val type: OccurrenceType)