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
package com.docdoku.loaders.documents;

import com.docdoku.core.exceptions.*;
import com.docdoku.core.services.IDocumentManagerWS;
import com.docdoku.loaders.tools.ScriptingTools;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Morgan GUIMARD
 */
public class DocumentsLoader {
    
    private static final Logger LOGGER = Logger.getLogger(DocumentsLoader.class.getName());

    private static IDocumentManagerWS dm;
    
    private static final String[] folders = {"Customers","Accounting","Users","Returns","Docs","Share"};

    public static boolean fillWorkspace(String serverURL, String workpaceId, String login, String password) {
        try {
            
            dm = ScriptingTools.createDocumentService(serverURL + "/services/document?wsdl", login, password);
            
            createFolders(workpaceId);
            createDocumentTemplates(workpaceId);
            createDocuments(workpaceId);
            
            return true;
            
        }catch(Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            return false; 
        }        
    }

    private static void createDocumentTemplates(String workpaceId) throws DocumentMasterTemplateAlreadyExistsException, NotAllowedException, WorkspaceNotFoundException, CreationException, AccessRightException, UserNotFoundException {
        dm.createDocumentMasterTemplate(workpaceId,"MyTemplate","Document template","DOC-***-###",null,true,false);
    }

    private static void createFolders(String workpaceId) throws NotAllowedException, WorkspaceNotFoundException, CreationException, AccessRightException, UserNotFoundException, FolderAlreadyExistsException, FolderNotFoundException {
        for (String folder:folders) {
            dm.createFolder(workpaceId, folder);
        }
    } 
    private static void createDocuments(String workpaceId) throws CreationException, FileAlreadyExistsException, DocumentRevisionAlreadyExistsException, WorkspaceNotFoundException, UserNotFoundException, NotAllowedException, DocumentMasterAlreadyExistsException, RoleNotFoundException, FolderNotFoundException, WorkflowModelNotFoundException, AccessRightException, DocumentMasterTemplateNotFoundException {
        dm.createDocumentMaster(workpaceId,"Welcome","Sample document","Welcome to DocdokuPLM",null,null,null,null,null);
        dm.createDocumentMaster(workpaceId,"DOC-AAA-001","Sample document","Nothing special","MyTemplate",null,null,null,null);

    }
}
