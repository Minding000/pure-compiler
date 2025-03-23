package logger

import errors.internal.CompilerError
import util.uppercaseFirstChar
import java.util.*

class Logger(private val systemName: String) {
	private val phases = LinkedList<Phase>()
	private var activePhase: Phase? = null

	fun addPhase(name: String) {
		val phase = Phase(name)
		phases.add(phase)
		activePhase = phase
	}

	fun add(issue: Issue) {
		val phase = activePhase ?: throw CompilerError("Tried to add issue while no phase was active.")
		phase.add(issue)
	}

	fun printReport(verbosity: Severity, ignoreInternalIssues: Boolean = true) {
		val capitalizedSystemName = systemName.uppercaseFirstChar()
		val totalIssueTypeCounts = Array(Severity.entries.size) { 0 }
		for(phase in phases) {
			if(!phase.containsVisibleIssues(verbosity, ignoreInternalIssues))
				continue
			if(verbosity <= Severity.INFO) {
				println()
				println("----- $capitalizedSystemName phase: ${phase.name} (${phase.getTypeCountString(ignoreInternalIssues)}) -----")
			}
			for(issue in phase.issues) {
				if(issue.severity >= verbosity && !(ignoreInternalIssues && issue.isInternal))
					println("${issue.severity.name}: $issue")
			}
			for(issueTypeOrdinal in phase.issueTypeCounts.indices) {
				totalIssueTypeCounts[issueTypeOrdinal] += phase.issueTypeCounts[issueTypeOrdinal]
				if(!ignoreInternalIssues)
					totalIssueTypeCounts[issueTypeOrdinal] += phase.internalIssueTypeCounts[issueTypeOrdinal]
			}
		}
		println("Total issues in $systemName: "
			+ "${totalIssueTypeCounts[Severity.ERROR.ordinal]} errors, "
			+ "${totalIssueTypeCounts[Severity.WARNING.ordinal]} warnings, "
			+ "${totalIssueTypeCounts[Severity.INFO.ordinal]} infos, "
			+ "${totalIssueTypeCounts[Severity.DEBUG.ordinal]} debug issues"
			+ " (Verbosity: ${verbosity.name})")
	}

	fun issues(): Iterator<Issue> {
		return IssueIterator()
	}

	fun containsErrors(): Boolean {
		for(issue in issues()) {
			if(issue.severity == Severity.ERROR)
				return true
		}
		return false
	}

	private inner class Phase(val name: String) {
		val issues = LinkedList<Issue>()
		val issueTypeCounts = Array(Severity.entries.size) { 0 }
		val internalIssueTypeCounts = Array(Severity.entries.size) { 0 }

		fun add(issue: Issue) {
			issues.add(issue)
			if(issue.isInternal)
				internalIssueTypeCounts[issue.severity.ordinal]++
			else
				issueTypeCounts[issue.severity.ordinal]++
		}

		fun containsVisibleIssues(verbosity: Severity, ignoreInternalIssues: Boolean): Boolean {
			for(issueType in Severity.entries) {
				if(issueType >= verbosity) {
					if(issueTypeCounts[issueType.ordinal] > 0)
						return true
					if(!ignoreInternalIssues && internalIssueTypeCounts[issueType.ordinal] > 0)
						return true
				}
			}
			return false
		}

		fun getTypeCountString(ignoreInternalIssues: Boolean): String {
			return Severity.entries.reversed().joinToString { issueType ->
				"${issueType.name.first()}${issueTypeCounts[issueType.ordinal] + (if(ignoreInternalIssues) 0 else internalIssueTypeCounts[issueType.ordinal])}"
			}
		}
	}

	private inner class IssueIterator: Iterator<Issue> {
		var phaseIndex = 0
		var currentIterator: Iterator<Issue>? = phases.getOrNull(phaseIndex)?.issues?.iterator()

		override fun hasNext(): Boolean {
			while(true) {
				val hasNext = currentIterator?.hasNext() ?: return false
				if(hasNext)
					return true
				phaseIndex++
				currentIterator = phases.getOrNull(phaseIndex)?.issues?.iterator()
			}
		}

		override fun next(): Issue {
			return currentIterator!!.next()
		}
	}
}
