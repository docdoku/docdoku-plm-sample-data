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

import com.docdoku.api.DocdokuPLMBasicClient;
import com.docdoku.api.client.ApiException;
import com.docdoku.api.services.WorkspacesApi;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * @author Morgan GUIMARD
 */
public class Dev {

    private static final String devURL = "http://localhost:8080";
    private static final Logger LOGGER = Logger.getLogger(Dev.class.getName());

    private Dev() {
    }

    public static void main(String[] args) throws Exception {
        String random = UUID.randomUUID().toString().substring(0, 8);
        String login = random;
        String password = random;
        String workspaceId = random;
        LOGGER.info("Loading sample to " + devURL + " with login=" + login + " password=" + password + " workspace=" + workspaceId);
        Main.main(new String[]{login, password, workspaceId, devURL});

        // option --delete-on-finish
        if(args.length == 1 && "--delete-on-finish".equals(args[1])){
            deleteWorkspace(login, password, workspaceId, devURL);
        }
    }

    private static void deleteWorkspace(String login, String password, String workspace, String url) throws ApiException {
        LOGGER.info("Deleting workspace");
        new WorkspacesApi(new DocdokuPLMBasicClient(url + "/api", login, password).getClient()).deleteWorkspace(workspace);
    }

}
