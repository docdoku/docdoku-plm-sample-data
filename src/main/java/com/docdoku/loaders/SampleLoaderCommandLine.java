package com.docdoku.loaders;

import org.kohsuke.args4j.Option;


/**
 * Available options for sample loader
 *
 * @author Morgan GUIMARD
 */

public class SampleLoaderCommandLine {

    @Option(required = true, name = "-u", aliases = "--user", metaVar = "<user>", usage = "user for login")
    protected String login;

    @Option(required = true, name = "-p", aliases = "--password", metaVar = "<password>", usage = "password for login")
    protected String password;

    @Option(required = true, name = "-h", aliases = "--host", metaVar = "<host>", usage = "host for server")
    protected String url;

    @Option(name = "-w", aliases = "--workspace", metaVar = "<workspace>", usage = "workspace to use")
    protected String workspaceId;

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getUrl() {
        return url;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }
}
