/*
 * DocDoku, Professional Open Source
 * Copyright 2006 - 2017 DocDoku SARL
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

import com.docdoku.api.DocDokuPLMClientFactory;
import com.docdoku.api.client.ApiClient;
import com.docdoku.api.client.ApiException;
import com.docdoku.api.models.*;
import com.docdoku.api.models.utils.AttributesHelper;
import com.docdoku.api.models.utils.LastIterationHelper;
import com.docdoku.api.models.utils.WorkflowHelper;
import com.docdoku.api.services.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class uses DocDokuPLM Java API to load sample data to given server url and workspace
 *
 * @author Morgan GUIMARD
 */
public class SampleLoader {

    private static final Logger LOGGER = SampleLoaderLogger.getLOGGER();
    private static final boolean debug = false;

    private String login;
    private String password;
    private String workspaceId;
    private String url;

    private ApiClient client;
    private ApiClient guestClient;

    private String[] logins = {"rob", "joe", "steve","mickey","bill","rendal","winie","titi","toto","tata"};
    private final String group1 = "Group1";
    private final String group2 = "Group2";
    private final String group3 = "Group3";
    private final String group4 = "Group4";
    private final String group5 = "Group5";

    public SampleLoader(String login, String password, String workspaceId, String url) {
        this.login = login;
        this.password = password;
        this.workspaceId = workspaceId;
        this.url = url;
        guestClient = DocDokuPLMClientFactory.createClient(url);
        client = DocDokuPLMClientFactory.createJWTClient(url, login, password);
    }

    public void load() throws ApiException, IOException, InterruptedException {

        LOGGER.info("Starting load process ... ");

        checkServerAvailability();

        try {
            createCallerAccount();
        } catch (ApiException e) {
            LOGGER.log(Level.INFO, "Cannot create account, trying to use given credentials for next operations");
        }

        createWorkspace();
        createOtherAccounts();
        createGroups();
        setAccessPermissionForGroups();
        enableUserInworkSpace();
        setAccessPermissionsForUser();

        createMilestones();
        setMilestoneAcl();
        createRolesAndWorkflow();
        createRolesAndWorkflowForDoorProduct();
        setWorkFlowACL();

        createDocumentTemplates();
        createFolders();
        createTags();
        createDocuments();

        createPartTemplates();
        createParts();
        createProducts();
        createDoorProduct();
        createConfiguration();
        createBaseline();
        createProductInstance();


        createRequests();
        createIssues();
        //setIssuesAcl();
        createOrders();

        checkoutParts();

    }

    private void checkServerAvailability() throws ApiException {
        LOGGER.log(Level.INFO, "Checking server availability ...");
        new LanguagesApi(guestClient).getLanguages();
    }

    private void createCallerAccount() throws ApiException {
        LOGGER.log(Level.INFO, "Creating your account ...");
        createAccount(login);
    }

    private void createOtherAccounts() throws ApiException {
        LOGGER.log(Level.INFO, "Creating accounts");
        for (String pLogin : logins) {
            try {
                createAccount(pLogin);
            } catch (ApiException e) {
                LOGGER.log(Level.INFO, "Cannot create account for " + pLogin + ", might already exist");
            }
            addUserToWorkspace(pLogin);
        }
    }

    private void createGroups() throws ApiException {
        LOGGER.log(Level.INFO, "Creating groups ...");
        UserGroupDTO group = new UserGroupDTO();

        group.setWorkspaceId(workspaceId);

        group.setId(group1);
        WorkspacesApi workspacesApi = new WorkspacesApi(client);
        workspacesApi.createGroup(workspaceId, group);

        group.setId(group2);
        workspacesApi.createGroup(workspaceId, group);

        group.setId(group3);
        workspacesApi.createGroup(workspaceId, group);

        group.setId(group4);
        workspacesApi.createGroup(workspaceId, group);

        group.setId(group5);
        workspacesApi.createGroup(workspaceId, group);

        int numGroup = 1;
        String groupName = "Group"+numGroup;

        for(int i = 0; i < logins.length;i++){

            UserDTO userDTO = new UserDTO();
            userDTO.setLogin(logins[i]);
            if(i%2 != 0){//restricted to only two user by group ( one read-only and one full access )

                userDTO.setMembership(UserDTO.MembershipEnum.READ_ONLY);
                workspacesApi.addUser(workspaceId,userDTO,groupName);
                numGroup+=1;
                groupName = "Group"+numGroup;
            }else{

                userDTO.setMembership(UserDTO.MembershipEnum.FULL_ACCESS);
                workspacesApi.addUser(workspaceId,userDTO,groupName);
            }
        }
        UserDTO userDTO = new UserDTO();
        userDTO.setLogin(login);
        userDTO.setMembership(UserDTO.MembershipEnum.FULL_ACCESS);
        workspacesApi.addUser(workspaceId,userDTO,group1);
    }

    private void enableUserInworkSpace() throws ApiException {

        LOGGER.log(Level.INFO, "enable user in workspace...");
        UserDTO userDTO = new UserDTO();
        WorkspacesApi wksApi = new WorkspacesApi(client);
        for(int i=0;i < logins.length;i++){
            userDTO.setLogin(logins[i]);
            wksApi.enableUser(workspaceId, userDTO);
        }
        userDTO.setLogin(login);
        wksApi.enableUser(workspaceId, userDTO);
    }

    private void setAccessPermissionsForUser() throws ApiException {

        LOGGER.log(Level.INFO, "Setting the access permissions of User ...");
        WorkspacesApi wksApi = new WorkspacesApi(client);
        for(int i=0;i < logins.length;i++){

            if(i%2 != 0){

                UserDTO userDTO = new UserDTO();
                userDTO.setLogin(logins[i]);
                userDTO.setMembership(UserDTO.MembershipEnum.READ_ONLY);
                wksApi.setUserAccess(workspaceId,userDTO);
            }
        }

    }

    private void setAccessPermissionForGroups() throws ApiException {

        LOGGER.log(Level.INFO, "Setting the access permissions of groups ...");
        WorkspaceUserGroupMemberShipDTO wksGrpMemberShipDTO = new WorkspaceUserGroupMemberShipDTO();
        WorkspacesApi wksApi = new WorkspacesApi(client);

        wksGrpMemberShipDTO.setWorkspaceId(workspaceId);
        wksGrpMemberShipDTO.setMemberId(group1);
        wksGrpMemberShipDTO.setReadOnly(false);
        wksApi.setGroupAccess(workspaceId, wksGrpMemberShipDTO);

        wksGrpMemberShipDTO.setMemberId(group2);
        wksGrpMemberShipDTO.setReadOnly(false);
        wksApi.setGroupAccess(workspaceId, wksGrpMemberShipDTO);

        wksGrpMemberShipDTO.setMemberId(group3);
        wksGrpMemberShipDTO.setReadOnly(true);
        wksApi.setGroupAccess(workspaceId, wksGrpMemberShipDTO);

        wksGrpMemberShipDTO.setMemberId(group4);
        wksGrpMemberShipDTO.setReadOnly(true);
        wksApi.setGroupAccess(workspaceId, wksGrpMemberShipDTO);

        wksGrpMemberShipDTO.setMemberId(group5);
        wksGrpMemberShipDTO.setReadOnly(true);
        wksApi.setGroupAccess(workspaceId, wksGrpMemberShipDTO);
    }

