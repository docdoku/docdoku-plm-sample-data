package com.docdoku.loaders.tools;

import com.docdoku.core.meta.*;
import com.docdoku.loaders.common.LOVLoader;
import com.docdoku.loaders.utils.JsonParserConstants;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by lebeaujulien on 25/03/15.
 */
public class UtilsLoader {

    private static final Logger LOGGER = Logger.getLogger(UtilsLoader.class.getName());

    public static InstanceAttributeTemplate getInstanceAttributeTemplate(String name, String type, boolean mandatory, String lovName, String workspaceId) {

        InstanceAttributeTemplate attr = null;
        if (type.equalsIgnoreCase("LOV")){
            if (lovName != null && !lovName.equalsIgnoreCase("")){
                ListOfValues lov = LOVLoader.getLov(workspaceId, lovName);
                if (lov != null){
                    attr = new ListOfValuesAttributeTemplate(name,lov);
                }
            }else{
                LOGGER.log(Level.SEVERE, "Can't create LOV attribute for template without a lovname");
            }
        }else {
            attr = new DefaultAttributeTemplate();
            switch (type) {
                case "BOOLEAN":
                    ((DefaultAttributeTemplate)attr).setAttributeType(InstanceAttributeTemplate.AttributeType.BOOLEAN);
                    break;
                case "TEXT":
                    ((DefaultAttributeTemplate)attr).setAttributeType(InstanceAttributeTemplate.AttributeType.TEXT);
                    break;
                case "NUMBER":
                    ((DefaultAttributeTemplate)attr).setAttributeType(InstanceAttributeTemplate.AttributeType.NUMBER);
                    break;
                case "DATE":
                    ((DefaultAttributeTemplate)attr).setAttributeType(InstanceAttributeTemplate.AttributeType.DATE);
                    break;
                case "URL":
                    ((DefaultAttributeTemplate)attr).setAttributeType(InstanceAttributeTemplate.AttributeType.URL);
                    break;
                default:
                    ((DefaultAttributeTemplate)attr).setAttributeType(InstanceAttributeTemplate.AttributeType.TEXT);
            }
        }

        attr.setName(name);
        attr.setMandatory(mandatory);

        return attr;
    }

    public static InstanceAttribute createInstanceAttribute(JsonObject attribute, String workspaceId){
        String type = attribute.getString(JsonParserConstants.ATTRIBUTE_TYPE, "TEXT");
        String name = attribute.getString(JsonParserConstants.ATTRIBUTE_NAME, null);
        String lovName = attribute.getString(JsonParserConstants.ATTRIBUTE_LOV_NAME, null);

        InstanceAttribute attr;
        switch (type) {
            case "BOOLEAN":
                attr = new InstanceBooleanAttribute();
                boolean boolValue = attribute.getBoolean(JsonParserConstants.ATTRIBUTE_VALUE, false);
                attr.setValue(boolValue);
                break;
            case "TEXT":
                attr = new InstanceTextAttribute();
                String textValue = attribute.getString(JsonParserConstants.ATTRIBUTE_VALUE, null);
                attr.setValue(textValue);
                break;
            case "NUMBER":
                attr = new InstanceNumberAttribute();
                String numberValue = attribute.getString(JsonParserConstants.ATTRIBUTE_VALUE, null);
                attr.setValue(numberValue);
                break;
            case "DATE":
                attr = new InstanceDateAttribute();
                String dateValue = attribute.getString(JsonParserConstants.ATTRIBUTE_VALUE, null);
                attr.setValue(dateValue);
                break;
            case "URL":
                attr = new InstanceURLAttribute();
                String urlValue = attribute.getString(JsonParserConstants.ATTRIBUTE_VALUE, null);
                attr.setValue(urlValue);
                break;
            case "LOV":
                InstanceListOfValuesAttribute lovAttribute = new InstanceListOfValuesAttribute();

                ListOfValues lov = LOVLoader.getLov(workspaceId, lovName);
                if (lov != null) {
                    lovAttribute.setItems(lov.getValues());
                    int selectedIndex = attribute.getJsonNumber(JsonParserConstants.ATTRIBUTE_VALUE).intValue();
                    lovAttribute.setValue(selectedIndex);
                }else{
                    return null;
                }


                attr = lovAttribute;
                break;
            default:
                throw new IllegalArgumentException("Instance attribute not supported");
        }

        attr.setName(name);
        return attr;

    }
}
