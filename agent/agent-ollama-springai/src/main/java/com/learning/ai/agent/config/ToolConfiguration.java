package com.learning.ai.agent.config;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ToolConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ToolConfiguration.class);

    public record DateTimeRequest(String format) {}

    @Bean
    public ToolCallback currentDateTimeTool(MeterRegistry meterRegistry) {
        return FunctionToolCallback.builder("currentDateTime", (Function<DateTimeRequest, String>) request -> {
                    long start = System.nanoTime();
                    try {
                        String pattern =
                                request.format() != null && !request.format().isBlank()
                                        ? request.format()
                                        : "yyyy-MM-dd HH:mm:ss";
                        String result = LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern));
                        meterRegistry
                                .timer("agent.tool.latency", "tool", "currentDateTime")
                                .record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
                        return result;
                    } catch (Exception e) {
                        meterRegistry
                                .counter("agent.tool.errors", "tool", "currentDateTime")
                                .increment();
                        log.error("Error getting current date time", e);
                        return "Error formatting date time: " + e.getMessage();
                    }
                })
                .description("Get the current date and time. Optionally provide a format string.")
                .inputType(DateTimeRequest.class)
                .build();
    }

    public record CalculatorRequest(double a, double b, String operation) {}

    @Bean
    public ToolCallback calculatorTool(MeterRegistry meterRegistry) {
        return FunctionToolCallback.builder("calculator", (Function<CalculatorRequest, String>) request -> {
                    long start = System.nanoTime();
                    try {
                        if (request.operation() == null || request.operation().isBlank()) {
                            throw new IllegalArgumentException("Operation must not be null or empty.");
                        }
                        // Guard against unsafe operation patterns (e.g., trying to inject SpEL or scripts)
                        if (request.operation().matches(".*[().;'{}\\[\\]].*")) {
                            throw new IllegalArgumentException("Unsafe or invalid operation detected.");
                        }
                        String result =
                                switch (request.operation().toLowerCase()) {
                                    case "add", "+" -> String.valueOf(request.a() + request.b());
                                    case "subtract", "-" -> String.valueOf(request.a() - request.b());
                                    case "multiply", "*" -> String.valueOf(request.a() * request.b());
                                    case "divide", "/" -> {
                                        if (request.b() == 0) yield "Error: Division by zero";
                                        yield String.valueOf(request.a() / request.b());
                                    }
                                    default -> "Error: Unknown operation " + request.operation();
                                };
                        meterRegistry
                                .timer("agent.tool.latency", "tool", "calculator")
                                .record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
                        return result;
                    } catch (Exception e) {
                        meterRegistry
                                .counter("agent.tool.errors", "tool", "calculator")
                                .increment();
                        log.error("Error in calculator tool", e);
                        return "Error evaluating calculation: " + e.getMessage();
                    }
                })
                .description("Perform a basic mathematical calculation (add, subtract, multiply, divide).")
                .inputType(CalculatorRequest.class)
                .build();
    }

    public record WeatherRequest(String city) {}

    @Bean
    public ToolCallback weatherLookupTool(MeterRegistry meterRegistry) {
        return FunctionToolCallback.builder("weatherLookup", (Function<WeatherRequest, String>) request -> {
                    long start = System.nanoTime();
                    try {
                        if (request.city() == null || request.city().isBlank()) {
                            return "Error: City must be provided.";
                        }
                        String result;
                        // Mock domain logic
                        if (request.city().equalsIgnoreCase("london")) {
                            result = "The weather in London is mostly cloudy with a chance of rain, 15°C.";
                        } else if (request.city().equalsIgnoreCase("tokyo")) {
                            result = "The weather in Tokyo is sunny, 22°C.";
                        } else {
                            result = "Weather information for " + request.city() + " is currently unavailable.";
                        }
                        meterRegistry
                                .timer("agent.tool.latency", "tool", "weatherLookup")
                                .record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
                        return result;
                    } catch (Exception e) {
                        meterRegistry
                                .counter("agent.tool.errors", "tool", "weatherLookup")
                                .increment();
                        log.error("Error looking up weather", e);
                        return "Error looking up weather: " + e.getMessage();
                    }
                })
                .description("Look up the current weather for a given city.")
                .inputType(WeatherRequest.class)
                .build();
    }
}
