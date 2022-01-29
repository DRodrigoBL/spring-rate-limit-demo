package com.app.controller

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@RestController
class DemoController(@Qualifier("rateLimitRedisTemplate") private val rt: RedisTemplate<String, Int>) {

    private val logger by lazy { LoggerFactory.getLogger(DemoController::class.java.simpleName) }

    @GetMapping("/products")
    fun retrieveProducts(@RequestHeader(name = "api-key", required = true) apiKey: String): ResponseEntity<List<Product>> {

        val currentValue = tryGetKey(apiKey)

        if (currentValue == null) {
            trySet(apiKey)
        } else if (currentValue < 2) {
            tryIncrement(apiKey)
        } else {
            return ResponseEntity.status(429).build()
        }
        return ResponseEntity.ok(null)
    }

    fun trySet(key: String) {
        runCatching {
            rt.opsForValue().set(key, 1, Duration.ofMillis(1000))
        }.onFailure {
            logger.error("error incrementing redis key $key. ${it.javaClass.simpleName}: ${it.message}")
        }
    }

    fun tryIncrement(key: String) {
        runCatching {
            rt.opsForValue().increment(key)
        }.onFailure {
            logger.error("error incrementing redis key $key. ${it.javaClass.simpleName}: ${it.message}")
        }
    }

    fun tryGetKey(key: String): Int? =
        runCatching { rt.opsForValue().get(key) }
            .onFailure { logger.error("error getting redis key $key. ${it.javaClass.simpleName}: ${it.message}") }
            .getOrNull()

}

data class Product(val name: String, val price: Double)