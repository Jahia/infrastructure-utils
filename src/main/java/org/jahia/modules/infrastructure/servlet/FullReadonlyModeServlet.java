package org.jahia.modules.infrastructure.servlet;

import org.apache.commons.lang.StringUtils;
import org.jahia.settings.readonlymode.ReadOnlyModeController;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FullReadonlyModeServlet extends AbstractJsonProducer {

    private static final Logger logger = LoggerFactory.getLogger(FullReadonlyModeServlet.class);

    @Override
    protected void doGetInternal(HttpServletRequest request, HttpServletResponse response, JSONObject result) {
        try {
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
                case "/":
                    break;
                default:
                    logger.error("Unexpected action type: " + action);
                    result.put("error", String.format("Unexpected action: %s", action));
                    return;
            }
            result.put("status", ReadOnlyModeController.getInstance().getReadOnlyStatus());
        } catch (JSONException e) {
            logger.error("", e);  //TODO: review me, I'm generated
        }
    }
}