    private void createAccount(String accountLogin) throws ApiException {
        LOGGER.log(Level.INFO, "Creating account [" + accountLogin + "]");
        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setName(accountLogin);
        try {
            accountDTO.setEmail(login + "@" + SampleLoaderUtils.getDomainName(url));
        } catch (URISyntaxException e) {
            LOGGER.log(Level.WARNING, "Cannot parse domain from url, using docdoku.com as domain for email");
            accountDTO.setEmail(login + "@docdoku.com");
        }
        accountDTO.setTimeZone("CET");
        accountDTO.setLanguage("en");
        accountDTO.setLogin(accountLogin);
        accountDTO.setNewPassword(password);
        new AccountsApi(guestClient).createAccount(accountDTO);
        addUserToWorkspace(login);

    }

    private void addUserToWorkspace(String pLogin) throws ApiException {
        LOGGER.log(Level.INFO, "Adding user " + pLogin + " to workspace " + workspaceId);
        UserDTO userDTO = new UserDTO();
        userDTO.setLogin(pLogin);
        new WorkspacesApi(client).addUser(workspaceId, userDTO, null);
    }

    private void createWorkspace() throws ApiException {
        LOGGER.log(Level.INFO, "Creating workspace ...");
        WorkspaceDTO workspaceDTO = new WorkspaceDTO();
        workspaceDTO.setId(workspaceId);
        workspaceDTO.setDescription("Some workspaceId created from sample loader");
        new WorkspacesApi(client).createWorkspace(workspaceDTO, login);
    }


    private void createDocumentTemplates() throws ApiException {
        LOGGER.log(Level.INFO, "Creating document templates ...");
        DocumentTemplateCreationDTO template = new DocumentTemplateCreationDTO();

        template.setWorkspaceId(workspaceId);
        template.setReference("Letter");
        template.setDocumentType("Paper");
        template.setMask("LETTER-###");
        new DocumentTemplatesApi(client).createDocumentMasterTemplate(workspaceId, template);

        template.setReference("Invoice");
        template.setDocumentType("Paper");
        template.setMask("INVOICE-###");
        new DocumentTemplatesApi(client).createDocumentMasterTemplate(workspaceId, template);

        template.setReference("UserManuals");
        template.setDocumentType("Documentation");
        template.setMask("USER-MAN-###");
        new DocumentTemplatesApi(client).createDocumentMasterTemplate(workspaceId, template);

        template.setReference("APIDocuments");
        template.setDocumentType("APIManuals");
        template.setMask("API-###");
        new DocumentTemplatesApi(client).createDocumentMasterTemplate(workspaceId, template);
    }

    private void createFolders() throws ApiException {
        LOGGER.log(Level.INFO, "Creating folders ...");
        FolderDTO folderDTO = new FolderDTO();
        folderDTO.setName("Letters");
        new FoldersApi(client).createSubFolder(workspaceId, workspaceId, folderDTO);
        folderDTO.setName("Invoices");
        new FoldersApi(client).createSubFolder(workspaceId, workspaceId, folderDTO);
        folderDTO.setName("Documentation");
        new FoldersApi(client).createSubFolder(workspaceId, workspaceId, folderDTO);

        folderDTO.setName("APIManuals");
        new FoldersApi(client).createSubFolder(workspaceId, workspaceId, folderDTO);
    }

    private void createTags() throws ApiException {
        LOGGER.log(Level.INFO, "Creating tags ...");
        TagListDTO tagListDTO = new TagListDTO();
        List<TagDTO> tags = new ArrayList<>();
        tagListDTO.setTags(tags);

        String[] tagNames = {"internal", "important", "2018", "archive","API"};

        for (String tagName : tagNames) {
            TagDTO tagDTO = new TagDTO();
            tagDTO.setId(tagName);
            tagDTO.setWorkspaceId(workspaceId);
            tagDTO.setLabel(tagName);
            tags.add(tagDTO);
        }

        new TagsApi(client).createTags(workspaceId, tagListDTO);
    }


    private List<ACLEntryDTO> generateACLEntries_FullAccesForGroupContainAdminWks(){


        List<ACLEntryDTO> acls = new ArrayList<>();

        ACLEntryDTO aclEntryDTOGrp1 = new ACLEntryDTO();
        aclEntryDTOGrp1.setKey(group1);
        aclEntryDTOGrp1.setValue( ACLEntryDTO.ValueEnum.FULL_ACCESS);
        acls.add(aclEntryDTOGrp1);

        ACLEntryDTO aclEntryDTOGrp2 = new ACLEntryDTO();
        aclEntryDTOGrp2.setKey(group2);
        aclEntryDTOGrp2.setValue( ACLEntryDTO.ValueEnum.READ_ONLY);
        acls.add(aclEntryDTOGrp2);

        ACLEntryDTO aclEntryDTOGrp3 = new ACLEntryDTO();
        aclEntryDTOGrp3.setKey(group3);
        aclEntryDTOGrp3.setValue( ACLEntryDTO.ValueEnum.READ_ONLY);
        acls.add(aclEntryDTOGrp3);

        ACLEntryDTO aclEntryDTOGrp4 = new ACLEntryDTO();
        aclEntryDTOGrp4.setKey(group4);
        aclEntryDTOGrp4.setValue( ACLEntryDTO.ValueEnum.READ_ONLY);
        acls.add(aclEntryDTOGrp4);

        ACLEntryDTO aclEntryDTOGrp5 = new ACLEntryDTO();
        aclEntryDTOGrp5.setKey(group5);
        aclEntryDTOGrp5.setValue( ACLEntryDTO.ValueEnum.FORBIDDEN);
        acls.add(aclEntryDTOGrp5);

        return acls;
    }


    private List<ACLEntryDTO> generateACLEntries_giveReadAccesGrp5(){

        List<ACLEntryDTO> acls = new ArrayList<>();

        ACLEntryDTO aclEntryDTOGrp1 = new ACLEntryDTO();
        aclEntryDTOGrp1.setKey(group1);
        aclEntryDTOGrp1.setValue( ACLEntryDTO.ValueEnum.FULL_ACCESS);
        acls.add(aclEntryDTOGrp1);

        ACLEntryDTO aclEntryDTOGrp2 = new ACLEntryDTO();
        aclEntryDTOGrp2.setKey(group2);
        aclEntryDTOGrp2.setValue( ACLEntryDTO.ValueEnum.READ_ONLY);
        acls.add(aclEntryDTOGrp2);

        ACLEntryDTO aclEntryDTOGrp3 = new ACLEntryDTO();
        aclEntryDTOGrp3.setKey(group3);
        aclEntryDTOGrp3.setValue( ACLEntryDTO.ValueEnum.READ_ONLY);
        acls.add(aclEntryDTOGrp3);

        ACLEntryDTO aclEntryDTOGrp4 = new ACLEntryDTO();
        aclEntryDTOGrp4.setKey(group4);
        aclEntryDTOGrp4.setValue( ACLEntryDTO.ValueEnum.READ_ONLY);
        acls.add(aclEntryDTOGrp4);

        ACLEntryDTO aclEntryDTOGrp5 = new ACLEntryDTO();
        aclEntryDTOGrp5.setKey(group5);
        aclEntryDTOGrp5.setValue( ACLEntryDTO.ValueEnum.READ_ONLY);
        acls.add(aclEntryDTOGrp5);

        return acls;
    }

