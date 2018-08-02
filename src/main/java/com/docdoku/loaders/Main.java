/*
 * DocDoku, Professional Open Source
 * Copyright 2006 - 2015 DocDoku SARL
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

import com.docdoku.api.client.ApiException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Program entry, parses arguments from command line, launch the sample load process
 *
 * See SampleLoaderCommandLine class for available options
 *
 * @author Morgan GUIMARD
 */
public class Main {

    private static final Logger LOGGER = SampleLoaderLogger.getLOGGER();

    private Main() {
    }

    public static void main(String[] args) throws Exception {

        SampleLoaderCommandLine commandLine = new SampleLoaderCommandLine();
        CmdLineParser parser = new CmdLineParser(commandLine);

        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            LOGGER.log(Level.SEVERE, "Usage : ./loadSample.sh -u userLogin -p userPassword -w workspaceId -h host");
            return;
        }

        String login = commandLine.getLogin();
        String password = commandLine.getPassword();
        String workspaceId = commandLine.getWorkspaceId();
        String url = commandLine.getUrl();

        // Use generated workspace id if not specified
        if(null == workspaceId || "".equals(workspaceId.trim())){
            LOGGER.log(Level.INFO, "No workspace name supplied, generating one...");
            workspaceId = "wks-" + UUID.randomUUID().toString().substring(0,8);
            LOGGER.log(Level.INFO, "Using "+ workspaceId + " as workspace name ");
        }

        SampleLoader sampleLoader = new SampleLoader(login, password, workspaceId, url + "/api");

        try {
            sampleLoader.load();
            LOGGER.info("Congratulations ! \n Everything is ok, you can now connect to DocDokuPLM " + url + "\n" + "Credentials : " + login + "/" + password + "\n" + "Workspace: " + workspaceId );
        } catch (ApiException e) {
            LOGGER.log(Level.SEVERE, "Ooops, something went wrong while loading sample data", e);
        }
    }

}
