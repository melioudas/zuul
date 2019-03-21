package com.example.demo.filter;

import com.google.common.util.concurrent.RateLimiter;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

/**
 * LimitFilter
 * 限流功能
 */
@RefreshScope
@Component
public class LimitFilter extends ZuulFilter {
    //是否限流

    @Value("${custom.limit.orlimit}")
    private Boolean orLimit;
    //每秒限流数
    @Value("${custom.limit.number}")
    private  double limitNumber;

    //定义令牌桶
    public RateLimiter RATE_LIMITER;


    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        //保证最先执行
        return -4;
    }

    @Override
    public boolean shouldFilter() {
        if(orLimit){
           if(RATE_LIMITER==null){
               RATE_LIMITER=RateLimiter.create(limitNumber);
           }
        }
        return orLimit;
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext requestContext = RequestContext.getCurrentContext();
        //如果没拿到，不做处理
        if (!RATE_LIMITER.tryAcquire()) {
            requestContext.setSendZuulResponse(false);
            //TOO MANY REQUEST
            requestContext.setResponseStatusCode(3429);
            throw new ZuulException("限流异常", 3429, "请求超过最大限流数！");
        }
        return null;
    }
}
