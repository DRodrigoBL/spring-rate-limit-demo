package com.app.config.redis

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import redis.clients.jedis.JedisPoolConfig
import java.time.Duration

@Configuration
class RedisConfiguration {

    @Bean
    fun jedisPoolConfig() =
        JedisPoolConfig().apply {
            maxTotal = 64
            maxIdle = 32
            testOnBorrow = true
            testOnReturn = true
            testWhileIdle = true
            minEvictableIdleTimeMillis = Duration.ofSeconds(60).toMillis()
            timeBetweenEvictionRunsMillis = Duration.ofSeconds(30).toMillis()
            numTestsPerEvictionRun = 3
            blockWhenExhausted = true
        }

    @Bean
    fun jedisClientConfiguration(jedisPoolConfig: JedisPoolConfig) =
        JedisClientConfiguration
            .builder()
            .apply {
                connectTimeout(Duration.ofSeconds(1))
                readTimeout(Duration.ofMillis(100))
            }
            .usePooling()
            .poolConfig(jedisPoolConfig)
            .build()

    @Bean
    @Primary
    fun redisStandaloneConfiguration(@Value("\${spring.redis.host}") hostName: String,
                                     @Value("\${spring.redis.port}") portNumber: Int) =
        RedisStandaloneConfiguration().apply {
            setHostName(hostName)
            port = portNumber
        }

    @Bean
    @Primary
    fun redisConnectionFactory(jedisConfig: RedisStandaloneConfiguration, jedisClientConfiguration: JedisClientConfiguration) =
        JedisConnectionFactory(jedisConfig, jedisClientConfiguration)

    @Bean
    @Primary
    fun redisTemplate(jedisConnectionFactory: JedisConnectionFactory) =
        RedisTemplate<String, Any>().apply {
            setConnectionFactory(jedisConnectionFactory)
            setDefaultSerializer(StringRedisSerializer())
            afterPropertiesSet()
        }

    @Bean("rateLimitRedisTemplate")
    fun rateLimitRedisTemplate(jedisConnectionFactory: JedisConnectionFactory) =
        RedisTemplate<String, Int>().apply {
            setConnectionFactory(jedisConnectionFactory)
            keySerializer = RedisSerializer.string()
            valueSerializer = RedisIntegerSerializer()
            afterPropertiesSet()
        }

}

class RedisIntegerSerializer: RedisSerializer<Int> {
    override fun serialize(t: Int?): ByteArray?  = t?.toString()?.toByteArray()
    override fun deserialize(bytes: ByteArray?): Int? = bytes?.let { String(it).toIntOrNull() }
}