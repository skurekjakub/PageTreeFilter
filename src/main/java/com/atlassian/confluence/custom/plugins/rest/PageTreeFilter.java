package com.atlassian.confluence.custom.plugins.rest;

/* Imports custom Atlassian Spring annotations */
import com.atlassian.confluence.custom.plugins.util.UserUtils;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;

/* Imports necessary Atlassian services and objects */
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.labels.Label;

/* Imports Spring annotations and servlet objects */
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/* Imports basic Java objects */
import java.util.ArrayList;
import java.util.List;

import com.atlassian.user.User;
import org.apache.commons.lang.StringUtils;

/* Imports custom helper objects */
import com.atlassian.confluence.custom.plugins.util.RestValidate;
import com.atlassian.confluence.custom.plugins.util.ThreadLocalHelper;

/**
 * Provides two additional REST endpoints for the native Confluence REST API.
 * '/rest/treefilter/getchildrenrecursive' returns a list of children on a path from a parent to a specific child page,
 * it also lists all other child pages on each visited level sorted by their position in the Confluence space.
 * '/rest/treefilter/getchildren' returns a list of children for a given parent, sorted by their position in the Confluence space
 */
@Path("/")
@Produces({MediaType.APPLICATION_JSON})
/* Tells Spring to scan this class for members that need to be initialized using its DI container. */
@Scanned
public class PageTreeFilter {

    @ComponentImport
    private final PageManager pageManager;
    @ComponentImport
    private final PermissionManager permissionManager;

    /* IDK why this is here, looks like the best place to store these, since i need them in
     * a couple of methods and passing them over and over down the chain seems weird. */
    private long spaceId;

    /* 'Stack' used in the Url building process on initial pageload */
    private String[] urlFragmentStack = new String[10];
    private int stackDepth = 0;

    /* Tells Spring where to DI */
    @Inject
    public PageTreeFilter(PageManager pageManager, PermissionManager permissionManager) {
        this.pageManager = pageManager;
        this.permissionManager = permissionManager;
    }

    /* Endpoint kept for testing purposes. */
    @GET
    @Path("/")
    @AnonymousAllowed
    @Produces({"application/json"})
    public Response helloWorld()
    {
        return Response.ok("{'value': 'Hello World!'}").build();
    }


    /* Endpoint kept for testing purposes. Returns a page and its direct descendants. */
    @GET
    @Path("getpage")
    @AnonymousAllowed
    @Produces({"application/json"})
    public Response getPage(@QueryParam("spaceKey")final String spaceKey,@QueryParam("pageTitle")final String pageTitle) {
        Page page = getPageByTitle(spaceKey, pageTitle);
        if (page != null) {
            if (page.hasChildren()) {
                List<JsonTreeNode> children = new ArrayList<>();
                for (Page child :
                        page.getSortedChildren()) {
                    children.add(new JsonTreeNode("normal", page.getSpaceKey(), child.getTitle(), child.getUrlPath(), null));
                }
                JsonTreeNode node = new JsonTreeNode("normal", page.getSpaceKey(), page.getTitle(), page.getUrlPath(), children);
                return Response.ok(node).build();
            }
            JsonTreeNode node = new JsonTreeNode("normal", page.getSpaceKey(), page.getTitle(), page.getUrlPath(), null);
            return Response.ok(node).build();
        }
        return Response.status(404).build();
    }


    /* Endpoint called on inital page load.
    NOTE: Modifications of this method's signature will require additional customization of our Scroll Viewport theme! */
    @GET
    @Path("getchildrenrecursive")
    @AnonymousAllowed
    @Produces({"application/json"})
    public Response getChildrenRecursive(@QueryParam("spaceId") final long spaceId, @QueryParam("parent") final String parentTitle,
                                         @QueryParam("current") final String currentTitle, @QueryParam("label") final String label,
                                         @Context final HttpServletRequest request, @Context final HttpServletResponse response)
    {
        this.spaceId = spaceId;
        String spaceKey = getPageById(spaceId).getSpaceKey().toLowerCase();

        /* First fragment of the URL */
        urlFragmentStack[stackDepth] = convertTitleToUrlFragment(spaceKey);

        /* Prepares the request environment. Required by Java applications ('servlets') exposing REST services.
         * Ensures no concurrency shenanigans can occur, etc.  */
        ThreadLocalHelper threadLocalHelper = new ThreadLocalHelper(request, response);
        threadLocalHelper.prepareThreadLocals();
        try
        {
            Page parentPage = getPageByTitle(spaceKey, processParentTitle(parentTitle));
            Page currentPage = getPageByTitle(spaceKey, currentTitle);

            /* Null check. Should not be necessary since the calls should always come with valid parameters
             * and in case an exception does occur, will be handled by the instance itself.  */
            RestValidate.notNull(parentPage, Response.Status.NOT_FOUND);

            /* Permission evaluation against an anonymous user. Logged in users not taken into account. */
            RestValidate.isTrue(canView(parentPage), Response.Status.FORBIDDEN);     //?

            /* Returns a JsonTreeNode structure up to the page specified by the "current" parameter */
            List<JsonTreeNode> children = getChildrenNodesFiltered(getVisibleChildren(parentPage, label), currentPage, label);

            return Response.ok(children).build();
        }
        /* Cleans up */
        finally
        {
            threadLocalHelper.restoreOriginalValues();
        }
    }


