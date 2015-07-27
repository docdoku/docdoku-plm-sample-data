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
package com.docdoku.loaders.products;

import com.docdoku.core.document.DocumentRevisionKey;
import com.docdoku.core.meta.InstanceAttribute;
import com.docdoku.core.meta.InstanceAttributeTemplate;
import com.docdoku.core.product.*;
import com.docdoku.core.services.IProductManagerWS;
import com.docdoku.core.services.IUploadDownloadWS;
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
public class ProductLoader {

    private static final Logger LOGGER = Logger.getLogger(ProductLoader.class.getName());

    private static IProductManagerWS pm;
    private static IUploadDownloadWS fm;

    public static boolean fillWorkspace(String serverURL, String workpaceId, String login, String password, JsonObject partObject) {
        try {

            pm = ScriptingTools.createProductService(serverURL + "/services/product?wsdl", login, password);
            fm = ScriptingTools.createFileManagerService(serverURL + "/services/UploadDownload?wsdl", login, password);

            if (partObject == null){
                throw new Exception("Can't find key '"+JsonParserConstants.PART_PART+"' in the json file");
            }

            boolean result = true;

            JsonArray templates = partObject.getJsonArray(JsonParserConstants.PART_TEMPLATE);
            if (templates != null) {
                for (int i = 0; i < templates.size(); i++) {
                    result = result && createPartTemplate(templates.getJsonObject(i), workpaceId);
                }
            }

            JsonArray partsArray = partObject.getJsonArray(JsonParserConstants.PART_PARTS);
            if (partsArray != null) {
                for (int i = 0; i < partsArray.size(); i++) {
                    JsonObject part = partsArray.getJsonObject(i);
                    result = result && createPart(part, workpaceId);
                }
            }

            JsonArray assembly = partObject.getJsonArray(JsonParserConstants.PART_ASSEMBLY);
            if (assembly!=null) {
                for (int i = 0; i < assembly.size(); i++) {
                    JsonObject partAssembly = assembly.getJsonObject(i);
                    result = result && createAssembly(partAssembly, workpaceId);
                }
            }

            JsonArray productsArray = partObject.getJsonArray(JsonParserConstants.PART_PRODUCT);
            if (productsArray != null) {
                for (int i = 0; i < productsArray.size(); i++) {
                    JsonObject product = productsArray.getJsonObject(i);
                    result = result && createProduct(product, workpaceId);
                }
            }

            return result;
        }catch(Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            return false;
        }
    }

    private static boolean createPartTemplate(JsonObject template, String workpaceId){

        String templateId = template.getString(JsonParserConstants.PART_TEMPLATE_ID, null);
        String templateType = template.getString(JsonParserConstants.PART_TEMPLATE_TYPE, null);
        String templateMask = template.getString(JsonParserConstants.PART_TEMPLATE_MASK, null);
        boolean templateAttributesLocked = template.getBoolean(JsonParserConstants.PART_TEMPLATE_ATTRIBUTE_LOCKED, false);
        boolean templateIdGeneration = template.getBoolean(JsonParserConstants.PART_TEMPLATE_ID_GENERATION, false);
        JsonArray attributes = template.getJsonArray(JsonParserConstants.PART_TEMPLATE_ATTRIBUTES);


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

        if (templateId != null && !templateId.equalsIgnoreCase("")) {
            try {
                pm.createPartMasterTemplate(workpaceId, templateId, templateType, null, templateMask, attributesList, lovNamesList, new InstanceAttributeTemplate[0], new String[0], templateIdGeneration, templateAttributesLocked);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Can't create a part template", e);
            }

        } else {
            LOGGER.log(Level.SEVERE, "Can't create a part template without a template id");
        }

        return true;
    }

