package com.docdoku.loaders.common;

import com.docdoku.core.exceptions.*;
import com.docdoku.core.meta.ListOfValues;
import com.docdoku.core.meta.ListOfValuesKey;
import com.docdoku.core.meta.NameValuePair;
import com.docdoku.core.services.ILOVManagerWS;
import com.docdoku.loaders.tools.ScriptingTools;
import com.docdoku.loaders.utils.JsonParserConstants;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by lebeaujulien on 25/03/15.
 */
public class LOVLoader {

    private static final Logger LOGGER = Logger.getLogger(LOVLoader.class.getName());

    private static ILOVManagerWS lm;

    public static boolean fillWorkspace(String serverURL, String workpaceId, String login, String password, JsonArray lovArray){
        try {
            lm = ScriptingTools.createLOVService(serverURL + "/services/lov?wsdl", login, password);

            if (lovArray != null) {
                for (int i = 0; i < lovArray.size(); i++) {
                    createLov(workpaceId, lovArray.getJsonObject(i));
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            return false;
        }
        return true;
    }

    private static void createLov(String workspaceId, JsonObject lovObject){

        String lovName = lovObject.getString(JsonParserConstants.LOV_LOV_NAME, null);

        if (lovName != null && !lovName.equalsIgnoreCase("")) {
            JsonArray lovPossibleValues = lovObject.getJsonArray(JsonParserConstants.LOV_LOV_VALUES);
            if (lovPossibleValues!=null){
                List<NameValuePair> possibleValues = new ArrayList<>();
                for (int i = 0; i < lovPossibleValues.size(); i++) {
                    JsonObject possibleValueObject = lovPossibleValues.getJsonObject(i);
                    String name = possibleValueObject.getString(JsonParserConstants.LOV_NAME, null);
                    String value = possibleValueObject.getString(JsonParserConstants.LOV_VALUE, "");

                    if (name != null && !name.equalsIgnoreCase("")){
                        possibleValues.add(new NameValuePair(name, value));
                    }else{
                        LOGGER.log(Level.SEVERE, "Can't create LOV with possible values with empty name or no name");
                    }
                }

                try {
                    lm.createLov(workspaceId, lovName, possibleValues);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Can't create LOV", e);
                }

            }else{
                LOGGER.log(Level.SEVERE, "Can't create LOV without possible values");
            }
        }else{
            LOGGER.log(Level.SEVERE, "Can't create LOV without a LOV name or empty name");
        }
    }

    public static ListOfValues getLov(String workspaceId, String lovName){

        try {
            ListOfValuesKey lovKey = new ListOfValuesKey(workspaceId, lovName);
            ListOfValues lov = lm.findLov(lovKey);
            return lov;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Can't find LOV with name : "+lovName+" in workspace "+workspaceId+"", e);
            return null;
        }
    }

}
