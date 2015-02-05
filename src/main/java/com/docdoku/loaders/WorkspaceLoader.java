package com.docdoku.loaders;

import com.docdoku.loaders.documents.DocumentsLoader;

import java.io.Console;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by morgan on 05/02/15.
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

            completlySuccess &= DocumentsLoader.fillWorkspace(serverURL,workspace,login,password);            
            
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
