package org.jahia.modules.infrastructure.servlet;

import org.eclipse.gemini.blueprint.context.BundleContextAware;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.util.Map;

public class HttpServiceListener implements BundleContextAware {

    private static final Logger logger = LoggerFactory.getLogger(HttpServiceListener.class);
    private BundleContext bundleContext;
    private Map<String, HttpServlet> servlets;

    @Override
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void onBind(ServiceReference serviceReference) {
        final ServiceReference realServiceReference = bundleContext.getServiceReference(HttpService.class.getName());
        final HttpService httpService = (HttpService) bundleContext.getService(realServiceReference);
        try {
            for (String alias : servlets.keySet()) {
                httpService.registerServlet(String.format("/infrastructure/%s", alias), servlets.get(alias), null, null);
                logger.info(String.format("Successfully registered custom servlet at /modules/infrastructure/%s", alias));
            }

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
        for (String alias : servlets.keySet()) {
            httpService.unregister("/infrastructure/" + alias);
            logger.info(String.format("Successfully unregistered custom servlet from /modules/infrastructure/%s", alias));
        }
    }

    public void setServlets(Map<String, HttpServlet> servlets) {
        this.servlets = servlets;
    }
}
