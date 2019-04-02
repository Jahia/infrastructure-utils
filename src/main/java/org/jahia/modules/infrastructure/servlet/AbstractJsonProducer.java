package org.jahia.modules.infrastructure.servlet;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.utils.DateUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public abstract class AbstractJsonProducer extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(AbstractJsonProducer.class);

    private String requiredPermission;
    private boolean unauthenticatedAccessAllowed;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final long start = System.currentTimeMillis();
        final JSONObject result = new JSONObject();
        final PrintWriter writer = response.getWriter();
        disableClientSideCache(response);
        try {
            if (!isUserAllowed()) {
                result.put("error", "Unsufficient privileges");
            } else {
                doGetInternal(request, response, result);
            }
            result.put("duration", DateUtils.formatDurationWords(System.currentTimeMillis() - start));
        } catch (JSONException e) {
            logger.error("", e);
        }

        writer.println(result.toString());
    }

    abstract protected void doGetInternal(HttpServletRequest request, HttpServletResponse response, JSONObject result) throws JSONException;

    private void disableClientSideCache(HttpServletResponse response) {
        response.setHeader("Expires", "Mon, 26 Jul 1990 05:00:00 GMT");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
    }

    private boolean isUserAllowed() {

        final String requiredPermission = getRequiredPermission();
        if (StringUtils.isBlank(requiredPermission)) {
            if (isUnauthenticatedAccessAllowed())
                return true;
            else {
                try {
                    return !"guest".equals(JCRSessionFactory.getInstance().getCurrentUserSession().getUser().getUsername());
                } catch (RepositoryException e) {
                    logger.error("", e);
                    return false;
                }
            }
        }

        try {
            final JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession();
            return session.getNode("/sites/systemsite").hasPermission(requiredPermission);
        } catch (PathNotFoundException ignored) {
            return false;
        } catch (RepositoryException e) {
            logger.error("", e);
            return false;
        }

    }

    public final String getRequiredPermission() {
        return requiredPermission;
    }

    public final void setRequiredPermission(String requiredPermission) {
        this.requiredPermission = requiredPermission;
    }

    public final boolean isUnauthenticatedAccessAllowed() {
        return unauthenticatedAccessAllowed;
    }

    public final void setUnauthenticatedAccessAllowed(boolean unauthenticatedAccessAllowed) {
        this.unauthenticatedAccessAllowed = unauthenticatedAccessAllowed;
    }
}