    /* Endpoint called when expanding a subtree on a loaded page.
    NOTE: Modification of this method's signature will require additional customizations in our Scroll Viewport theme! */
    @GET
    @Path("getchildren")
    @AnonymousAllowed
    @Produces({"application/json"})
    public Response getChildren(@QueryParam("spaceId") final long spaceId, @QueryParam("parent") final String parentTitle,
                                @QueryParam("parentLink") final String parentLink, @QueryParam("label") final String label,
                                @Context final HttpServletRequest request, @Context final HttpServletResponse response)
    {
        this.spaceId = spaceId;
        String spaceKey = getPageById(spaceId).getSpaceKey().toLowerCase();

        /* Prepares the request environment. Required by Java applications ('servlets') exposing REST services.
         * Ensures no concurrency shenanigans can occur, etc.  */
        ThreadLocalHelper threadLocalHelper = new ThreadLocalHelper(request, response);
        threadLocalHelper.prepareThreadLocals();
        try
        {
            Page parentPage = getPageByTitle(spaceKey, parentTitle);

            RestValidate.notNull(parentPage, Response.Status.NOT_FOUND);
            RestValidate.isTrue(canView(parentPage), Response.Status.FORBIDDEN);

            List<JsonTreeNode> children = getDirectDescendants(getVisibleChildren(parentPage, label), label, parentLink);

            return Response.ok(children).build();
        }
        finally
        {
            threadLocalHelper.restoreOriginalValues();
        }
    }


    /* =========================================PRIVATE METHODS===================================================== */

    /* Recursively retrieves all children up to the current node based on supplied parameters */
    private List<JsonTreeNode> getChildrenNodesFiltered(List<Page> parentsVisibleChildren, Page currentPage, String label)
    {
        List<JsonTreeNode> result = new ArrayList<>();
        for (Page child : parentsVisibleChildren)
        {
            List<Page> childsVisibleChildren = getVisibleChildren(child, label);

            JsonTreeNode node = new JsonTreeNode(child, buildUrl(child), !childsVisibleChildren.isEmpty());

            /* Do we need recursion? */
            if (currentPage != null) {
                boolean isAncestor = currentPage.getAncestors().contains(child);
                boolean isCurrent = currentPage.equals(child);
                if ((isAncestor) || ((isCurrent) && (!childsVisibleChildren.isEmpty()))) {
                    stackDepth++;
                    urlFragmentStack[stackDepth] = convertTitleToUrlFragment(node.title);
                    node.children = getChildrenNodesFiltered(childsVisibleChildren, currentPage, label);
                }
                if (isAncestor) {
                    node.type = "ancestor";
                }
                if (isCurrent) {
                    node.type = "current";
                }
            }
            result.add(node);
        }
        if(stackDepth != 0)
            stackDepth--;
        return result;
    }


    private List<JsonTreeNode> getDirectDescendants(List<Page> parentsVisibleChildren, String label, String parentLink) {
        List<JsonTreeNode> result = new ArrayList<>();
        for (Page child : parentsVisibleChildren)
        {
            JsonTreeNode node = new JsonTreeNode(child, parentLink + convertTitleToUrlFragment(child.getTitle()), !getVisibleChildren(child, label).isEmpty());

            result.add(node);
        }
        return result;

    }


    private String buildUrl(Page child) {
        StringBuilder link = new StringBuilder();
        for (int i = 0; i<= stackDepth; i++) {
            link.append(urlFragmentStack[i]);
        }
        link.append(convertTitleToUrlFragment(child.getTitle()));
        return link.toString();
    }


    /* Retrieves a user from current context and evaluates his permissions for the given page */
    private boolean canView(Page page)
    {
        return permissionManager.hasPermission(UserUtils.getCurrentUser(), Permission.VIEW, page);
    }

    
    /* This is what not knowing RegEx leads to */
    private String convertTitleToUrlFragment(String title) {
        return "/" + title.toLowerCase()
                .replace("(", "")
                .replace(")","")
                .replace(" ", "-")
                .replace("&", "-")
                .replace("---","-")
                .replace(".", "-")
                .replace("/","-");
    }


    private List<String> retrieveLabels(Page page) {
        List<String> labels = new ArrayList<String>();
        for (Label label: page.getLabels())
        {
            labels.add(label.toString());
        }
        return labels;
    }


    private List<Page> getVisibleChildren(Page page, String label)
    {
        List<Page> result = new ArrayList<>();
        for (Page child : page.getSortedChildren()) {
            if(canView(child) && !retrieveLabels(child).contains(label)) {
                result.add(child);
            }
        }
        return result;
    }

    private Page getRootPage() {
        return getPageById(spaceId);
    }


    private String processParentTitle(String parentTitle) {
        /* Check for initial page load request - parent always set to root in this case, only time i
        cannot guarantee the actual page title will be sent to the endpoint */
        if(parentTitle.charAt(0) == '/')
        {
            return getRootPage().getTitle();
        }
        return parentTitle;
    }


    private Page getPageByTitle(String spaceKey, String title)
    {
        return StringUtils.isNotBlank(title) ?  pageManager.getPage(spaceKey, title) : null;
    }


    private Page getPageById(long Id)
    {
        return pageManager.getPage(Id);
    }
}