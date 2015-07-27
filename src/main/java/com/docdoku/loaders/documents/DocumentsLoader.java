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

import com.docdoku.core.document.DocumentIterationKey;
import com.docdoku.core.document.DocumentRevision;
import com.docdoku.core.document.DocumentRevisionKey;
import com.docdoku.core.meta.InstanceAttribute;
import com.docdoku.core.meta.InstanceAttributeTemplate;
import com.docdoku.core.services.IDocumentManagerWS;
import com.docdoku.loaders.tools.ScriptingTools;
import com.docdoku.loaders.tools.UtilsLoader;
import com.docdoku.loaders.utils.JsonParserConstants;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Morgan GUIMARD
 */
public class DocumentsLoader {
    
    private static final Logger LOGGER = Logger.getLogger(DocumentsLoader.class.getName());

    private static IDocumentManagerWS dm;

    public static boolean fillWorkspace(String serverURL, String workspaceId, String login, String password, JsonObject partObject) {
        try {
            
            dm = ScriptingTools.createDocumentService(serverURL + "/services/document?wsdl", login, password);
            
            createFolders(workspaceId, partObject.getJsonArray(JsonParserConstants.DOC_FOLDERS));

            JsonArray templates = partObject.getJsonArray(JsonParserConstants.DOC_TEMPLATE);
            if (templates != null) {
                for (int i = 0; i < templates.size(); i++) {
                    createDocumentTemplates(workspaceId, templates.getJsonObject(i));
                }
            }

            JsonArray documents = partObject.getJsonArray(JsonParserConstants.DOC_DOCUMENTS);
            if (documents != null) {
                for (int i = 0; i < documents.size(); i++) {
                    createDocuments(workspaceId, documents.getJsonObject(i));
                }
            }
            
            return true;
            
        }catch(Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            return false; 
        }        
    }

    private static void createDocumentTemplates(String workpaceId, JsonObject template){

        String templateId = template.getString(JsonParserConstants.DOC_TEMPLATE_ID, null);
        String templateType = template.getString(JsonParserConstants.DOC_TEMPLATE_TYPE, null);
        String templateMask = template.getString(JsonParserConstants.DOC_TEMPLATE_MASK, null);
        boolean templateAttributesLocked = template.getBoolean(JsonParserConstants.DOC_TEMPLATE_ATTRIBUTE_LOCKED, false);
        boolean templateIdGeneration = template.getBoolean(JsonParserConstants.DOC_TEMPLATE_ID_GENERATION, false);
        JsonArray attributes = template.getJsonArray(JsonParserConstants.DOC_TEMPLATE_ATTRIBUTES);


        List<InstanceAttributeTemplate> attributesTemplates = new ArrayList<>();
        List<String> lovNames = new ArrayList<>();
        for (int i = 0; i < attributes.size(); i++) {
            JsonObject attributeTemplate = attributes.getJsonObject(i);
            String name = attributeTemplate.getString(JsonParserConstants.ATTRIBUTE_NAME, null);
            String type = attributeTemplate.getString(JsonParserConstants.ATTRIBUTE_TYPE, null);
            String lovName = attributeTemplate.getString(JsonParserConstants.ATTRIBUTE_LOV_NAME, null);
            boolean mandatory = attributeTemplate.getBoolean(JsonParserConstants.ATTRIBUTE_MANDATORY, false);

            if (name != null && !name.equalsIgnoreCase("") && type!=null && !type.equalsIgnoreCase("")){
                InstanceAttributeTemplate attributeTemplateInstance = UtilsLoader.getInstanceAttributeTemplate(name, type, mandatory, lovName, workpaceId);
                if (attributeTemplateInstance != null) {
                    attributesTemplates.add(attributeTemplateInstance);
                    if (lovName != null){
                        lovNames.add(lovName);
                    }else{
                        lovNames.add("");
                    }
                }
            }else{
                LOGGER.log(Level.SEVERE, "Can't create attribute for template without a name or a type");
            }
        }

        InstanceAttributeTemplate[] attributesList = attributesTemplates.toArray(new InstanceAttributeTemplate[attributesTemplates.size()]);
        String[] lovNamesList = lovNames.toArray(new String[lovNames.size()]);
        if (templateId != null && !templateId.equalsIgnoreCase("")){
            try {
                dm.createDocumentMasterTemplate(workpaceId,templateId,templateType,null,templateMask,attributesList,lovNamesList,templateIdGeneration,templateAttributesLocked);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Can't create a doc template", e);
            }
        }else{
            LOGGER.log(Level.SEVERE, "Can't create a doc template without a template id");
        }

    }

