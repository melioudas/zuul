package com.example.demo.controller;

import com.google.common.base.Strings;
import com.netflix.zuul.exception.ZuulException;
import org.springframework.boot.autoconfigure.web.BasicErrorController;
import org.springframework.boot.autoconfigure.web.DefaultErrorAttributes;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * ErrorController
 * 处理Filter中抛出的异常
 */

@RestController
public class ErrorController extends BasicErrorController {

    public ErrorController() {
        super(new DefaultErrorAttributes(), new ErrorProperties());
    }

    @RequestMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
        Map<String, Object> body = getErrorAttributes(request, isIncludeStackTrace(request, MediaType.ALL));
        HttpStatus status = getStatus(request);
        //自定义的错误信息类
        //status.value():错误代码，
        //body.get("message").toString()错误信息
        Map<String, Object> ret = new HashMap();
        String code = "";
        if (body.get("status") != null) {
            code = body.get("status").toString();
        }
        ret.put("code", "".equals(code) ? status : code);
        ret.put("message", body.get("message").toString());

        //Filter抛出的自定义错误类
        if (!Strings.isNullOrEmpty((String) body.get("exception")) && body.get("exception").equals(ZuulException.class.getName())) {
            body.put("status", HttpStatus.FORBIDDEN.value());
            status = HttpStatus.FORBIDDEN;
        }

        return new ResponseEntity<Map<String, Object>>(ret, status);
    }

}

