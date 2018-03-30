package com.atlassian.confluence.custom.plugins.rest;

import com.atlassian.confluence.pages.Page;
import java.util.ArrayList;
import java.util.List;

public class JsonTreeNode
{
    public static final String TYPE_ANCESTOR = "ancestor";
    public static final String TYPE_CURRENT = "current";
    public static final String TYPE_NORMAL = "normal";
    public String type = "normal";
    public String id;
    public String title;
    public String link;
    public List<JsonTreeNode> children;

    JsonTreeNode(Page page, String link, boolean hasChildren)
    {
        this("normal", page.getIdAsString(), page.getTitle(), link, hasChildren ? new ArrayList() : null);
    }

    JsonTreeNode(String type, String id, String title, String link, List<JsonTreeNode> children)
    {
        this.type = type;
        this.id = id;
        this.title = title;
        this.link = link;
        this.children = children;
    }
}