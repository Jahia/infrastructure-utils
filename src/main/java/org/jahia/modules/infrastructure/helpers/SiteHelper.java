package org.jahia.modules.infrastructure.helpers;

import org.apache.jackrabbit.core.data.DataStoreException;
import org.jahia.api.Constants;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.sites.JahiaSitesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SiteHelper {

    private static final Logger logger = LoggerFactory.getLogger(SiteHelper.class);

    public static long getSiteBinariesSize(String sitekey) {
        return getSiteBinariesSize(sitekey, Constants.EDIT_WORKSPACE);
    }

    public static long getSiteBinariesSize(String sitekey, String workspace) {
        try {
            return JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, workspace, null, new JCRCallback<Long>() {
                @Override
                public Long doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    final JCRSiteNode site = JahiaSitesService.getInstance().getSiteByKey(sitekey, session);
                    final Map<String, Long> binaries = new HashMap<>();
                    processSubtree(site, binaries);
                    long computedSize = 0L;
                    for (Long size : binaries.values()) {
                        computedSize += size;
                    }
                    return computedSize;
                }

                private void processSubtree(JCRNodeWrapper node, Map<String, Long> binaries) throws RepositoryException {
                    final PropertyIterator properties = node.getProperties();

                    while (properties.hasNext()) {
                        final Property property = properties.nextProperty();
                        if (property.getType() != PropertyType.BINARY) continue;
                        final String propertyPath = property.getPath();
                        if (binaries.containsKey(propertyPath)) continue;
                        try {
                            binaries.put(propertyPath, property.getBinary().getSize());
                        } catch (DataStoreException e) {
                            logger.error("", e);
                        }
                    }

                    for (JCRNodeWrapper child : node.getNodes()) {
                        processSubtree(child, binaries);
                    }
                }
            });
        } catch (RepositoryException e) {
            logger.error("", e);
        }
        return -1L;
    }

}
