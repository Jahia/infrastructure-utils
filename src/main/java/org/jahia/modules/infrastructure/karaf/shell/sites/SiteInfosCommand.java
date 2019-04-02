package org.jahia.modules.infrastructure.karaf.shell.sites;

import org.apache.commons.io.FileUtils;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.jahia.api.Constants;
import org.jahia.modules.infrastructure.helpers.SiteHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "infrastructure", name = "site-infos", description = "Gives some information about a website")
@Service
public class SiteInfosCommand implements Action {

    private static final Logger logger = LoggerFactory.getLogger(SiteInfosCommand.class);

    @Argument(description = "Data to retrieve")
    @Completion(value = StringsCompleter.class, values = {"binaries-size"})
    private String data;

    // TODO : implement a completer
    @Option(required = true, name = "-k", aliases = "--sitekey", description = "Site key")
    private String sitekey;

    @Option(name = "-ws", aliases = "--workspace")
    @Completion(value = StringsCompleter.class, values = {Constants.EDIT_WORKSPACE, Constants.LIVE_WORKSPACE}, caseSensitive = true)
    private String workspace;

    @Override
    public Object execute() throws Exception {
        if (data == null) {
            System.out.println("Unspecified data");
            return null;
        }

        switch (data) {
            case "binaries-size":
                final long size = SiteHelper.getSiteBinariesSize(sitekey, workspace);
                System.out.println(FileUtils.byteCountToDisplaySize(size));
                break;
            default:
                System.out.println("Unexpected value for data");
        }
        return null;
    }
}
