package org.rxjava.gateway.client;

import org.apache.commons.lang3.StringUtils;
import org.rxjava.api.user.serve.ServeUserApi;
import org.rxjava.common.core.utils.JsonUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author happy 2019-05-12 22:07
 */
public class TokenFilter implements GlobalFilter, Ordered {
    private static final String AUTHORIZATION = "authorization";
    private ServeUserApi serveUserApi;

    public TokenFilter(ServeUserApi serveUserApi) {
        this.serveUserApi = serveUserApi;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String token = request.getHeaders().getFirst(AUTHORIZATION);

        if (StringUtils.isNotEmpty(token)) {

            return serveUserApi.tokenToLoginInfo(token)
                    .map(loginInfo -> {
                        String loginInfoJson;
                        try {
                            loginInfoJson = URLEncoder.encode(JsonUtils.serialize(loginInfo), "utf8");
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }
                        return loginInfoJson;
                    })
                    .flatMap(loginInfoJson -> {
                                ServerHttpRequest host = request
                                        .mutate()
                                        .header("loginInfo", loginInfoJson)
                                        .build();
                                ServerWebExchange build = exchange
                                        .mutate()
                                        .request(host)
                                        .build();
                                return chain.filter(build);
                            }
                    )
                    .onErrorResume(WebClientResponseException.class, e -> chain.filter(exchange));
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