    private static void createFolders(String workpaceId, JsonArray folders){

        for (int i = 0; i < folders.size() ; i++) {
            String folderName = folders.isNull(i) ? null : folders.getString(i);
            if (folderName != null && ! folderName.equalsIgnoreCase("")) {
                try {
                    dm.createFolder(workpaceId, folderName);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Can't create a Folder : "+folderName, e);
                }
            }else{
                LOGGER.log(Level.SEVERE, "Can't create a Folder without a name");
            }
        }
    } 
    private static void createDocuments(String workpaceId, JsonObject document){

        String docFolder = document.getString(JsonParserConstants.DOCUMENT_FOLDER, null);
        String docID = document.getString(JsonParserConstants.DOCUMENT_ID, null);
        String docTitle = document.getString(JsonParserConstants.DOCUMENT_TITLE, null);
        String docDescription = document.getString(JsonParserConstants.DOCUMENT_DESCRIPTION, null);
        String docTemplateName = document.getString(JsonParserConstants.DOCUMENT_TEMPLATE, null);
        JsonArray documentLinks = document.getJsonArray(JsonParserConstants.DOCUMENT_DOC_LINKED);
        JsonArray docAttributes = document.getJsonArray(JsonParserConstants.DOCUMENT_ATTRIBUTES);

        if (docID != null && !docID.equalsIgnoreCase("")) {

            DocumentRevision docRev = null;
            try {
                String pathToFolder = workpaceId;
                if (docFolder != null && !docFolder.trim().equalsIgnoreCase("")){
                    pathToFolder = workpaceId+"/"+docFolder;
                }
                docRev = dm.createDocumentMaster(pathToFolder,docID,docTitle,docDescription,docTemplateName,null,null,null,null);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Can't create doc master : " + docID, e);
            }

            DocumentIterationKey docIterationKey = new DocumentIterationKey(workpaceId,docID,"A",1);


            ArrayList<InstanceAttribute> attributeList = null;
            //Create InstanceAttribute
            if (docAttributes != null) {
                attributeList = new ArrayList<>();
                for (int i = 0; i < docAttributes.size(); i++) {
                    InstanceAttribute instanceAttribute = UtilsLoader.createInstanceAttribute(docAttributes.getJsonObject(i), workpaceId);
                    if (instanceAttribute != null){
                        attributeList.add(instanceAttribute);
                    }
                }
            }

            //Create DocumentIterationKey[]
            List<DocumentRevisionKey> documentRevisionKeyList = null;
            List<String> documentLinkCommentList = null;
            DocumentRevisionKey[] documentRevisionKeys = null;
            String[] commentList = null;
            if (documentLinks != null) {
                documentRevisionKeyList = new ArrayList<>();
                documentLinkCommentList = new ArrayList<>();
                for (int i = 0; i < documentLinks.size(); i++) {
                    JsonObject documentLinkedJson = documentLinks.getJsonObject(i);
                    String documentId = documentLinkedJson.getString(JsonParserConstants.DOCUMENT_DOCUMENT_LINKS_DOC_ID, null);
                    String comment = documentLinkedJson.getString(JsonParserConstants.DOCUMENT_DOCUMENT_LINKS_COMMENT, "");
                    if (documentId != null && !documentId.equalsIgnoreCase("")){
                        documentRevisionKeyList.add(new DocumentRevisionKey(workpaceId, documentId, "A"));
                        documentLinkCommentList.add(comment);
                    }else{
                        LOGGER.log(Level.SEVERE, "Can't create Document link with empty docID");
                    }
                }
                documentRevisionKeys = documentRevisionKeyList.toArray(new DocumentRevisionKey[documentRevisionKeyList.size()]);
                commentList = documentLinkCommentList.toArray(new String[documentLinkCommentList.size()]);
            }

            try {
                dm.updateDocument(docIterationKey, "", attributeList, documentRevisionKeys, commentList);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Can't create part master : " + docID, e);
            }
        }else{
            LOGGER.log(Level.SEVERE, "Can't create document with empty docID ");
        }


    }
}
