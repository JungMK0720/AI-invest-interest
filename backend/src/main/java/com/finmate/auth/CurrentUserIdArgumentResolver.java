package com.finmate.auth;

import com.finmate.common.ApiException;
import com.finmate.common.CurrentUserId;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthTokenStore tokenStore;

    public CurrentUserIdArgumentResolver(AuthTokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && String.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String header = webRequest.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw ApiException.unauthorized("로그인이 필요합니다.");
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return tokenStore.resolveUserId(token)
                .orElseThrow(() -> ApiException.unauthorized("로그인이 필요합니다."));
    }
}
