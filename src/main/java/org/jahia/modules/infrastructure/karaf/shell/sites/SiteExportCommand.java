package org.jahia.modules.infrastructure.karaf.shell.sites;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.jahia.modules.infrastructure.helpers.SiteHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;

@Command(scope = "infrastructure", name = "site-export", description = "Command to export websites")
@Service
public class SiteExportCommand implements Action {

    private static final Logger logger = LoggerFactory.getLogger(SiteExportCommand.class);

    // TODO : implement a completer
    // TODO : make it multiple
    @Option(required = true, name = "-k", aliases = "--sitekey", description = "Site key")
    private String sitekey;

    @Option(required = false, name = "-x", aliases = "--xslpath", description = "Path to a XSL file to apply on the exported repository*.xml exported files")
    private String xslPath;

    @Override
    public Object execute() throws Exception {
        if (xslPath != null) {
            final File xslfile = new File(xslPath);
            if (!xslfile.exists()) {
                logTwice("error", String.format("The specified xsl file does not exist: %s", xslfile.getPath()));
                return null;
            }
        }

        final File exportDir = new File(System.getProperty("java.io.tmpdir"), "site-exports");
        final boolean folderCreated = exportDir.exists() || exportDir.mkdirs();

        if (folderCreated && exportDir.canWrite()) {
            //formattedTestDate = FastDateFormat.getInstance("yyyy_MM_dd-HH_mm_ss_SSS").format(testDate);
            final File outputDir = new File(exportDir, FastDateFormat.getInstance("yyyy_MM_dd-HH_mm_ss_SSS").format(System.currentTimeMillis()));
            final String outputDirPath = outputDir.getPath();
            SiteHelper.exportToFile(outputDirPath, Collections.singleton(sitekey), true, xslPath);
            logTwice(String.format("Exported the site in %s", outputDirPath));
        } else {
            logTwice("error", String.format("Impossible to write the filesystem in %s", exportDir.getPath()));
        }
        return null;
    }

    // TODO : move to parent class and reuse across commands
    private void logTwice(String msg) {
        logTwice("info", msg);
    }

    private void logTwice(String level, String msg) {
        switch (StringUtils.lowerCase(level)) {
            case "info": logger.info(msg); break;
            case "warn": logger.warn(msg); break;
            case "error": logger.error(msg); break;
            default: logger.debug(msg);
        }
        System.out.println(msg);
    }
}
