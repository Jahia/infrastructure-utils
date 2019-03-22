package org.jahia.modules.infrastructure.servlet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.infrastructure.helpers.SiteHelper;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SiteInfosServlet extends AbstractJsonProducer {

    private static final Logger logger = LoggerFactory.getLogger(SiteInfosServlet.class);

    @Override
    protected void doGetInternal(HttpServletRequest request, HttpServletResponse response, JSONObject result) throws JSONException {
        final String pathInfo = request.getPathInfo();
        if (StringUtils.isBlank(pathInfo) || "/".equals(pathInfo)) {
            result.put("sites", "to be coded");
        } else {
            final String sitekey = pathInfo.substring(1);
            final long siteBinariesSize = SiteHelper.getSiteBinariesSize(sitekey);
            final JSONObject jsonObject = new JSONObject();
            result.put(sitekey, jsonObject);
            jsonObject.put("Raw size", siteBinariesSize);
            jsonObject.put("Pretty size", FileUtils.byteCountToDisplaySize(siteBinariesSize));
        }
    }
}
