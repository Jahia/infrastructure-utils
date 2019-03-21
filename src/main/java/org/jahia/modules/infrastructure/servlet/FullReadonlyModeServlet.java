package org.jahia.modules.infrastructure.servlet;

import org.apache.commons.lang.StringUtils;
import org.eclipse.gemini.blueprint.context.BundleContextAware;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.settings.readonlymode.ReadOnlyModeController;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class FullReadonlyModeServlet extends HttpServlet implements BundleContextAware {

    private static final Logger logger = LoggerFactory.getLogger(FullReadonlyModeServlet.class);
    BundleContext bundleContext;

    @Override
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void onBind(ServiceReference serviceReference) {
        final ServiceReference realServiceReference = bundleContext.getServiceReference(HttpService.class.getName());
        final HttpService httpService = (HttpService) bundleContext.getService(realServiceReference);
        try {
            httpService.registerServlet("/infrastructure/readonlymode", this, null, null);
            logger.info("Successfully registered custom servlet at /modules/infrastructure/readonlymode");
        } catch (ServletException | NamespaceException e) {
            logger.error("", e);
        }

    }

    public void onUnbind(ServiceReference serviceReference) {

        if (serviceReference == null) {
            return;
        }
        final ServiceReference realServiceReference = bundleContext.getServiceReference(HttpService.class.getName());
        if (realServiceReference == null) {
            return;
        }
        final HttpService httpService = (HttpService) bundleContext.getService(realServiceReference);
        if (httpService == null) {
            return;
        }
        httpService.unregister("/infrastructure/readonlymode");
        logger.info("Successfully unregistered custom servlet from /modules/infrastructure/readonlymode");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final JSONObject result = new JSONObject();
        final PrintWriter writer = response.getWriter();
        try {
            if(!isUserAllowed()) {
                result.put("error", "Unsufficient privileges");
                disableClientSideCache(response);
                writer.println(result.toString());
                return;
            }

            String pathInfo = request.getPathInfo();
            if (pathInfo == null) pathInfo = StringUtils.EMPTY;
            final String action = pathInfo.toUpperCase();
            switch (action) {
                case "/ENABLE":
                    try {
                        ReadOnlyModeController.getInstance().switchReadOnlyMode(true);
                        result.put("action", "Enabled the readonly mode");
                    } catch (IllegalStateException ignored) {
                        // already in readonly mode
                    }
                    break;
                case "/DISABLE":
                    try {
                        ReadOnlyModeController.getInstance().switchReadOnlyMode(false);
                        result.put("action", "Disabled the readonly mode");
                    } catch (IllegalStateException ignored) {
                        // already out of readonly mode
                    }
                    break;
                case StringUtils.EMPTY:
                    break;
                default:
                    logger.error("Unexpected action type: " + action);
                    result.put("error", String.format("Unexpected action: %s", action));
                    disableClientSideCache(response);
                    writer.println(result.toString());
                    return;
            }
            result.put("status", ReadOnlyModeController.getInstance().getReadOnlyStatus());
        } catch (JSONException e) {
            logger.error("", e);  //TODO: review me, I'm generated
        }

        disableClientSideCache(response);
        writer.println(result.toString());
    }

    private void disableClientSideCache(HttpServletResponse response) {
        response.setHeader("Expires", "Mon, 26 Jul 1990 05:00:00 GMT");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
    }

    private boolean isUserAllowed() {

        try {
            final JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession();
            return session.getNode("/sites/systemsite").hasPermission("readonlyMode");
        } catch (RepositoryException e) {
            logger.error("", e);
            return false;
        }

    }
}
