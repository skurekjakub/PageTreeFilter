package com.atlassian.confluence.custom.plugins.util;

import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.user.User;

public class UserUtils
{
    public static User getCurrentUser()
    {
        return AuthenticatedUserThreadLocal.get();
    }

    public static boolean isAnonymous(User user)
    {
        return user == null;
    }

    public static boolean isNotAnonymous(User user)
    {

        return user != null;
    }
}