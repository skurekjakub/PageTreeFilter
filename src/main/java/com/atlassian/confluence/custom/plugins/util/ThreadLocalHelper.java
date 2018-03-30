package com.atlassian.confluence.custom.plugins.util;

import com.atlassian.confluence.cache.ThreadLocalCache;
import com.atlassian.core.filters.ServletContextThreadLocal;
import com.opensymphony.webwork.ServletActionContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/* Isolates individual requests, prevents concurrency issues. */
public class ThreadLocalHelper
{
    private static final String THREAD_LOCAL_CACHE_MARKER_KEY = ThreadLocalHelper.class.getName();
    private static final String THREAD_LOCAL_CACHE_MARKER_VALUE = "Some marker";
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private HttpServletRequest originalRequestInActionContext;
    private HttpServletResponse originalResponseInActionContext;
    private HttpServletRequest originalRequestInServletContext;

    public ThreadLocalHelper(HttpServletRequest request, HttpServletResponse response)
    {
        this.request = request;
        this.response = response;
    }

    public void prepareTreadLocals()
    {
        originalRequestInActionContext = ServletActionContext.getRequest();
        originalResponseInActionContext = ServletActionContext.getResponse();

        originalRequestInServletContext = ServletContextThreadLocal.getRequest();
        if (ServletActionContext.getRequest() == null) {
            ServletActionContext.setRequest(request);
        }
        if (ServletActionContext.getResponse() == null) {
            ServletActionContext.setResponse(response);
        }
        if (ServletContextThreadLocal.getRequest() == null) {
            ServletContextThreadLocal.setRequest(request);
        }
        setupThreadLocalCache();
    }

    public void restoreOriginalValues()
    {
        cleanupThreadLocalCache();

        ServletActionContext.setRequest(originalRequestInActionContext);
        ServletActionContext.setResponse(originalResponseInActionContext);

        ServletContextThreadLocal.setRequest(originalRequestInServletContext);
    }

    private void setupThreadLocalCache()
    {
        ThreadLocalCache.put(THREAD_LOCAL_CACHE_MARKER_KEY, "Some marker");
        if (!isInitializedByConfluenceFilter()) {
            ThreadLocalCache.init();
        }
    }

    private void cleanupThreadLocalCache()
    {
        if (!isInitializedByConfluenceFilter()) {
            ThreadLocalCache.dispose();
        }
    }

    private boolean isInitializedByConfluenceFilter()
    {
        return "Some marker".equals(ThreadLocalCache.get(THREAD_LOCAL_CACHE_MARKER_KEY));
    }
}