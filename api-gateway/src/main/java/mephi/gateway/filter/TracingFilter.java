package mephi.gateway.filter;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class TracingFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(TracingFilter.class);

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";

    @Override
    public @NonNull Mono<Void> filter(ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        String requestId = UUID.randomUUID().toString();

        final String finalCorrelationId = correlationId;

        log.info(
            "Incoming request: method={}, path={}, correlationId={}, requestId={}",
            request.getMethod(),
            request.getPath(),
            correlationId,
            requestId
        );

        ServerHttpRequest modifiedRequest = request.mutate()
            .header(CORRELATION_ID_HEADER, correlationId)
            .header(REQUEST_ID_HEADER, requestId)
            .build();

        ServerWebExchange modifiedExchange = exchange.mutate()
            .request(modifiedRequest)
            .build();

        modifiedExchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);
        modifiedExchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);

        long startTime = System.currentTimeMillis();

        return chain.filter(modifiedExchange)
            .doOnSuccess(aVoid -> {
                long duration = System.currentTimeMillis() - startTime;

                log.info(
                    "Request completed: correlationId={}, requestId={}, status={}, duration={}ms",
                    finalCorrelationId,
                    requestId,
                    modifiedExchange.getResponse().getStatusCode(),
                    duration
                );
            })
            .doOnError(throwable -> {
                long duration = System.currentTimeMillis() - startTime;

                log.error(
                    "Request failed: correlationId={}, requestId={}, duration={}ms, error={}",
                    finalCorrelationId,
                    requestId,
                    duration,
                    throwable.getMessage()
                );
            });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
