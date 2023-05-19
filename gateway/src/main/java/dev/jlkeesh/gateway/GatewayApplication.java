package dev.jlkeesh.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@SpringBootApplication
@EnableDiscoveryClient
@RestController
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }


    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("postService", p -> p
                        .path("/api/posts/**")
                        .uri("lb://post-service"))
                .route("commentService", p -> p
                        .path("/api/comments/**")
                        .filters(f -> f.circuitBreaker(cb -> cb.setFallbackUri("/comment/fallback")))
                        .uri("lb://comment-service"))
                .build();

        // edge security
        // service to service KEY-CLOCK
    }

    @GetMapping("/comment/fallback")
    public Mono<String> commentFallback() {
        return Mono.just("comment service down");
    }


}
