package br.com.gym.flow.shared.observability;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Copies the calling thread's MDC into the executor thread used by
 * {@code @ApplicationModuleListener} and {@code @Async}, so log lines
 * emitted by async event handlers carry the originating requestId.
 */
@Configuration
public class MdcAsyncConfig {

    @Bean
    Executor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("cardapio-async-");
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.initialize();
        return executor;
    }

    private TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            Map<String, String> ctx = MDC.getCopyOfContextMap();
            return MdcContext.runWith(ctx, runnable);
        };
    }
}
