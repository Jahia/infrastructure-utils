package org.jahia.modules.infrastructure.helpers;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.jahia.api.Constants;
import org.jahia.bin.listeners.JahiaContextLoaderListener;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.importexport.ImportExportService;
import org.jahia.services.sites.JahiaSitesService;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public static void exportToFile(String exportPath, Set<String> siteKeys) throws RepositoryException {
        exportToFile(exportPath, siteKeys, true, null);
    }

    public static void exportToFile(String exportPath, Set<String> siteKeys, boolean exportLive, String customXSLPath) throws RepositoryException {

        final Map<String, Object> params = new HashMap<String, Object>(15);

        params.put(ImportExportService.VIEW_CONTENT, true);
        params.put(ImportExportService.VIEW_VERSION, false);
        params.put(ImportExportService.VIEW_ACL, true);
        params.put(ImportExportService.VIEW_METADATA, true);
        params.put(ImportExportService.VIEW_JAHIALINKS, true);
        params.put(ImportExportService.VIEW_WORKFLOW, true);
        params.put(ImportExportService.SERVER_DIRECTORY, exportPath);
        params.put(ImportExportService.INCLUDE_ALL_FILES, true);
        params.put(ImportExportService.INCLUDE_TEMPLATES, true);
        params.put(ImportExportService.INCLUDE_SITE_INFOS, true);
        params.put(ImportExportService.INCLUDE_DEFINITIONS, true);
        params.put(ImportExportService.INCLUDE_LIVE_EXPORT, exportLive);
        params.put(ImportExportService.INCLUDE_USERS, true);
        params.put(ImportExportService.INCLUDE_ROLES, true);
        final String cleanupXsl = StringUtils.isNotBlank(customXSLPath) ? customXSLPath :
                JahiaContextLoaderListener.getServletContext().getRealPath("/WEB-INF/etc/repository/export/cleanup-custom.xsl");
        params.put(ImportExportService.XSL_PATH, cleanupXsl);

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.LIVE_WORKSPACE, null, new JCRCallback<Void>() {
            @Override
            public Void doInJCR(JCRSessionWrapper session) throws RepositoryException {
                final List<JCRSiteNode> sites = new ArrayList<JCRSiteNode>();
                if (siteKeys != null) {
                    for (final String sitekey : siteKeys) {
                        try {
                            final JCRSiteNode site = ServicesRegistry.getInstance()
                                    .getJahiaSitesService().getSiteByKey(sitekey, session);
                            sites.add(site);
                        } catch (PathNotFoundException pnfe) {
                            logger.error(String.format("The site %s does not exist", sitekey));
                        }
                    }
                }

                if (CollectionUtils.isEmpty(sites)) {
                    logger.warn("No site to export");
                    return null;
                }

                JahiaUser currentUser = null;
                try {
                    currentUser = JCRSessionFactory.getInstance().getCurrentUser();
                    if (currentUser == null) JCRSessionFactory.getInstance().setCurrentUser(JahiaUserManagerService.getInstance().lookupRootUser().getJahiaUser());
                    ServicesRegistry.getInstance().getImportExportService().exportSites(new ByteArrayOutputStream(), params, sites);
                } catch (IOException | SAXException | TransformerException e) {
                    logger.error("Cannot export site(s)", e);
                } finally {
                    if (currentUser != null) {
                        JCRSessionFactory.getInstance().setCurrentUser(currentUser);
                    }
                }
                return null;
            }
        });
    }

}
