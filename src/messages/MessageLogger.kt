package messages

import errors.internal.CompilerError
import java.util.*

class MessageLogger(private val systemName: String, private val verbosity: Message.Type) {
	private val phases = LinkedList<Phase>()
	private var activePhase: Phase? = null

	fun addPhase(name: String) {
		val phase = Phase(name)
		phases.add(phase)
		activePhase = phase
	}

	fun add(message: Message) {
		val phase = activePhase ?: throw CompilerError("Tried to add message while no phase was active.")
		phase.add(message)
	}

	fun printReport() {
		val capitalizedSystemName = systemName.replaceFirstChar { char -> char.uppercase() }
		val totalMessageTypeCounts = Array(Message.Type.values().size) { 0 }
		for(phase in phases) {
			if(phase.messages.isEmpty())
				continue
			if(verbosity <= Message.Type.INFO) {
				println()
				println("----- $capitalizedSystemName phase: ${phase.name} (${phase.getTypeCountString()}) -----")
			}
			for(message in phase.messages) {
				if(message.type >= verbosity)
					println("${message.type.name}: ${message.description}")
			}
			for(messageTypeOrdinal in phase.messageTypeCounts.indices)
				totalMessageTypeCounts[messageTypeOrdinal] += phase.messageTypeCounts[messageTypeOrdinal]
		}
		println("Total messages in $systemName: "
			+ "${totalMessageTypeCounts[Message.Type.ERROR.ordinal]} errors, "
			+ "${totalMessageTypeCounts[Message.Type.WARNING.ordinal]} warnings, "
			+ "${totalMessageTypeCounts[Message.Type.INFO.ordinal]} infos, "
			+ "${totalMessageTypeCounts[Message.Type.DEBUG.ordinal]} debug messages"
			+ " (Verbosity: ${verbosity.name})")
	}

	fun messages(): Iterator<Message> {
		return MessageIterator()
	}

	private class Phase(val name: String) {
		val messages = LinkedList<Message>()
		val messageTypeCounts = Array(Message.Type.values().size) { 0 }

		fun add(message: Message) {
			messages.add(message)
			messageTypeCounts[message.type.ordinal]++
		}

		fun getTypeCountString(): String {
			return Message.Type.values().reversed().joinToString { messageType ->
				"${messageType.name.first()}${messageTypeCounts[messageType.ordinal]}" }
		}
	}

	private inner class MessageIterator: Iterator<Message> {
		var phaseIndex = 0
		var currentIterator: Iterator<Message>? = phases.getOrNull(phaseIndex)?.messages?.iterator()

		override fun hasNext(): Boolean {
			while(true) {
				val hasNext = currentIterator?.hasNext() ?: return false
				if(hasNext)
					return true
				phaseIndex++
				currentIterator = phases.getOrNull(phaseIndex)?.messages?.iterator()
			}
		}

		override fun next(): Message {
			return currentIterator!!.next()
		}
	}
}
