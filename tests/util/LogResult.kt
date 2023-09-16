package util

import logger.Issue
import logger.Logger
import logger.Severity

open class LogResult(val logger: Logger) {

	inline fun <reified I: Issue>assertIssueDetected(expectedText: String? = null, expectedSeverity: Severity? = null,
													 expectedLineNumber: Int? = null) {
		var detectedLineNumber: Int? = null
		var detectedText: String? = null
		var matchCount = 0
		for(issue in logger.issues()) {
			if(issue.isInternal)
				continue
			if(issue is I) {
				val line = issue.source?.start?.line
				if(line?.file?.name != TestUtil.TEST_FILE_NAME)
					continue
				if(expectedLineNumber != null && line.number != expectedLineNumber) {
					detectedLineNumber = line.number
					continue
				}
				if(expectedSeverity != null && issue.severity != expectedSeverity)
					throw AssertionError("Issue '${I::class.simpleName}' has severity '${issue.severity}'" +
						" instead of expected severity '$expectedSeverity'.")
				if(expectedText != null && issue.text != expectedText) {
					detectedText = issue.text
					continue
				}
				matchCount++
			}
		}
		when(matchCount) {
			0 -> {
				if(detectedLineNumber != null)
					throw AssertionError("Issue '${I::class.simpleName}' is in line '${detectedLineNumber}'" +
						" instead of expected line number '$expectedLineNumber'.")
				if(detectedText != null) {
					if(expectedText != null)
						TestUtil.printDiffPosition(expectedText, detectedText)
					throw AssertionError("Issue '${I::class.simpleName}' has text '${detectedText}'" +
						" instead of expected text '$expectedText'.")
				}
				throw AssertionError("Expected issue '${I::class.simpleName}' hasn't been detected.")
			}
			1 -> return
			else -> {
				throw AssertionError("Expected issue '${I::class.simpleName}' has been detected '$matchCount' times.")
			}
		}
	}

	inline fun <reified I: Issue>assertIssueNotDetected(fileName: String = TestUtil.TEST_FILE_NAME) {
		for(issue in logger.issues()) {
			if(issue.isInternal)
				continue
			if(issue is I) {
				if(issue.source?.start?.line?.file?.name != fileName)
					continue
				throw AssertionError("Issue '${I::class.simpleName}' has unexpectedly been detected.")
			}
		}
	}
}
