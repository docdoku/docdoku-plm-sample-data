package com.docdoku.loaders.utils;

/**
 * Created by lebeaujulien on 23/03/15.
 */
public class JsonParserConstants {


    /****************************************************************************
     *
     *
     *                          PART
     *
     *
     *****************************************************************************/
    public static final String PART_PART = "PART";
    public static final String PART_TEMPLATE = "templates";
    public static final String PART_PARTS = "parts";
    public static final String PART_ASSEMBLY = "assembly";
    public static final String PART_PRODUCT = "products";

    /**
     * Part
     */
    public static final String PART_NUMBER = "partNumber";
    public static final String PART_NAME = "partName";
    public static final String PART_DESCRIPTION = "partDescription";
    public static final String PART_IS_STANDARD_PART = "isStandardPart";
    public static final String PART_TEMPLATE_NAME = "partTemplate";
    public static final String PART_DOCUMENT_LINKS = "documentLinks";
    public static final String PART_DOCUMENT_LINKS_DOC_ID = "docId";
    public static final String PART_DOCUMENT_LINKS_COMMENT = "comment";
    public static final String PART_ATTRIBUTES = "attributes";

    /**
     * Template
     */
    public static final String PART_TEMPLATE_ID = "id";
    public static final String PART_TEMPLATE_TYPE = "type";
    public static final String PART_TEMPLATE_MASK = "mask";
    public static final String PART_TEMPLATE_ATTRIBUTE_LOCKED = "attributesLocked";
    public static final String PART_TEMPLATE_ID_GENERATION = "idGeneration";
    public static final String PART_TEMPLATE_ATTRIBUTES = "attributes";

    /**
     * Assembly
     */
    public static final String ASSEMBLY_ROOT_PART_NUMBER = "partNumber";
    public static final String ASSEMBLY_PARTS = "parts";
    public static final String ASSEMBLY_PART_PART_NUMBER = "partNumber";
    public static final String ASSEMBLY_PART_IS_OPTIONAL = "optional";
    public static final String ASSEMBLY_PART_AMOUNT = "amount";
    public static final String ASSEMBLY_PART_UNIT = "unit";
    public static final String ASSEMBLY_SUBSTITUTE = "substitute";
    public static final String ASSEMBLY_SUBSTITUTE_PART_NUMBER = "partNumber";
    public static final String ASSEMBLY_SUBSTITUTE_AMOUNT = "amount";
    public static final String ASSEMBLY_SUBSTITUTE_UNIT = "unit";
    public static final String ASSEMBLY_CAD_INSTANCES = "cadInstances";
    public static final String ASSEMBLY_CAD_INSTANCES_RX = "rx";
    public static final String ASSEMBLY_CAD_INSTANCES_RY = "ry";
    public static final String ASSEMBLY_CAD_INSTANCES_RZ = "rz";
    public static final String ASSEMBLY_CAD_INSTANCES_TX = "tx";
    public static final String ASSEMBLY_CAD_INSTANCES_TY = "ty";
    public static final String ASSEMBLY_CAD_INSTANCES_TZ = "tz";

    /**
     * Product
     */
    public static final String PRODUCT_NAME = "name";
    public static final String PRODUCT_DESCRIPTION = "description";
    public static final String PRODUCT_ROOT_PART = "rootPart";



    /****************************************************************************
     *
     *
     *                          DOC
     *
     *
     *****************************************************************************/

    public static final String DOC = "DOC";
    public static final String DOC_FOLDERS = "folders";
    public static final String DOC_TEMPLATE = "templates";
    public static final String DOC_DOCUMENTS = "documents";

    /**
     * Template
     */
    public static final String DOC_TEMPLATE_ID = "id";
    public static final String DOC_TEMPLATE_MASK = "mask";
    public static final String DOC_TEMPLATE_TYPE = "type";
    public static final String DOC_TEMPLATE_ATTRIBUTE_LOCKED = "attributesLocked";
    public static final String DOC_TEMPLATE_ATTRIBUTES = "attributes";
    public static final String DOC_TEMPLATE_ID_GENERATION = "idGeneration";

    /**
     * Doc
     */
    public static final String DOCUMENT_FOLDER = "folder";
    public static final String DOCUMENT_ID = "docID";
    public static final String DOCUMENT_TEMPLATE = "docTemplate";
    public static final String DOCUMENT_TITLE = "docTitle";
    public static final String DOCUMENT_DESCRIPTION = "docDescription";
    public static final String DOCUMENT_DOC_LINKED = "documentLinks";
    public static final String DOCUMENT_ATTRIBUTES = "attributes";
    public static final String DOCUMENT_DOCUMENT_LINKS_DOC_ID = "docId";
    public static final String DOCUMENT_DOCUMENT_LINKS_COMMENT = "comment";

    /****************************************************************************
     *
     *
     *                          LOV
     *
     *
     *****************************************************************************/

    public static final String LOV = "LOV";
    public static final String LOV_LOV_NAME = "lovName";
    public static final String LOV_LOV_VALUES = "possibleValues";
    public static final String LOV_NAME = "name";
    public static final String LOV_VALUE = "value";

    /****************************************************************************
     *
     *
     *                          Attributes
     *
     *
     *****************************************************************************/
    public static final String ATTRIBUTE_NAME = "name";
    public static final String ATTRIBUTE_TYPE = "type";
    public static final String ATTRIBUTE_VALUE = "value";
    public static final String ATTRIBUTE_MANDATORY = "mandatory";
    public static final String ATTRIBUTE_LOV_NAME = "lovName";
}
