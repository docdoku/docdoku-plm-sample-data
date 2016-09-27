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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Morgan GUIMARD
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private Main(){
    }

    public static void main(String[] args) throws Exception {

        if (args.length != 4) {
            LOGGER.log(Level.SEVERE,"Usage : ./loadSample.sh [login] [password] [workspaceId] [url]");
            return;
        }

        String login = args[0];
        String password = args[1];
        String workspaceId = args[2];
        String url = args[3];

        SampleLoader sampleLoader = new SampleLoader(login, password, workspaceId, url + "/api");

        try {
            sampleLoader.load();
            LOGGER.info("Everything done, you can now connect to DocdokuPLM " + url + "\n" + "Credentials : " + login + "/" + password);
        } catch (ApiException e){
            LOGGER.log(Level.SEVERE,"Ooops, something went wrong while loading sample data", e);
        }
    }
    
}
