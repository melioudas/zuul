package com.example.demo.filter;

import com.alibaba.fastjson.JSONObject;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

/**
 * AccessFilter
 * 校验token的过滤器
 * 在主类增加了该filter的引入
 */
@RefreshScope
@Component
public class AccessTokenFilter extends ZuulFilter {
    private static final Logger log = LoggerFactory.getLogger(AccessTokenFilter.class);
    private static final String AUTH_SUCCESS = "0";

    @Autowired
    RestTemplate restTemplate;
    //校验服务器信息地址
    @Value("${custom.auth.token.url}")
    String authTokenUrl;

    //不走该过滤器的请求,如登录请求
    @Value("${custom.not.check.uri}")
    String notCheckUri;

    //设置是什么类型的Filter
    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    //设置执行顺序
    @Override
    public int filterOrder() {
        return 0;
    }

    /**
     * 根据请求的uri判断是否为登录请求，若为登录请求则不执行该过滤器
     */
    @Override
    public boolean shouldFilter() {
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest request = requestContext.getRequest();
        // 用户登录||游客登录
        if (!notCheckUri.isEmpty()) {
            String[] uris = notCheckUri.split(",");
            for (String uri : uris) {
                if (uri.equalsIgnoreCase(request.getRequestURI())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        String authorization = request.getHeader("Authorization");

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Authorization", authorization);

        RestTemplate template = new RestTemplate();
        HttpEntity<String> requestEntity = new HttpEntity<String>(null, requestHeaders);
        String authTokenReturn="";
        try{
            ResponseEntity<String> response = template.exchange(authTokenUrl, HttpMethod.GET, requestEntity, String.class);
            authTokenReturn = response.getBody();
        }catch (Exception e){
            throw new ZuulException("校验服务器连接失败", new Integer(3500), "校验服务器连接失败");
        }

        JSONObject jsonObject = JSONObject.parseObject(authTokenReturn);

        String code = jsonObject.getString("code");
        String message = jsonObject.getString("message");
        //校验成功
        if (AUTH_SUCCESS.equals(code)) {
            String result = jsonObject.getString("result");
            //注：这里值为userinfo:{"market":"US","userStatus":"c8","clientType":"Web","userType":"VIP","userName":"Leo"}
            ctx.getZuulRequestHeaders().put("Userinfo", result);
        } else {
            //校验失败，
            ctx.setSendZuulResponse(false);
            ctx.setResponseStatusCode(new Integer(code));
            throw new ZuulException("token校验失败", new Integer(code), message);
        }

        return null;
    }
}
