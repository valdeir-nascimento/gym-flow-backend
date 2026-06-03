package br.com.gym.flow.shared.observability;

import org.slf4j.MDC;

import java.util.Map;

/**
 * Constants and helpers for the MDC keys propagated through the request
 * lifecycle and into async listeners.
 */
public final class MdcContext {

    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_CUSTOMER_ID = "customerId";
    public static final String MDC_CORRELATION_ID = "correlationId";

    private MdcContext() {}

    public static Runnable runWith(Map<String, String> ctx, Runnable inner) {
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            try {
                if (ctx != null) ctx.forEach((k, v) -> { if (v != null) MDC.put(k, v); });
                inner.run();
            } finally {
                MDC.clear();
                if (previous != null) MDC.setContextMap(previous);
            }
        };
    }
}
