package com.discovery.workload.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TransactionIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Transaction-Id";
    public static final String MDC_KEY = "transactionId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String txId = request.getHeader(HEADER);

            if (txId == null || txId.isBlank()) {
                txId = UUID.randomUUID().toString();
            }

            MDC.put(MDC_KEY, txId);
            response.setHeader(HEADER, txId);

            filterChain.doFilter(request, response);

        } finally {
            MDC.clear();
        }
    }
}