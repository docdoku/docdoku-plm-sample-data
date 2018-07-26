/*
 * DocDoku, Professional Open Source
 * Copyright 2006 - 2017 DocDoku SARL
 *
 * This file is part of DocDokuPLM.
 *
 * DocDokuPLM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DocDokuPLM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with DocDokuPLM.  If not, see <http://www.gnu.org/licenses/>.
 */

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
