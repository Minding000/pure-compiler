package util

fun <T> Iterator<T>.jumpTo(element: T) {
	while(hasNext()) {
		if(next() == element)
			break
	}
}