    private static boolean createPart(JsonObject part, String workpaceId){

        String partNumber = part.getString(JsonParserConstants.PART_NUMBER, null);
        String partName = part.getString(JsonParserConstants.PART_NAME, null);
        String partDescription = part.getString(JsonParserConstants.PART_DESCRIPTION, null);
        boolean isStandardPart = part.getBoolean(JsonParserConstants.PART_IS_STANDARD_PART, false);
        String partTemplateName = part.getString(JsonParserConstants.PART_TEMPLATE_NAME, null);
        JsonArray documentLinks = part.getJsonArray(JsonParserConstants.PART_DOCUMENT_LINKS);
        JsonArray partAttributes = part.getJsonArray(JsonParserConstants.PART_ATTRIBUTES);

        if (partNumber != null && !partNumber.equalsIgnoreCase("")) {
            try {
                pm.createPartMaster(workpaceId, partNumber, partName, isStandardPart, null, partDescription, partTemplateName, null, null, null);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Can't create part master : " + partNumber, e);
                return true;
            }

            /**
             * Creation of the iteration
             */
            PartIterationKey partIterationKey = new PartIterationKey(workpaceId,partNumber,"A",1);
            PartIteration.Source source = PartIteration.Source.MAKE;

            ArrayList<InstanceAttribute> attributList = null;
            //Create InstanceAtribute
            if (partAttributes != null) {
                attributList = new ArrayList<>();
                for (int i = 0; i < partAttributes.size(); i++) {
                    InstanceAttribute instanceAttribute = UtilsLoader.createInstanceAttribute(partAttributes.getJsonObject(i), workpaceId);
                    if (instanceAttribute != null){
                        attributList.add(instanceAttribute);
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
                    String documentId = documentLinkedJson.getString(JsonParserConstants.PART_DOCUMENT_LINKS_DOC_ID, null);
                    String comment = documentLinkedJson.getString(JsonParserConstants.PART_DOCUMENT_LINKS_COMMENT, "");
                    if (documentId != null && !documentId.equalsIgnoreCase("")){
                        documentRevisionKeyList.add(new DocumentRevisionKey(workpaceId, documentId, "A"));
                        documentLinkCommentList.add(comment);
                    } else {
                        LOGGER.log(Level.SEVERE, "Can't create Document link with empty docID");
                    }
                }
                documentRevisionKeys = documentRevisionKeyList.toArray(new DocumentRevisionKey[documentRevisionKeyList.size()]);
                commentList = documentLinkCommentList.toArray(new String[documentLinkCommentList.size()]);
            }

            try {
                pm.updatePartIteration(partIterationKey, "", source, null, attributList, null, documentRevisionKeys, commentList, null);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Can't create part master : " + partNumber, e);
            }

        } else {
            LOGGER.log(Level.SEVERE, "Can't create part master with empty part number");
        }

        return true;
    }

    private static boolean createAssembly(JsonObject assembly, String workpaceId) {
        String rootPart = assembly.getString(JsonParserConstants.ASSEMBLY_ROOT_PART_NUMBER, null);
        JsonArray linkedParts = assembly.getJsonArray(JsonParserConstants.ASSEMBLY_PARTS);

        //Check if the root part exist and retrieve it
        PartIteration partIteration = null;
        if (rootPart != null && !rootPart.equalsIgnoreCase("")){
            try {
                List<PartMaster> listOfPArtMAster = pm.findPartMasters(workpaceId, rootPart, rootPart, 1);
                if (listOfPArtMAster != null && listOfPArtMAster.size()>0){
                    PartMaster partMaster = listOfPArtMAster.get(0);
                    partIteration = partMaster.getLastRevision().getLastIteration();
                }

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Can't can't find root part with part number : "+rootPart, e);
            }
        }else{
            LOGGER.log(Level.SEVERE, "Can't create assembly with no root part");
        }

        if (partIteration != null) {
            List<PartUsageLink> partUsageLinks = null;
            if (linkedParts != null && linkedParts.size() != 0) {
                partUsageLinks = getPartUsageLinks(workpaceId, linkedParts);
            } else {
                LOGGER.log(Level.SEVERE, "Can't create assembly with no sub part");
            }

            try {
                pm.updatePartIteration(new PartIterationKey(workpaceId, rootPart,"A",1),"", PartIteration.Source.MAKE,partUsageLinks,partIteration.getInstanceAttributes(),null,null,null,null);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Can't create assembly for part with part number : " + rootPart, e);
            }
        }else{
            LOGGER.log(Level.SEVERE, "Can't find last iteration of root part with part number : "+rootPart);
        }

        return true;
    }

    private static boolean createProduct(JsonObject product, String workpaceId){

        String productName = product.getString(JsonParserConstants.PRODUCT_NAME, null);
        String description = product.getString(JsonParserConstants.PRODUCT_DESCRIPTION, null);
        String rootPart = product.getString(JsonParserConstants.PRODUCT_ROOT_PART, null);

        if (productName != null && !productName.equalsIgnoreCase("")){
            if (rootPart != null && !rootPart.equalsIgnoreCase("")){
                try {
                    pm.createConfigurationItem(workpaceId,productName,description,rootPart);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Can't create a product", e);
                }
            }else{
                LOGGER.log(Level.SEVERE, "Can't create a product without a root part name");
            }

        }else{
            LOGGER.log(Level.SEVERE, "Can't create a product without a name");
        }


        return true;
    }

    private static PartMaster getPartMasterWithPartNumber(String partNumber, String workspaceId){
        try {
            List<PartMaster> listOfPArtMAster = pm.findPartMasters(workspaceId, partNumber, partNumber, 1);
            if (listOfPArtMAster != null && listOfPArtMAster.size()>0){
                return listOfPArtMAster.get(0);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Can't can't find root part with part number : "+partNumber, e);
        }

        return null;
    }

    private static List<PartSubstituteLink> getPartSubstituteLinks(String workspaceId, JsonObject subPart) {

        JsonArray substitutes = subPart.getJsonArray(JsonParserConstants.ASSEMBLY_SUBSTITUTE);
        List<PartSubstituteLink> partSubstitutes = null;
        if (substitutes != null && substitutes.size() > 0) {
            partSubstitutes = new ArrayList<>();
            for (int j = 0; j < substitutes.size(); j++) {
                JsonObject substituteJsonObject = substitutes.getJsonObject(j);
                String substitutePartNumber = substituteJsonObject.getString(JsonParserConstants.ASSEMBLY_SUBSTITUTE_PART_NUMBER, null);
                double substituteAmount = substituteJsonObject.getJsonNumber(JsonParserConstants.ASSEMBLY_SUBSTITUTE_AMOUNT).doubleValue();
                String substituteUnit = substituteJsonObject.getString(JsonParserConstants.ASSEMBLY_SUBSTITUTE_UNIT, null);
                JsonArray substituteCadInstances = subPart.getJsonArray(JsonParserConstants.ASSEMBLY_CAD_INSTANCES);

                if (substituteAmount == substituteCadInstances.size()) {
                    List<CADInstance> cadInstanceList = new ArrayList<>();
                    for (int i=0; i<substituteCadInstances.size(); i++) {
                        JsonObject cadInstance = (JsonObject) substituteCadInstances.get(i);

                        double rx = cadInstance.getJsonNumber(JsonParserConstants.ASSEMBLY_CAD_INSTANCES_RX).doubleValue();
                        double ry = cadInstance.getJsonNumber(JsonParserConstants.ASSEMBLY_CAD_INSTANCES_RY).doubleValue();
                        double rz = cadInstance.getJsonNumber(JsonParserConstants.ASSEMBLY_CAD_INSTANCES_RZ).doubleValue();
                        double tx = cadInstance.getJsonNumber(JsonParserConstants.ASSEMBLY_CAD_INSTANCES_TX).doubleValue();
                        double ty = cadInstance.getJsonNumber(JsonParserConstants.ASSEMBLY_CAD_INSTANCES_TY).doubleValue();
                        double tz = cadInstance.getJsonNumber(JsonParserConstants.ASSEMBLY_CAD_INSTANCES_TZ).doubleValue();

                        CADInstance cadInstancei = new CADInstance(tx, ty, tz, rx, ry, rz);
                        cadInstanceList.add(cadInstancei);
                    }

                    PartMaster substitutePartMaster = getPartMasterWithPartNumber(substitutePartNumber, workspaceId);
                    if (substitutePartMaster != null) {
                        PartSubstituteLink substitute = new PartSubstituteLink();
                        substitute.setSubstitute(substitutePartMaster);
                        substitute.setAmount(substituteAmount);
                        substitute.setUnit(substituteUnit);
                        substitute.setCadInstances(cadInstanceList);
                        partSubstitutes.add(substitute);
                    } else {
                        LOGGER.log(Level.SEVERE, "(Substitute) Can't find part master for part number : "+substitutePartNumber);
                    }

                } else {
                    LOGGER.log(Level.SEVERE, "(Substitute) Can't find enough cad instances for part number : "+substitutePartNumber);
                }
            }
        } return partSubstitutes;
    }

    private static List<PartUsageLink> getPartUsageLinks(String workpaceId, JsonArray linkedParts) {
        List<PartUsageLink> partUsageLinks = new ArrayList<>();
        for (int i = 0; i < linkedParts.size(); i++) {
            JsonObject subPart = linkedParts.getJsonObject(i);
            String subPartNumber = subPart.getString(JsonParserConstants.ASSEMBLY_PART_PART_NUMBER, null);
            boolean isOptional = subPart.getBoolean(JsonParserConstants.ASSEMBLY_PART_IS_OPTIONAL, false);
            double amount = subPart.getJsonNumber(JsonParserConstants.ASSEMBLY_PART_AMOUNT).doubleValue();
            String unit = subPart.getString(JsonParserConstants.ASSEMBLY_PART_UNIT, null);
            JsonArray cadInstances = subPart.getJsonArray(JsonParserConstants.ASSEMBLY_CAD_INSTANCES);

            if (amount == cadInstances.size()) {
                List<CADInstance> cadInstanceList = new ArrayList<>();
                for (int j=0; j<cadInstances.size(); j++) {
                    JsonObject cadInstance = (JsonObject) cadInstances.get(j);

                    double rx = cadInstance.getJsonNumber(JsonParserConstants.ASSEMBLY_CAD_INSTANCES_RX).doubleValue();
                    double ry = cadInstance.getJsonNumber(JsonParserConstants.ASSEMBLY_CAD_INSTANCES_RY).doubleValue();
                    double rz = cadInstance.getJsonNumber(JsonParserConstants.ASSEMBLY_CAD_INSTANCES_RZ).doubleValue();
                    double tx = cadInstance.getJsonNumber(JsonParserConstants.ASSEMBLY_CAD_INSTANCES_TX).doubleValue();
                    double ty = cadInstance.getJsonNumber(JsonParserConstants.ASSEMBLY_CAD_INSTANCES_TY).doubleValue();
                    double tz = cadInstance.getJsonNumber(JsonParserConstants.ASSEMBLY_CAD_INSTANCES_TZ).doubleValue();

                    CADInstance cadInstancej = new CADInstance(tx, ty, tz, rx, ry, rz);
                    cadInstanceList.add(cadInstancej);
                }

                PartMaster subPartMaster = getPartMasterWithPartNumber(subPartNumber, workpaceId);
                if (subPartMaster != null) {
                    PartUsageLink usageLink = new PartUsageLink(subPartMaster, amount, unit, isOptional);
                    usageLink.setCadInstances(cadInstanceList);
                    List<PartSubstituteLink> partSubstitutes = getPartSubstituteLinks(workpaceId, subPart);
                    usageLink.setSubstitutes(partSubstitutes);
                    partUsageLinks.add(usageLink);
                } else {
                    LOGGER.log(Level.SEVERE, "(SubPart) Can't find part master for part number : "+subPartMaster);
                }

            } else {
                LOGGER.log(Level.SEVERE, "(SubPart) Can't find enough cad instances for part number : "+subPartNumber);
            }
        }

        return partUsageLinks;
    }
}