    private List<ACLEntryDTO> generateACLEntries_giveFullAccesGrp1AndGrp2(){

        List<ACLEntryDTO> acls = new ArrayList<>();

        ACLEntryDTO aclEntryDTOGrp1 = new ACLEntryDTO();
        aclEntryDTOGrp1.setKey(group1);
        aclEntryDTOGrp1.setValue( ACLEntryDTO.ValueEnum.FULL_ACCESS);
        acls.add(aclEntryDTOGrp1);

        ACLEntryDTO aclEntryDTOGrp2 = new ACLEntryDTO();
        aclEntryDTOGrp2.setKey(group2);
        aclEntryDTOGrp2.setValue( ACLEntryDTO.ValueEnum.FULL_ACCESS);
        acls.add(aclEntryDTOGrp2);

        ACLEntryDTO aclEntryDTOGrp3 = new ACLEntryDTO();
        aclEntryDTOGrp3.setKey(group3);
        aclEntryDTOGrp3.setValue( ACLEntryDTO.ValueEnum.FORBIDDEN);
        acls.add(aclEntryDTOGrp3);

        ACLEntryDTO aclEntryDTOGrp4 = new ACLEntryDTO();
        aclEntryDTOGrp4.setKey(group4);
        aclEntryDTOGrp4.setValue( ACLEntryDTO.ValueEnum.FORBIDDEN);
        acls.add(aclEntryDTOGrp4);

        ACLEntryDTO aclEntryDTOGrp5 = new ACLEntryDTO();
        aclEntryDTOGrp5.setKey(group5);
        aclEntryDTOGrp5.setValue( ACLEntryDTO.ValueEnum.FORBIDDEN);
        acls.add(aclEntryDTOGrp5);

        return acls;
    }

    private List<ACLEntryDTO> generateACLEntries_giveFullAccesGrp1AndGrp3(){

        List<ACLEntryDTO> acls = new ArrayList<>();

        ACLEntryDTO aclEntryDTOGrp1 = new ACLEntryDTO();
        aclEntryDTOGrp1.setKey(group1);
        aclEntryDTOGrp1.setValue( ACLEntryDTO.ValueEnum.FULL_ACCESS);
        acls.add(aclEntryDTOGrp1);

        ACLEntryDTO aclEntryDTOGrp2 = new ACLEntryDTO();
        aclEntryDTOGrp2.setKey(group2);
        aclEntryDTOGrp2.setValue( ACLEntryDTO.ValueEnum.FORBIDDEN);
        acls.add(aclEntryDTOGrp2);

        ACLEntryDTO aclEntryDTOGrp3 = new ACLEntryDTO();
        aclEntryDTOGrp3.setKey(group3);
        aclEntryDTOGrp3.setValue( ACLEntryDTO.ValueEnum.FULL_ACCESS);
        acls.add(aclEntryDTOGrp3);

        ACLEntryDTO aclEntryDTOGrp4 = new ACLEntryDTO();
        aclEntryDTOGrp4.setKey(group4);
        aclEntryDTOGrp4.setValue( ACLEntryDTO.ValueEnum.FORBIDDEN);
        acls.add(aclEntryDTOGrp4);

        ACLEntryDTO aclEntryDTOGrp5 = new ACLEntryDTO();
        aclEntryDTOGrp5.setKey(group5);
        aclEntryDTOGrp5.setValue( ACLEntryDTO.ValueEnum.FORBIDDEN);
        acls.add(aclEntryDTOGrp5);

        return acls;
    }

    private void createDocuments() throws ApiException, IOException {
        LOGGER.log(Level.INFO, "Creating documents ...");

        FoldersApi foldersApi = new FoldersApi(client);
        ACLDTO aclDto = new ACLDTO();
        aclDto.setGroupEntries(generateACLEntries_giveFullAccesGrp1AndGrp3());

        //Workflow model

        WorkflowModelDTO workflowModelDTO = new WorkspacesApi(client)
                .getWorkflowModelInWorkspace(workspaceId, "My first workflow");

        List<RoleMappingDTO> roleMappingDTOs = resolveDefaultRoles(workflowModelDTO);

        // Creation
        DocumentCreationDTO documentCreationDTO = new DocumentCreationDTO();
        documentCreationDTO.setReference("LETTER-001");
        documentCreationDTO.setTitle("My first letter");
        documentCreationDTO.setWorkspaceId(workspaceId);
        documentCreationDTO.setTemplateId("Letter");
        documentCreationDTO.setDescription("Some letter created with sample loader");
        documentCreationDTO.setWorkflowModelId(workflowModelDTO.getId());
        documentCreationDTO.setRoleMapping(roleMappingDTOs);

        documentCreationDTO.setAcl(aclDto);
        foldersApi.createDocumentMasterInFolder(workspaceId, documentCreationDTO, workspaceId + ":Letters");

        documentCreationDTO.setReference("LETTER-002");
        documentCreationDTO.setTitle("An other letter");
        foldersApi.createDocumentMasterInFolder(workspaceId, documentCreationDTO, workspaceId + ":Letters");

        aclDto.setGroupEntries(generateACLEntries_FullAccesForGroupContainAdminWks());
        documentCreationDTO.setAcl(aclDto);
        documentCreationDTO.setReference("INVOICE-001");
        documentCreationDTO.setTitle("My first invoice");
        documentCreationDTO.setWorkspaceId(workspaceId);
        documentCreationDTO.setTemplateId("Invoice");
        documentCreationDTO.setDescription("Some invoice created with sample loader");
        foldersApi.createDocumentMasterInFolder(workspaceId, documentCreationDTO, workspaceId + ":Invoices");

        documentCreationDTO.setReference("INVOICE-002");
        documentCreationDTO.setTitle("A second invoice");
        foldersApi.createDocumentMasterInFolder(workspaceId, documentCreationDTO, workspaceId + ":Invoices");

        documentCreationDTO.setReference("USER-MAN-001");
        documentCreationDTO.setTitle("User documentation");
        documentCreationDTO.setWorkspaceId(workspaceId);
        documentCreationDTO.setTemplateId("UserManuals");
        documentCreationDTO.setDescription("Some end-user documentation");
        foldersApi.createDocumentMasterInFolder(workspaceId, documentCreationDTO, workspaceId + ":Documentation");

        aclDto.setGroupEntries(generateACLEntries_giveReadAccesGrp5());
        documentCreationDTO.setReference("API-001");
        documentCreationDTO.setTitle("API V1.0");
        documentCreationDTO.setWorkspaceId(workspaceId);
        documentCreationDTO.setTemplateId("APIDocuments");
        documentCreationDTO.setAcl(aclDto);
        documentCreationDTO.setDescription("First version of API description ");

        foldersApi.createDocumentMasterInFolder(workspaceId, documentCreationDTO, workspaceId + ":APIManuals");

        LOGGER.log(Level.INFO, "Uploading document files...");
        // Upload
        DocumentIterationDTO documentIterationDTO = new DocumentIterationDTO();
        documentIterationDTO.setWorkspaceId(workspaceId);
        documentIterationDTO.setVersion("A");
        documentIterationDTO.setIteration(1);

        documentIterationDTO.setDocumentMasterId("LETTER-001");

        DocumentBinaryApi documentBinaryApi = new DocumentBinaryApi(client);

        documentBinaryApi.uploadDocumentFiles(workspaceId, "LETTER-001", "A", 1, SampleLoaderUtils.getFile("letter-001.docx"));
        documentBinaryApi.uploadDocumentFiles(workspaceId, "LETTER-002", "A", 1, SampleLoaderUtils.getFile("letter-002.docx"));
        documentBinaryApi.uploadDocumentFiles(workspaceId, "INVOICE-001", "A", 1, SampleLoaderUtils.getFile("invoice-001.xlsx"));
        documentBinaryApi.uploadDocumentFiles(workspaceId, "INVOICE-002", "A", 1, SampleLoaderUtils.getFile("invoice-002.xlsx"));
        documentBinaryApi.uploadDocumentFiles(workspaceId, "USER-MAN-001", "A", 1, SampleLoaderUtils.getFile("user-man-001.txt"));
        documentBinaryApi.uploadDocumentFiles(workspaceId, "API-001", "A", 1, SampleLoaderUtils.getFile("API-001"));

        // Check in
        LOGGER.log(Level.INFO, "Checking in documents ...");
        DocumentApi documentApi = new DocumentApi(client);
        documentApi.checkInDocument(workspaceId, "LETTER-001", "A");
        documentApi.checkInDocument(workspaceId, "LETTER-002", "A");
        documentApi.checkInDocument(workspaceId, "INVOICE-001", "A");
        documentApi.checkInDocument(workspaceId, "INVOICE-002", "A");
        documentApi.checkInDocument(workspaceId, "USER-MAN-001", "A");
        documentApi.checkInDocument(workspaceId, "API-001", "A");
    }


