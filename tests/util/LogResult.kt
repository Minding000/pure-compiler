package util

import logger.Issue
import logger.Logger
import logger.Severity

open class LogResult(val logger: Logger) {

	inline fun <reified I: Issue>assertIssueDetected(expectedText: String? = null, expectedSeverity: Severity? = null) {
		var detectedText: String? = null
		for(issue in logger.issues()) {
			if(issue.isInternal)
				continue
			if(issue is I) {
				if(issue.source?.start?.line?.file?.name != TestUtil.TEST_FILE_NAME)
					continue
				if(expectedSeverity != null && issue.severity != expectedSeverity)
					throw AssertionError("Issue '${I::class.simpleName}' has severity '${issue.severity}'" +
						" instead of expected severity '$expectedSeverity'.")
				if(expectedText != null && issue.text != expectedText) {
					detectedText = issue.text
					continue
				}
				return
			}
		}
		if(detectedText != null)
			throw AssertionError("Issue '${I::class.simpleName}' has text '${detectedText}'" +
				" instead of expected text '$expectedText'.")
		throw AssertionError("Expected issue '${I::class.simpleName}' hasn't been detected.")
	}

	inline fun <reified I: Issue>assertIssueNotDetected() {
		for(issue in logger.issues()) {
			if(issue.isInternal)
				continue
			if(issue is I) {
				if(issue.source?.start?.line?.file?.name != TestUtil.TEST_FILE_NAME)
					continue
				throw AssertionError("Issue '${I::class.simpleName}' has unexpectedly been detected.")
			}
		}
	}
}
