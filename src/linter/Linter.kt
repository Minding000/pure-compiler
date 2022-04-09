package linter

import linter.elements.general.Program
import linter.messages.Message
import java.util.*

class Linter {
	val messages = LinkedList<Message>()

	fun lint(ast: parsing.ast.general.Program): Program {
		val program = ast.concretize(this)
		program.resolveFileReferences(this)
		program.linkTypes(this)
		program.linkReferences(this)
		program.validate(this)
		return program
	}
/*
	private fun constructTypeDefinition(typeDefinition: TypeDefinition): Unit {
		when(val typeType = typeDefinition.type.getValue()) {
			"class" -> return constructClass(typeDefinition)
			"object" -> return constructObject(typeDefinition)
			"enum" -> return constructEnum(typeDefinition)
			"trait" -> return constructTrait(typeDefinition)
			else -> {
				messages.add(Message("Unknown type type '$typeType'."))
			}
		}
	}

	private fun constructClass(typeDefinition: TypeDefinition): Class {
		val initializers = LinkedList<Initializer>()
		val deinitializers = LinkedList<Initializer>()
		val properties = LinkedList<Initializer>()
		val constants = LinkedList<Initializer>()
		val computedProperties = LinkedList<Initializer>()
		val instances = LinkedList<Initializer>()
		val functions = LinkedList<Initializer>()
		//typeDefinition.
		val name = typeDefinition.identifier.getValue()
		for(member in typeDefinition.body.members) {
			when(member) {
				is InitializerDefinition -> initializers.add(constructInitializer(typeDefinition, member))
			}
		}
		return Class(name, initializers)
	}

	private fun constructInitializer(typeDefinition: TypeDefinition, initializerDefinition: InitializerDefinition): Initializer {
		val mainStatements = LinkedList<Statement>()
		val rawMainStatements = initializerDefinition.body?.mainBlock?.statements
		if(rawMainStatements != null) {
			for(statement in rawMainStatements) {
				mainStatements.add(constructStatement(statement))
			}
		}
		val handleStatements = LinkedList<Statement>()
		val handleBlocks = initializerDefinition.body?.handleBlocks
		if(handleBlocks != null) {
			for(handleBlock in handleBlocks) {
				for(statement in handleBlock.block.statements) {
					handleStatements.add(constructStatement(statement))
				}
			}
		}
		val alwaysStatements = LinkedList<Statement>()
		val rawAlwaysStatements = initializerDefinition.body?.alwaysBlock?.block?.statements
		if(rawAlwaysStatements != null) {
			for(statement in rawAlwaysStatements) {
				alwaysStatements.add(constructStatement(statement))
			}
		}
		return Initializer()
	}*/
}