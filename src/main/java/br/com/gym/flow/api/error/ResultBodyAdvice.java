package br.com.gym.flow.api.error;

import br.com.gym.flow.shared.domain.Result;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Unwraps {@link Result} returned by REST endpoints before serialization:
 * a {@link Result.Success} writes only its inner value; a {@link Result.Failure}
 * is translated into a {@link BusinessException} that {@link GlobalExceptionHandler}
 * routes to the right HTTP status based on the first error's {@code ErrorCategory}.
 *
 * <p>Endpoints can return {@code Result<T>} directly and stay free of any
 * {@code .orElseThrow(BusinessException::new)} boilerplate.
 */
@RestControllerAdvice
public class ResultBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return Result.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body instanceof Result.Failure<?> failure) {
            throw new BusinessException(failure.notification());
        }
        if (body instanceof Result.Success<?> success) {
            return success.value();
        }
        return body;
    }
}