    private void createMilestones() throws ApiException {
        LOGGER.log(Level.INFO, "Creating milestones ...");
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, 15);

        MilestonesApi milestonesApi =new MilestonesApi(client);

        MilestoneDTO milestoneDTO = new MilestoneDTO();
        milestoneDTO.setWorkspaceId(workspaceId);
        milestoneDTO.setTitle("1.0");
        milestoneDTO.setDescription("First release");
        milestoneDTO.setDueDate(c.getTime());

        milestonesApi.createMilestone(workspaceId, milestoneDTO);

        c.add(Calendar.DATE, 90);
        milestoneDTO.setTitle("2.0");
        milestoneDTO.setDescription("Second release");
        milestoneDTO.setDueDate(c.getTime());
        milestonesApi.createMilestone(workspaceId, milestoneDTO);


    }

    private void setMilestoneAcl() throws ApiException {

        LOGGER.log(Level.INFO, "create access for milestones");
        MilestonesApi milestonesApi = new MilestonesApi(client);
        List<MilestoneDTO>milestoneDTOs = milestonesApi.getMilestones(workspaceId);
        ACLDTO aclDto = new ACLDTO();
        aclDto.setGroupEntries(generateACLEntries_FullAccesForGroupContainAdminWks());

        for(MilestoneDTO milestoneDTO : milestoneDTOs){

            milestonesApi.updateMilestoneACL(workspaceId,milestoneDTO.getId(),aclDto);
            LOGGER.log(Level.INFO, "updated milestone :"+milestoneDTO.getId());
        }
    }

    private void createRequests() throws ApiException {
        LOGGER.log(Level.INFO, "Creating requests ...");
        ChangeItemsApi changeItemsApi = new ChangeItemsApi(client);
        ChangeRequestDTO changeRequestDTO = new ChangeRequestDTO();
        changeRequestDTO.setWorkspaceId(workspaceId);

        changeRequestDTO.setName("REQ-001");
        changeRequestDTO.setDescription("Something needs to be corrected");
        changeRequestDTO.setCategory(ChangeRequestDTO.CategoryEnum.CORRECTIVE);
        changeItemsApi.createRequest(workspaceId, changeRequestDTO);

        changeRequestDTO.setName("REQ-002");
        changeRequestDTO.setDescription("Something needs to be perfected");
        changeRequestDTO.setCategory(ChangeRequestDTO.CategoryEnum.PERFECTIVE);
        changeItemsApi.createRequest(workspaceId, changeRequestDTO);
    }

    private void createIssues() throws ApiException {
        LOGGER.log(Level.INFO, "Creating issues ...");
        ChangeItemsApi changeItemsApi = new ChangeItemsApi(client);
        ChangeIssueDTO changeIssueDTO = new ChangeIssueDTO();
        changeIssueDTO.setWorkspaceId(workspaceId);

        changeIssueDTO.setName("ISSUE-001");
        changeIssueDTO.setDescription("Something is wrong");
        changeIssueDTO.setPriority(ChangeIssueDTO.PriorityEnum.HIGH);
        changeIssueDTO.setAssignee("bill");
        changeItemsApi.createIssue(workspaceId, changeIssueDTO);


        changeIssueDTO.setName("ISSUE-002");
        changeIssueDTO.setDescription("Something is terribly wrong");
        changeIssueDTO.setPriority(ChangeIssueDTO.PriorityEnum.EMERGENCY);
        changeIssueDTO.setAssignee("joe");
        changeItemsApi.createIssue(workspaceId, changeIssueDTO);
    }

    private void setIssuesAcl() throws ApiException {

        LOGGER.log(Level.INFO, "create access for issues");
        ChangeItemsApi changeItemApi = new ChangeItemsApi(client);
        List<ChangeIssueDTO> changeIssueDTOs = changeItemApi.getIssues(workspaceId);
        ACLDTO aclDto = new ACLDTO();
        aclDto.setGroupEntries(generateACLEntries_FullAccesForGroupContainAdminWks());

        for(ChangeIssueDTO chageIssueDTO: changeIssueDTOs) {

            changeItemApi.updateChangeIssueACL(workspaceId, chageIssueDTO.getId(), aclDto);
            LOGGER.log(Level.INFO, "updated milestone :" + chageIssueDTO.getId());
        }
    }

    private void createOrders() throws ApiException {
        LOGGER.log(Level.INFO, "Creating orders ...");
        ChangeItemsApi changeItemsApi = new ChangeItemsApi(client);
        ChangeOrderDTO changeOrderDTO = new ChangeOrderDTO();
        changeOrderDTO.setWorkspaceId(workspaceId);

        changeOrderDTO.setName("ORDER-001");
        changeOrderDTO.setDescription("Order for some documents");
        changeOrderDTO.setCategory(ChangeOrderDTO.CategoryEnum.OTHER);
        changeItemsApi.createOrder(workspaceId, changeOrderDTO);

        changeOrderDTO.setName("ORDER-002");
        changeOrderDTO.setDescription("Order for some parts");
        changeOrderDTO.setCategory(ChangeOrderDTO.CategoryEnum.OTHER);
        changeItemsApi.createOrder(workspaceId, changeOrderDTO);
    }

    private void setWorkFlowACL() throws ApiException{

        LOGGER.log(Level.INFO,"Setting acl for created workflows...");
        WorkflowModelsApi workflowModelsApi = new WorkflowModelsApi(client);
        List<WorkflowModelDTO> workflowModelDTOs = workflowModelsApi.getWorkflowModelsInWorkspace(workspaceId);
        ACLDTO aclDto = new ACLDTO();
        aclDto.setGroupEntries(generateACLEntries_FullAccesForGroupContainAdminWks());
        for(WorkflowModelDTO workflowModelDTO : workflowModelDTOs){

            workflowModelsApi.updateWorkflowModelACL(workspaceId,workflowModelDTO.getId(),aclDto);
            LOGGER.log(Level.INFO,"workflow with id "+workflowModelDTO.getId()+" was updated...");
        }
    }

    private void createRolesAndWorkflow() throws ApiException {
        LOGGER.log(Level.INFO, "Creating roles ...");
        RolesApi rolesApi = new RolesApi(client);
        // Roles
        List<UserDTO> designers = new ArrayList<>();
        List<UserDTO> technicians = new ArrayList<>();
        List<UserGroupDTO> groupsAvailable=  new WorkspacesApi(client).getGroups(workspaceId);

        UserDTO rob = new UserDTO();
        rob.setWorkspaceId(workspaceId);
        rob.setLogin("rob");
        designers.add(rob);

        UserDTO joe = new UserDTO();
        joe.setWorkspaceId(workspaceId);
        joe.setLogin("joe");
        technicians.add(joe);

        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setWorkspaceId(workspaceId);

        roleDTO.setName("designers");
        roleDTO.setDefaultAssignedUsers(designers);
        RoleDTO designerRole = rolesApi.createRole(workspaceId, roleDTO);

        roleDTO.setName("technicians");
        roleDTO.setDefaultAssignedUsers(technicians);
        RoleDTO technicianRole = rolesApi.createRole(workspaceId, roleDTO);

        RoleDTO roleGroupDTO = new RoleDTO();
        roleGroupDTO.setWorkspaceId(workspaceId);
        List<UserGroupDTO> tmpArrays =  new ArrayList<>();

        roleGroupDTO.setDefaultAssignedUsers(null);
        tmpArrays.add(groupsAvailable.get(0));

        roleGroupDTO.setName("ceo");
        roleGroupDTO.setDefaultAssignedGroups(tmpArrays);
        RoleDTO ceo = rolesApi.createRole(workspaceId,roleGroupDTO);

        tmpArrays =  new ArrayList<>();
        tmpArrays.add(groupsAvailable.get(1));
        tmpArrays.add(groupsAvailable.get(2));

        roleGroupDTO.setName("ingineers");
        roleGroupDTO.setDefaultAssignedGroups(tmpArrays);
        RoleDTO engineers = rolesApi.createRole(workspaceId,roleGroupDTO);

        tmpArrays =  new ArrayList<>();
        tmpArrays.add(groupsAvailable.get(3));

        roleGroupDTO.setName("support");
        roleGroupDTO.setDefaultAssignedGroups(tmpArrays);
        RoleDTO support = rolesApi.createRole(workspaceId,roleGroupDTO);

        tmpArrays = null;

        // Workflow
        LOGGER.log(Level.INFO, "Creating workflow ...");

        TaskModelDTO firstTask = new TaskModelDTO();
        firstTask.setNum(0);
        firstTask.setTitle("Organise Milestones");
        firstTask.setInstructions("check the requests client and organise works with engineers");
        firstTask.setRole(ceo);

        TaskModelDTO secondTask = new TaskModelDTO();
        secondTask.setNum(1);
        secondTask.setTitle("Build architecture diagrams");
        secondTask.setInstructions("Build the prototype and validate the task");
        secondTask.setRole(engineers);

        TaskModelDTO thirdTask = new TaskModelDTO();
        thirdTask .setNum(2);
        thirdTask .setTitle("Start first iteration");
        thirdTask .setInstructions("Make a reunion with technicians and start to plane iterations");
        thirdTask .setRole(technicianRole);

        TaskModelDTO fourthTask = new TaskModelDTO();
        fourthTask.setNum(3);
        fourthTask.setTitle("Design some prototypes");
        fourthTask.setInstructions("Create a new prototypes design, then validate the task");
        fourthTask.setRole(designerRole);

        TaskModelDTO fifthTask = new TaskModelDTO();
        fifthTask.setNum(4);
        fifthTask.setTitle("Open Ticket and versioning management");
        fifthTask.setInstructions("check modifications and open tickets if necessary with right version");
        fifthTask.setRole(support);

        List<TaskModelDTO> tasks = new ArrayList<>();
        tasks.add(firstTask);
        tasks.add(secondTask);
        tasks.add(thirdTask);
        tasks.add(fourthTask);
        tasks.add(fifthTask);

        ActivityModelDTO firstActivity = new ActivityModelDTO();
        firstActivity.setStep(0);
        firstActivity.setTaskModels(tasks);
        firstActivity.setType(ActivityModelDTO.TypeEnum.SEQUENTIAL);
        firstActivity.setLifeCycleState("Start project");

        List<ActivityModelDTO> activities = new ArrayList<>();
        activities.add(firstActivity);

        WorkflowModelDTO workflowModelDTO = new WorkflowModelDTO();
        workflowModelDTO.setActivityModels(activities);
        workflowModelDTO.setReference("My first workflow");
        workflowModelDTO.setFinalLifeCycleState("Success");
        workflowModelDTO.setId("My first workflow");

        new WorkflowModelsApi(client).createWorkflowModel(workspaceId, workflowModelDTO);
    }


    private void createPartTemplates() throws ApiException {
        LOGGER.log(Level.INFO, "Creating part templates ...");
        PartTemplatesApi partTemplatesApi = new PartTemplatesApi(client);
        PartTemplateCreationDTO partTemplateCreationDTO = new PartTemplateCreationDTO();
        partTemplateCreationDTO.setWorkspaceId(workspaceId);
        partTemplateCreationDTO.setReference("SEATS");
        partTemplateCreationDTO.setMask("SEAT-###");
        partTemplateCreationDTO.setAttributesLocked(true);

        List<InstanceAttributeTemplateDTO> attributes = new ArrayList<>();
        InstanceAttributeTemplateDTO weight = AttributesHelper.createInstanceAttributeTemplate(
                InstanceAttributeTemplateDTO.AttributeTypeEnum.NUMBER, "weight", true, true);
        InstanceAttributeTemplateDTO price = AttributesHelper.createInstanceAttributeTemplate(
                InstanceAttributeTemplateDTO.AttributeTypeEnum.NUMBER, "price", true, true);

        attributes.add(price);
        attributes.add(weight);

        partTemplateCreationDTO.setAttributeTemplates(attributes);
        partTemplatesApi.createPartMasterTemplate(workspaceId, partTemplateCreationDTO);

        partTemplateCreationDTO.setReference("ENGINES");
        partTemplateCreationDTO.setMask("ENGINE-###");
        partTemplateCreationDTO.setAttributeTemplates(attributes);
        partTemplatesApi.createPartMasterTemplate(workspaceId, partTemplateCreationDTO);

        partTemplateCreationDTO.setReference("DOOR");
        partTemplateCreationDTO.setMask("DOOR-###");
        partTemplateCreationDTO.setAttributeTemplates(attributes);
        partTemplatesApi.createPartMasterTemplate(workspaceId, partTemplateCreationDTO);

        partTemplateCreationDTO.setReference("WINDOW");
        partTemplateCreationDTO.setMask("WINDOW-###");
        partTemplateCreationDTO.setAttributeTemplates(attributes);
        partTemplatesApi.createPartMasterTemplate(workspaceId, partTemplateCreationDTO);

        partTemplateCreationDTO.setReference("LOCK");
        partTemplateCreationDTO.setMask("LOCK-###");
        partTemplateCreationDTO.setAttributeTemplates(attributes);
        partTemplatesApi.createPartMasterTemplate(workspaceId, partTemplateCreationDTO);
    }


    private void createParts() throws ApiException, IOException, InterruptedException {

        LOGGER.log(Level.INFO, "Creating parts ...");
        PartsApi partsApi = new PartsApi(client);
        PartCreationDTO part = new PartCreationDTO();

        part.setWorkspaceId(workspaceId);
        part.setDescription("Sample part create with sample loader");

        //ACLS set up
        ACLDTO aclDto = new ACLDTO();
        aclDto.setGroupEntries(generateACLEntries_giveFullAccesGrp1AndGrp2());

        //Workflow model creation
        WorkflowModelDTO workflowModelDTO = new WorkspacesApi(client)
                .getWorkflowModelInWorkspace(workspaceId, "My first workflow");
        List<RoleMappingDTO> roleMappingDTOs = resolveDefaultRoles(workflowModelDTO);

        //Parts creations
        part.setTemplateId("SEATS");
        part.setNumber("SEAT-010");
        part.setName("front seat");
        part.setWorkflowModelId(workflowModelDTO.getId());
        part.setRoleMapping(roleMappingDTOs);
        part.setAcl(aclDto);
        PartRevisionDTO frontSeat = partsApi.createNewPart(workspaceId, part);
        addAttributes(partsApi, frontSeat);
        part.setNumber("SEAT-020");
        part.setName("back seat");
        PartRevisionDTO backSeat = partsApi.createNewPart(workspaceId, part);
        addAttributes(partsApi, backSeat);

        part.setTemplateId("ENGINES");
        part.setNumber("ENGINE-050");
        part.setName("50cc engine");
        PartRevisionDTO engine50 = partsApi.createNewPart(workspaceId, part);
        addAttributes(partsApi, engine50);
        part.setNumber("ENGINE-100");
        part.setName("100cc engine");
        PartRevisionDTO engine100 = partsApi.createNewPart(workspaceId, part);
        addAttributes(partsApi, engine100);

        // Create an assembly
        part.setNumber("CAR-001");
        part.setName("Car assembly");
        part.setTemplateId(null);
        PartRevisionDTO assembly = partsApi.createNewPart(workspaceId, part);
        PartIterationDTO lastIteration = LastIterationHelper.getLastIteration(assembly);

        List<PartUsageLinkDTO> links = new ArrayList<>();

        PartUsageLinkDTO driverSeatLink = new PartUsageLinkDTO();
        PartUsageLinkDTO passengerSeatLink = new PartUsageLinkDTO();
        PartUsageLinkDTO engineLink = new PartUsageLinkDTO();

        ComponentDTO seat = new ComponentDTO();
        seat.setNumber("SEAT-010");
        seat.setVersion("A");

        ComponentDTO engine = new ComponentDTO();
        engine.setNumber("ENGINE-050");
        engine.setVersion("A");

        driverSeatLink.setComponent(seat);
        driverSeatLink.setAmount(1.0);
        driverSeatLink.setReferenceDescription("Driver seat");
        driverSeatLink.setOptional(true);

        passengerSeatLink.setComponent(seat);
        passengerSeatLink.setAmount(1.0);
        passengerSeatLink.setReferenceDescription("Passenger seat");
        passengerSeatLink.setOptional(true);

        engineLink.setComponent(engine);
        engineLink.setAmount(1.0);
        engineLink.setReferenceDescription("The engine of this car");
        engineLink.setOptional(true);

        links.add(driverSeatLink);
        links.add(passengerSeatLink);
        links.add(engineLink);

        CADInstanceDTO driverSeatCadInstance = new CADInstanceDTO();
        driverSeatCadInstance.setRx(0.0);
        driverSeatCadInstance.setRy(0.0);
        driverSeatCadInstance.setRz(0.0);
        driverSeatCadInstance.setTx(0.0);
        driverSeatCadInstance.setTy(0.0);
        driverSeatCadInstance.setTz(0.0);

        CADInstanceDTO passengerSeatCadInstance = new CADInstanceDTO();
        passengerSeatCadInstance.setRx(0.0);
        passengerSeatCadInstance.setRy(0.0);
        passengerSeatCadInstance.setRz(0.0);
        passengerSeatCadInstance.setTx(20.0);
        passengerSeatCadInstance.setTy(0.0);
        passengerSeatCadInstance.setTz(0.0);

        CADInstanceDTO engineCadInstance = new CADInstanceDTO();
        engineCadInstance.setRx(0.0);
        engineCadInstance.setRy(0.0);
        engineCadInstance.setRz(0.0);
        engineCadInstance.setTx(0.0);
        engineCadInstance.setTy(0.0);
        engineCadInstance.setTz(50.0);

        List<CADInstanceDTO> driverSeatCadInstances = new ArrayList<>();
        driverSeatCadInstances.add(driverSeatCadInstance);
        driverSeatLink.setCadInstances(driverSeatCadInstances);

        List<CADInstanceDTO> passengerSeatCadInstances = new ArrayList<>();
        passengerSeatCadInstances.add(passengerSeatCadInstance);
        passengerSeatLink.setCadInstances(passengerSeatCadInstances);

        List<CADInstanceDTO> engineCadInstances = new ArrayList<>();
        engineCadInstances.add(engineCadInstance);
        engineLink.setCadInstances(engineCadInstances);

        lastIteration.setComponents(links);
        lastIteration.setIterationNote("Creating assembly");

        partsApi.updatePartIteration(workspaceId, assembly.getNumber(), "A", 1, lastIteration);

        // Upload 3D files
        LOGGER.log(Level.INFO, "Uploading 3D files...");

        PartIterationDTO partIterationDTO = new PartIterationDTO();
        partIterationDTO.setWorkspaceId(workspaceId);
        partIterationDTO.setVersion("A");
        partIterationDTO.setIteration(1);

        partIterationDTO.setNumber("SEAT-010");
        uploadNativeCADFile(partIterationDTO, client, SampleLoaderUtils.getFile("BassBoat-FrontSeat.obj"));
        uploadAttachedFile(partIterationDTO, client, SampleLoaderUtils.getFile("BassBoat-FrontSeat.mtl"));

        partIterationDTO.setNumber("SEAT-020");
        uploadNativeCADFile(partIterationDTO, client, SampleLoaderUtils.getFile("BassBoat-BackSeat.obj"));
        uploadAttachedFile(partIterationDTO, client, SampleLoaderUtils.getFile("BassBoat-BackSeat.mtl"));

        partIterationDTO.setNumber("ENGINE-050");
        uploadNativeCADFile(partIterationDTO, client, SampleLoaderUtils.getFile("BassBoat-OutboardMotor.obj"));
        uploadAttachedFile(partIterationDTO, client, SampleLoaderUtils.getFile("BassBoat-OutboardMotor.mtl"));

        partIterationDTO.setNumber("ENGINE-100");
        uploadNativeCADFile(partIterationDTO, client, SampleLoaderUtils.getFile("BassBoat-TrollingMotor.obj"));
        uploadAttachedFile(partIterationDTO, client, SampleLoaderUtils.getFile("BassBoat-TrollingMotor.mtl"));

        LOGGER.log(Level.INFO, "Waiting for conversion...");
        // Let the conversion finish
        Thread.sleep(5000);

        LOGGER.log(Level.INFO, "Checking in parts...");
        new PartApi(client).checkIn(workspaceId, "SEAT-010", "A");
        new PartApi(client).checkIn(workspaceId, "SEAT-020", "A");
        new PartApi(client).checkIn(workspaceId, "ENGINE-050", "A");
        new PartApi(client).checkIn(workspaceId, "ENGINE-100", "A");
        new PartApi(client).checkIn(workspaceId, "CAR-001", "A");

    }

    private void addAttributes(PartsApi partsApi, PartRevisionDTO partRevision) throws ApiException {

        PartIterationDTO lastIteration = LastIterationHelper.getLastIteration(partRevision);

        // Define weight and price
        List<InstanceAttributeDTO> attributes = lastIteration.getInstanceAttributes();

        if (attributes.size() == 2) {
            attributes.get(0).setValue(String.valueOf(Math.random() * 20));
            attributes.get(1).setValue(String.valueOf(Math.random() * 20));
        } else {
            LOGGER.log(Level.WARNING, "Attributes have not been found");
        }

        partsApi.updatePartIteration(workspaceId, partRevision.getNumber(), "A", 1, lastIteration);

    }

    private void uploadAttachedFile(PartIterationDTO partIterationDTO, ApiClient client, File file) throws ApiException {
        new PartBinaryApi(client).uploadAttachedFiles(partIterationDTO.getWorkspaceId(),
                partIterationDTO.getNumber(), partIterationDTO.getVersion(), 1, file);
    }

    private void uploadNativeCADFile(PartIterationDTO partIterationDTO, ApiClient client, File file) throws ApiException {
        new PartBinaryApi(client).uploadNativeCADFile(partIterationDTO.getWorkspaceId(),
                partIterationDTO.getNumber(), partIterationDTO.getVersion(), 1, file);
    }

    private void createProducts() throws ApiException {
        LOGGER.log(Level.INFO, "Creating products ...");
        ConfigurationItemDTO configurationItemDTO = new ConfigurationItemDTO();
        configurationItemDTO.setWorkspaceId(workspaceId);
        configurationItemDTO.setDesignItemNumber("CAR-001");
        configurationItemDTO.setId("CAR-001");
        new ProductsApi(client).createConfigurationItem(workspaceId, configurationItemDTO);
    }

    private void createBaseline() throws ApiException {
        ProductBaselineDTO baseline = new ProductBaselineDTO();
        baseline.setConfigurationItemId("CAR-001");
        baseline.setName("MyFirstBaseline");
        baseline.setType(ProductBaselineDTO.TypeEnum.LATEST);
        new ProductBaselineApi(client).createProductBaseline(workspaceId, baseline);
    }

    private void createProductInstance() throws ApiException {

        List<ProductBaselineDTO> baselines = new ProductBaselineApi(client).getProductBaselinesForProduct(workspaceId, "CAR-001");
        ProductBaselineDTO firstBaselineFound = baselines.get(0);

        ProductInstanceCreationDTO productInstance = new ProductInstanceCreationDTO();
        productInstance.setConfigurationItemId("CAR-001");
        productInstance.setSerialNumber("CAR-001-XX001");

        productInstance.setBaselineId(firstBaselineFound.getId());
        new ProductInstancesApi(client).createProductInstanceMaster(workspaceId, productInstance);
    }


    //inspired by test which located here :  com.docdoku.api.ProductApiTest
    private void createDoorProduct() throws ApiException {

        LOGGER.log(Level.INFO, "Creating the door Product ...");
        PartsApi partsApi = new PartsApi(client);
        PartApi partApi = new PartApi(client);
        ProductsApi productsApi = new ProductsApi(client);
        String[] partsNumber =  {"DOOR-001","WINDOW-001","LOCK-001"};

        //ACLS set up

        ACLDTO aclDto = new ACLDTO();
        aclDto.setGroupEntries(generateACLEntries_giveFullAccesGrp1AndGrp2());

        //Workflow model creation

        WorkflowModelDTO workflowModelDTO = new WorkspacesApi(client)
                .getWorkflowModelInWorkspace(workspaceId, "Workflow-door-creation");
        List<RoleMappingDTO> roleMappingDTOs = resolveDefaultRoles(workflowModelDTO);

        //Create Parts for door structure
        PartCreationDTO partCreationDTO = new PartCreationDTO();
        List<String> useOptionalLinks =  new ArrayList<>();

        partCreationDTO.setTemplateId("DOOR");
        partCreationDTO.setNumber(partsNumber[0]);
        partCreationDTO.setName("Left front door");
        partCreationDTO.setVersion("A");
        partCreationDTO.setWorkflowModelId(workflowModelDTO.getId());
        partCreationDTO.setRoleMapping(roleMappingDTOs);
        partCreationDTO.setAcl(aclDto);

        PartRevisionDTO leftDoor =  partsApi.createNewPart(workspaceId,partCreationDTO);
        addAttributes(partsApi,leftDoor);

        partCreationDTO.setTemplateId("WINDOW");
        partCreationDTO.setNumber(partsNumber[1]);
        partCreationDTO.setName("Left front window");

        PartRevisionDTO leftWindow =  partsApi.createNewPart(workspaceId,partCreationDTO);
        addAttributes(partsApi,leftWindow);

        partCreationDTO.setTemplateId("LOCK");
        partCreationDTO.setNumber(partsNumber[2]);
        partCreationDTO.setName("Left front lock");

        //Missing CAD and attached files ( don't forget to add them when issue related will be fix)

        PartRevisionDTO leftLock =  partsApi.createNewPart(workspaceId,partCreationDTO);
        addAttributes(partsApi,leftLock);

        //Create structure product
        // 1 - DOOR
        //     1.1 - LOCK
        //     1.2 - WINDOW

        PartRevisionDTO doorRevisionDto =  partApi.getPartRevision(workspaceId,partsNumber[0],"A");
        PartIterationDTO doorIterationDto = LastIterationHelper.getLastIteration(doorRevisionDto);

        List<PartUsageLinkDTO> components =  new ArrayList<>();

        PartUsageLinkDTO windowLink =  new PartUsageLinkDTO();
        ComponentDTO windowComponent = new ComponentDTO();
        windowComponent.setNumber(partsNumber[1]);
        windowComponent.setAmount(1.0);
        windowComponent.setVersion("A");
        windowComponent.setOptional(true);
        windowComponent.setPartUsageLinkReferenceDescription("DOOR -> WINDOWS");
        windowLink.setComponent(windowComponent);
        windowLink.setOptional(true);
        components.add(windowLink);

        PartUsageLinkDTO lockLink =  new PartUsageLinkDTO();
        ComponentDTO lockComponent = new ComponentDTO();
        lockComponent.setNumber(partsNumber[2]);
        lockComponent.setAmount(1.0);
        lockComponent.setVersion("A");
        lockComponent.setOptional(true);
        lockComponent.setPartUsageLinkReferenceDescription("DOOR -> LOCK");
        lockLink.setComponent(lockComponent);
        lockLink.setOptional(true);
        components.add(lockLink);
        doorIterationDto.setComponents(components);

        partApi.updatePartIteration(workspaceId,partsNumber[0],"A",1,doorIterationDto);
        for(String s : partsNumber){

            partApi.checkIn(workspaceId,s,"A");
        }

        //Create the product
        ConfigurationItemDTO product = new ConfigurationItemDTO();
        product.setId("DOOR-001");
        product.setDesignItemNumber(partsNumber[0]);
        product.setDescription("GENERATED FROM SAMPLE DATA FOR TEST");
        product.setWorkspaceId(workspaceId);

        productsApi.createConfigurationItem(workspaceId,product);

        doorRevisionDto  = partApi.getPartRevision(workspaceId,partsNumber[0],"A");
        doorIterationDto = LastIterationHelper.getLastIteration(doorRevisionDto);

        //Create the baseline
        ProductBaselineDTO baseline =  new ProductBaselineDTO();
        baseline.setType(ProductBaselineDTO.TypeEnum.LATEST);
        baseline.setName("DOOR-BASELINE");
        baseline.setConfigurationItemId(product.getId());
        for(PartUsageLinkDTO puldto : doorIterationDto.getComponents() ){

            useOptionalLinks.add("-1-" + puldto.getFullId());
        }
        baseline.setOptionalUsageLinks(useOptionalLinks);

        ComponentDTO structure = productsApi.filterProductStructure(workspaceId,
                product.getId(), "wip", "-1", -1, null, true);

        List<ComponentDTO> structureComponents = structure.getComponents();

        //Create a typed links
        LightPathToPathLinkDTO link = new LightPathToPathLinkDTO();
        link.setType("description ( added from sample )");
        link.setDescription("a typed link created from sample data");
        link.setSourcePath(structureComponents.get(0).getPath());
        link.setTargetPath(structureComponents.get(1).getPath());
        productsApi.createPathToPathLink(workspaceId, product.getId(), link);
        baseline.setPathToPathLinks(product.getPathToPathLinks());

        new ProductBaselineApi(client).createProductBaseline(workspaceId,baseline);
    }

    private void createConfiguration() throws ApiException {

        LOGGER.log(Level.INFO, "Creating configuration ...");
        ProductConfigurationsApi productConfigurationsApi = new ProductConfigurationsApi(client);

        List<String> useOptionalLinks =  new ArrayList<>();
        ACLDTO aclDto = new ACLDTO();
        aclDto.setGroupEntries(generateACLEntries_FullAccesForGroupContainAdminWks());

        PartRevisionDTO doorRevisionDto =  new PartApi(client).getPartRevision(workspaceId,"DOOR-001","A");
        PartIterationDTO doorIterationDto = LastIterationHelper.getLastIteration(doorRevisionDto);

        for(PartUsageLinkDTO puldto : doorIterationDto.getComponents() ){

            useOptionalLinks.add("-1-"+puldto.getFullId());
        }

        ProductConfigurationDTO productConfigurationDTO = new ProductConfigurationDTO();
        productConfigurationDTO.setName("cfg-001");
        productConfigurationDTO.setConfigurationItemId("DOOR-001");
        productConfigurationDTO.setDescription("configuration created from sample");
        productConfigurationDTO.setOptionalUsageLinks(useOptionalLinks);
        productConfigurationDTO.setAcl(aclDto);
        productConfigurationsApi.createConfiguration(workspaceId,productConfigurationDTO);
    }


    private void createRolesAndWorkflowForDoorProduct() throws ApiException {

        LOGGER.log(Level.INFO, "Creating roles for door product ...");
        RolesApi rolesApi = new RolesApi(client);
        List<UserGroupDTO> groupsAvailable=  new WorkspacesApi(client).getGroups(workspaceId);

        List<UserGroupDTO> tmpArrays =  new ArrayList<>();
        tmpArrays.add(groupsAvailable.get(1));
        tmpArrays.add(groupsAvailable.get(2));

        RoleDTO roleGroupDTO = new RoleDTO();
        roleGroupDTO.setWorkspaceId(workspaceId);
        roleGroupDTO.setName("Assembly engineers");
        roleGroupDTO.setDefaultAssignedGroups(tmpArrays);
        RoleDTO engineers = rolesApi.createRole(workspaceId,roleGroupDTO);

        // Workflow
        LOGGER.log(Level.INFO, "Setting workflow ...");

        TaskModelDTO firstTask = new TaskModelDTO();
        firstTask.setNum(0);
        firstTask.setTitle("design door prototype");
        firstTask.setInstructions("make design for door");
        firstTask.setRole(engineers);

        TaskModelDTO secondTask = new TaskModelDTO();
        secondTask.setNum(1);
        secondTask.setTitle("Build door prototype");
        secondTask.setInstructions("Build the prototype");
        secondTask.setRole(engineers);

        List<TaskModelDTO> tasks = new ArrayList<>();
        tasks.add(firstTask);
        tasks.add(secondTask);

        ActivityModelDTO firstActivity = new ActivityModelDTO();
        firstActivity.setStep(0);
        firstActivity.setTaskModels(tasks);
        firstActivity.setType(ActivityModelDTO.TypeEnum.SEQUENTIAL);
        firstActivity.setLifeCycleState("Build Door");

        List<TaskModelDTO> tasks2 = new ArrayList<>();

        TaskModelDTO fourthTask = new TaskModelDTO();
        fourthTask.setNum(0);
        fourthTask.setTitle("Build lock ");
        fourthTask.setInstructions("build a lock");
        fourthTask.setRole(engineers);

        TaskModelDTO fifthTask = new TaskModelDTO();
        fifthTask.setNum(1);
        fifthTask.setTitle("bild window");
        fifthTask.setInstructions("build window");
        fifthTask.setRole(engineers);

        tasks2.add(fourthTask);
        tasks2.add(fifthTask);

        ActivityModelDTO secondActivity = new ActivityModelDTO();
        secondActivity.setStep(1);
        secondActivity.setTaskModels(tasks2);
        secondActivity.setType(ActivityModelDTO.TypeEnum.PARALLEL);
        secondActivity.setLifeCycleState("Designing lock and door");
        secondActivity.setTasksToComplete(0);

        List<ActivityModelDTO> activities = new ArrayList<>();
        activities.add(firstActivity);
        activities.add(secondActivity);

        LOGGER.log(Level.INFO, "Creating workflow ...");
        WorkflowModelDTO workflowModelDTO = new WorkflowModelDTO();
        workflowModelDTO.setActivityModels(activities);
        workflowModelDTO.setReference("Workflow-door-creation");
        workflowModelDTO.setFinalLifeCycleState("Terminated");
        workflowModelDTO.setId("Workflow-door-creation");

        new WorkflowModelsApi(client).createWorkflowModel(workspaceId, workflowModelDTO);
    }

    //take from test : com.docdoku.api.WorkflowApiTest
    private List<RoleMappingDTO> resolveDefaultRoles(WorkflowModelDTO workflowModel) {
        Set<RoleDTO> rolesInvolved = WorkflowHelper.getRolesInvolved(workflowModel);
        List<RoleMappingDTO> roleMapping = new ArrayList<>();

        // we need to resolve the roles (use defaults assignments)
        for (RoleDTO role : rolesInvolved) {

            RoleMappingDTO roleMappingDTO = new RoleMappingDTO();
            roleMappingDTO.setRoleName(role.getName());

            for (UserGroupDTO group : role.getDefaultAssignedGroups()) {

                roleMappingDTO.getGroupIds().add(group.getId());
            }
            for (UserDTO user : role.getDefaultAssignedUsers()) {
                roleMappingDTO.getUserLogins().add(user.getLogin());
            }
            roleMapping.add(roleMappingDTO);
        }

        return roleMapping;
    }

    private void checkoutParts() throws ApiException {

        LOGGER.log(Level.INFO, "Doing checkout on some parts and documents...");
        PartApi partApi = new PartApi(client);
        partApi.checkOut(workspaceId,"DOOR-001","A");
        partApi.checkOut(workspaceId,"LOCK-001","A");
        partApi.checkOut(workspaceId,"WINDOW-001","A");

        ApiClient joe = DocDokuPLMClientFactory.createJWTClient(url, "joe", "test");
        partApi.setApiClient(joe);

        partApi.checkOut(workspaceId,"CAR-001","A");
        partApi.checkOut(workspaceId,"ENGINE-100","A");

        ApiClient rob = DocDokuPLMClientFactory.createJWTClient(url, "rob", "test");
        partApi.setApiClient(rob);
        partApi.checkOut(workspaceId,"ENGINE-050","A");
        partApi.checkOut(workspaceId,"SEAT-010","A");

        ApiClient steve = DocDokuPLMClientFactory.createJWTClient(url, "steve", "test");
        partApi.setApiClient(steve);
        partApi.checkOut(workspaceId,"SEAT-020","A");

        DocumentApi documentApi = new DocumentApi(client);
        documentApi.checkOutDocument(workspaceId,"USER-MAN-001","A");
        documentApi.checkOutDocument(workspaceId,"INVOICE-002","A");
        documentApi.checkOutDocument(workspaceId,"INVOICE-001","A");

        documentApi.setApiClient(joe);
        documentApi.checkOutDocument(workspaceId,"API-001","A");

        ApiClient bill = DocDokuPLMClientFactory.createJWTClient(url, "bill", "test");
        documentApi.setApiClient(bill);
        documentApi.checkOutDocument(workspaceId,"LETTER-001","A");
        documentApi.checkOutDocument(workspaceId,"LETTER-002","A");

    }
}
