package instructions

import objects.Instruction
import objects.Register

class Copy(val targetRegister: Register, val sourceRegister: Register): Instruction()