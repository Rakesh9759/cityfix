package com.cityfix.api.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
public class StartupPing {
    @Bean
    CommandLineRunner pingRedisAndRabbit(RedisConnectionFactory redis, ConnectionFactory rabbit) {
        return args -> {
            try (var conn = redis.getConnection()) {
                conn.ping();
                System.out.println("Redis ping OK");
            } catch (Exception e) {
                System.err.println("Redis ping FAILED: " + e.getMessage());
            }
            try (var c = rabbit.createConnection()) {
                c.close();
                System.out.println("RabbitMQ connection OK");
            } catch (Exception e) {
                System.err.println("RabbitMQ connection FAILED: " + e.getMessage());
            }
        };
    }
}
