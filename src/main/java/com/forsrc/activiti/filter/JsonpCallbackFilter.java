package com.forsrc.activiti.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonpCallbackFilter implements Filter {

    private static Logger log = LoggerFactory.getLogger(JsonpCallbackFilter.class);

    public void init(FilterConfig fConfig) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        Map<String, String[]> parms = httpRequest.getParameterMap();

        if (!parms.containsKey("callback")) {
            chain.doFilter(request, response);
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Wrapping response with JSONP callback '" + parms.get("callback")[0] + "'");
        }

        GenericResponseWrapper wrapper = new GenericResponseWrapper(httpResponse);

        chain.doFilter(request, wrapper);

        // handles the content-size truncation
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(new String(parms.get("callback")[0] + "(").getBytes());
        outputStream.write(wrapper.getData());
        outputStream.write(new String(");").getBytes());
        byte jsonpResponse[] = outputStream.toByteArray();

        wrapper.setContentType("text/javascript;charset=UTF-8");
        wrapper.setContentLength(jsonpResponse.length);

        try (OutputStream out = httpResponse.getOutputStream()) {
            out.write(jsonpResponse);
        }

    }

    public void destroy() {
    }
}
