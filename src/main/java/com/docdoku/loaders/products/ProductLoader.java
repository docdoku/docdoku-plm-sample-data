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

import com.docdoku.core.meta.InstanceAttribute;
import com.docdoku.core.meta.InstanceNumberAttribute;
import com.docdoku.core.product.*;
import com.docdoku.core.services.IProductManagerWS;
import com.docdoku.core.services.IUploadDownloadWS;
import com.docdoku.loaders.tools.ScriptingTools;

import javax.activation.DataHandler;
import java.net.URL;
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

    public static boolean fillWorkspace(String serverURL, String workpaceId, String login, String password) {
        try {

            pm = ScriptingTools.createProductService(serverURL + "/services/product?wsdl", login, password);
            fm = ScriptingTools.createFileManagerService(serverURL + "/services/UploadDownload?wsdl", login, password);

            createBikeSampleProduct(workpaceId);
            return true;
        }catch(Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            return false;
        }
    }
    
    
    private static boolean createBikeSampleProduct(String workspace) {
        
        final String createBikeSampleProductFail = "The Bike creation fail.\n";

        try {
            pm.createPartMaster(workspace, "BMX", "BMX", false, null, "created by loader", null, null, null, null);
            pm.createConfigurationItem(workspace, "Bike", "Bicycle Motocross", "BMX");

            PartMaster componentM = pm.createPartMaster(workspace, "BPM12VTX", "", false, null, "", null, null, null, null);
            List<PartUsageLink> subParts = new ArrayList<>();
            PartUsageLink link = new PartUsageLink();
            link.setAmount(1);
            link.setComponent(componentM);
            List<CADInstance> cads = new ArrayList<>();
            cads.add(new CADInstance(0D, 0D, 0D, 0D, 0D, 0D));
            link.setCadInstances(cads);
            subParts.add(link);

            pm.updatePartIteration(new PartIterationKey(new PartRevisionKey(new PartMasterKey(workspace, "BMX"), "A"), 1), "created by loader", PartIteration.Source.MAKE, subParts, null, null);

            List<InstanceAttribute> attrs = new ArrayList<>();
            InstanceNumberAttribute instanceAttribute = new InstanceNumberAttribute("radius", 9000000000f, false);
            attrs.add(instanceAttribute);
            pm.updatePartIteration(new PartIterationKey(new PartRevisionKey(new PartMasterKey(workspace, "BPM12VTX"), "A"), 1), "created by loader", PartIteration.Source.MAKE, null, attrs, null);


            URL jsonURL = ProductLoader.class.getResource("/com/docdoku/loaders/bike.js");
            URL binURL = ProductLoader.class.getResource("/com/docdoku/loaders/bike.bin");

            DataHandler dh = new DataHandler(jsonURL);
            fm.uploadGeometryToPart(workspace, "BPM12VTX", "A", 1, "bike.js", 0, dh);

            dh = new DataHandler(binURL);
            fm.uploadToPart(workspace, "BPM12VTX", "A", 1, "bike.bin", dh);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, createBikeSampleProductFail + "Forbidden Exception provide by DocdokuPLM", e);
            return false;
        } 
    }
}
