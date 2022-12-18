package util

import java.math.BigDecimal

fun BigDecimal.isInteger(): Boolean {
	return stripTrailingZeros().scale() <= 0
}

fun BigDecimal.isRepresentedAsAnInteger(): Boolean {
	return scale() <= 0
}
