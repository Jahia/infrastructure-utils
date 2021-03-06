package org.jahia.modules.infrastructure.osgi;

import org.jahia.services.SpringContextSingleton;
import org.jahia.services.modulemanager.BundleInfo;
import org.jahia.services.modulemanager.ModuleManager;
import org.jahia.settings.SettingsBean;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

public class ModuleVersionsPurgeActivator implements BundleActivator {

    private static final Logger logger = LoggerFactory.getLogger(ModuleVersionsPurgeActivator.class);

    private BundleListener bundleListener;
    private int nbFormerVersionToKeep = 1;

    @Override
    public void start(BundleContext context) throws Exception {
        if (!SettingsBean.getInstance().isProcessingServer()) return;
        final boolean isActive = Boolean.parseBoolean(SettingsBean.getInstance().getPropertiesFile().getProperty("modules.versions.autoPurge", "false"));
        if (!isActive) return;

        context.addBundleListener(bundleListener = new BundleListener() {

            @Override
            public void bundleChanged(BundleEvent event) {
                if (event.getType() != BundleEvent.STARTED) {
                    return;
                }
                final Bundle changedBundle = event.getBundle();
                if (changedBundle == null) return;
                final Object header = changedBundle.getHeaders().get("Jahia-CI-Purge");
                if (!(header instanceof String) || !"true".equalsIgnoreCase((String) header)) return;
                final String symbolicName = changedBundle.getSymbolicName();
                final Version version = changedBundle.getVersion();
                logger.info(String.format("Bundle %s-%s started, let's scan for lower versions installed", symbolicName, version));
                for (Bundle b : context.getBundles()) {
                    final String bundleSN = b.getSymbolicName();
                    final Version bundleVersion = b.getVersion();
                    if (bundleSN.equals(symbolicName) && bundleVersion.compareTo(version) < 0) {
                        final long bundleId = b.getBundleId();
                        logger.info(String.format("Detected a module to purge: %s-%s (%s)", bundleSN, bundleVersion, bundleId));
                        try {
                            final ModuleManager moduleManager = (ModuleManager) SpringContextSingleton.getBean("ModuleManager");
                            moduleManager.uninstall(BundleInfo.fromBundle(b).getKey(), null);
                        } catch (NoSuchBeanDefinitionException nsbde) {
                            logger.error("Impossible to load the ModuleManager");
                        }
                    }
                }
            }
        });
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (bundleListener != null)
            context.removeBundleListener(bundleListener);
    }
}
