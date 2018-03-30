package com.atlassian.confluence.custom.plugins.util;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/* Utility class providing REST validation services */
public class RestValidate
{
    public static void notNull(Object object, Response.Status failureStatus)
    {
        if (object == null) {
            throw new WebApplicationException(failureStatus);
        }
    }

    public static void isTrue(boolean expression, Response.Status failureStatus)
    {
        if (!expression) {
            throw new WebApplicationException(failureStatus);
        }
    }

    public static void isFalse(boolean expression, int failureStatus)
    {
        if (expression) {
            throw new WebApplicationException(failureStatus);
        }
    }

    public static void isInstanceOf(Class<?> type, Object obj, int failureStatus)
    {
        if (!type.isInstance(obj)) {
            throw new WebApplicationException(failureStatus);
        }
    }
}
