package com.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RateLimitApplication

fun main(args: Array<String>) {
	runApplication<RateLimitApplication>(*args)
}
