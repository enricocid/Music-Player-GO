package com.iven.musicplayergo.ui

import kotlin.math.abs

object SingleClickHelper {

	private const val MIN_CLICK_INTERVAL = 500
	private var sLastClickTime: Long = 0

	fun isBlockingClick(): Boolean = isBlockingClick(MIN_CLICK_INTERVAL.toLong())

    @Suppress("SameParameterValue")
	private fun isBlockingClick(minClickInterval: Long): Boolean {
		val isBlocking: Boolean
		val currentTime = System.currentTimeMillis()
		isBlocking = abs(currentTime - sLastClickTime) < minClickInterval
		if (!isBlocking) {
			sLastClickTime = currentTime
		}
		return isBlocking
	}
}
