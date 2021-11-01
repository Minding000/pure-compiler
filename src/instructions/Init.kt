package instructions

import objects.Instruction
import objects.Register

class Init(val register: Register, val value: Any): Instruction()