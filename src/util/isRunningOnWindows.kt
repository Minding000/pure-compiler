package util

fun isRunningOnWindows(): Boolean {
	return System.getProperty("os.name").lowercase().contains("windows")
}
