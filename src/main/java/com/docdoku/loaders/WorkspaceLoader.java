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

import com.docdoku.loaders.common.LOVLoader;
import com.docdoku.loaders.documents.DocumentsLoader;
import com.docdoku.loaders.products.ProductLoader;
import com.docdoku.loaders.utils.JsonParserConstants;

import javax.activation.DataHandler;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.Console;
import java.io.FileReader;
import java.io.StringReader;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Morgan GUIMARD
 */
public class WorkspaceLoader {
    
    private static String workspace;
    
    private static final Logger LOGGER = Logger.getLogger(WorkspaceLoader.class.getName());

    private WorkspaceLoader(){
    }

    public static void main(String[] args) throws Exception {

        try{
            
            String serverURL;
            String login;
            String password;

            boolean completlySuccess = true;
            
            if (args.length >= 3) {
                login =args[0];
                password =args[1];
                workspace=args[2];
                serverURL= (args.length==4) ? args[3] : "http://localhost:8080";
            }else{
                Console c = System.console();
                if(c != null) {
                    login = c.readLine("Please enter your login: ");
                    password = new String(c.readPassword("Please enter your password: "));
                    workspace = c.readLine("Please enter the workspace into which the sample data will be imported: ");
                    serverURL = c.readLine("Please enter the URL of your DocDokuPLM server, http://localhost:8080 for example: ");
                }else{
                    LOGGER.log(Level.SEVERE, "cannot read arguments");
                    return;
                }
            }

            URL exempleURL = WorkspaceLoader.class.getResource("/com/docdoku/loaders/sample.json");
            DataHandler dh = new DataHandler(exempleURL);

            JsonReader reader = Json.createReader(dh.getInputStream());
            JsonObject exempleToLoad = (JsonObject) reader.read();
            reader.close();


            completlySuccess &= LOVLoader.fillWorkspace(serverURL, workspace, login, password, exempleToLoad.getJsonArray(JsonParserConstants.LOV));
            completlySuccess &= DocumentsLoader.fillWorkspace(serverURL, workspace, login, password, exempleToLoad.getJsonObject(JsonParserConstants.DOC));
            completlySuccess &= ProductLoader.fillWorkspace(serverURL, workspace, login, password, exempleToLoad.getJsonObject(JsonParserConstants.PART_PART));



            if(completlySuccess){
                LOGGER.log(Level.INFO, "...done!");
            }else{
                LOGGER.log(Level.WARNING, "...incomplete!");
            }
            
        } catch (Exception e){
            LOGGER.log(Level.SEVERE, "...FAIL!",e);
        }
    }
    
}
