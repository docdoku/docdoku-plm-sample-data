package com.docdoku.loaders.documents;

import com.docdoku.core.exceptions.*;
import com.docdoku.core.services.IDocumentManagerWS;
import com.docdoku.loaders.ScriptingTools;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by morgan on 05/02/15.
 */
public class DocumentsLoader {
    
    private static final Logger LOGGER = Logger.getLogger(DocumentsLoader.class.getName());

    private static IDocumentManagerWS dm;
    
    private static final String[] folders = {"Customers","Accounting","Users","Returns","Docs","Share"};

    public static boolean fillWorkspace(String serverURL, String workpaceId, String login, String password) {
        try {
            dm = ScriptingTools.createDocumentService(serverURL + "/services/document?wsdl", login, password);
            //createFolders(workpaceId);
            createDocumentTemplates(workpaceId);
            createDocuments(workpaceId);
            return true;
        }catch(Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            return false; 
        }        
    }

    private static void createDocumentTemplates(String workpaceId) throws DocumentMasterTemplateAlreadyExistsException, NotAllowedException, WorkspaceNotFoundException, CreationException, AccessRightException, UserNotFoundException {
        //String pWorkspaceId, String pId, String pDocumentType, String pMask, InstanceAttributeTemplate[] pAttributeTemplates, boolean idGenerated, boolean attributesLocked
        dm.createDocumentMasterTemplate(workpaceId,"MyTemplate","Document template","DOC-***-###",null,true,false);
    }

    private static void createFolders(String workpaceId) throws NotAllowedException, WorkspaceNotFoundException, CreationException, AccessRightException, UserNotFoundException, FolderAlreadyExistsException, FolderNotFoundException {
        for (String folder:folders) {
            dm.createFolder(workpaceId, folder);
        }
    } 
    private static void createDocuments(String workpaceId) throws CreationException, FileAlreadyExistsException, DocumentRevisionAlreadyExistsException, WorkspaceNotFoundException, UserNotFoundException, NotAllowedException, DocumentMasterAlreadyExistsException, RoleNotFoundException, FolderNotFoundException, WorkflowModelNotFoundException, AccessRightException, DocumentMasterTemplateNotFoundException {

        // String pParentFolder, String pDocMId, String pTitle, String pDescription, String pDocMTemplateId, String pWorkflowModelId, ACLUserEntry[] pACLUserEntries, ACLUserGroupEntry[] pACLUserGroupEntries, Map<String, String> roleMappings
        dm.createDocumentMaster(workpaceId,"Welcome","Sample document","Welcome to DocdokuPLM",null,null,null,null,null);
        dm.createDocumentMaster(workpaceId,"DOC-AAA-001","Sample document","Nothing special","MyTemplate",null,null,null,null);

    }
}
