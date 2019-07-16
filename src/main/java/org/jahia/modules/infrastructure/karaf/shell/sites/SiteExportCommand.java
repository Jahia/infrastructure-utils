package org.jahia.modules.infrastructure.karaf.shell.sites;

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

    @Override
    public Object execute() throws Exception {
        final File exportDir = new File(System.getProperty("java.io.tmpdir"), "site-exports");
        final boolean folderCreated = exportDir.exists() || exportDir.mkdirs();

        if (folderCreated && exportDir.canWrite()) {
            //formattedTestDate = FastDateFormat.getInstance("yyyy_MM_dd-HH_mm_ss_SSS").format(testDate);
            final File outputDir = new File(exportDir, FastDateFormat.getInstance("yyyy_MM_dd-HH_mm_ss_SSS").format(System.currentTimeMillis()));
            final String outputDirPath = outputDir.getPath();
            SiteHelper.exportToFile(outputDirPath, Collections.singleton(sitekey), true);
            logger.info(String.format("Exported the site in %s", outputDirPath));
        } else {
            logger.error(String.format("Impossible to write the filesystem in %s", exportDir.getPath()));
        }
        return null;
    }
}
