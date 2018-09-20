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
import java.util.logging.Logger;

/**
 * This class uses DocDokuPLM Java API to load sample data to given server url and workspace
 *
 * @author Morgan GUIMARD
 */
public class SampleLoader {

    private static final Logger LOGGER = SampleLoaderLogger.getLOGGER();

    private static final String[] LOGINS = {"rob", "joe", "steve", "mickey", "bill", "rendal", "winie", "titi", "toto", "tata"};

    private static final String GROUP_1 = "Group1";
    private static final String GROUP_2 = "Group2";
    private static final String GROUP_3 = "Group3";
    private static final String GROUP_4 = "Group4";
    private static final String GROUP_5 = "Group5";

    private final static int SLEEPTIME = 500;

    private String login;
    private String password;
    private String workspaceId;
    private String url;

    private ApiClient client;
    private ApiClient guestClient;


    public SampleLoader(String login, String password, String workspaceId, String url) {
        this.login = login;
        this.password = password;
        this.workspaceId = workspaceId;
        this.url = url;
        this.guestClient = DocDokuPLMClientFactory.createClient(url);
    }

    public void load() throws ApiException, IOException, InterruptedException {
        LOGGER.info("Starting load process... ");

        checkServerAvailability();

        try {
            createCallerAccount();
        } catch (ApiException e) {
            LOGGER.info("Cannot create account, trying to use given credentials for next operations");
        }

        client = DocDokuPLMClientFactory.createJWTClient(url, login, password);

        createWorkspace();
        addUserToWorkspace(login);
        createOtherAccounts();
        createGroups();
        setAccessPermissionForGroups();
        enableUserInWorkspace();
        setAccessPermissionsForUser();

        createOrganization();

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
        createDoorProduct();

        createCarProduct();
        createNewVersionsAndReleasedParts();
        createEffectivities();
        createConfiguration();
        createBaseline();
        createProductInstance();

        createRequests();
        setRequestsAcl();
        createIssues();
        setIssuesAcl();
        createOrders();
        setOrdersAcl();
        updateAffectedPartInOrder();
        subscribeGroupToTag();

        checkoutParts();
    }

    private void checkServerAvailability() throws ApiException {
        LOGGER.info("Checking server availability...");
        new LanguagesApi(guestClient).getLanguages();
    }

    private void createCallerAccount() throws ApiException {
        LOGGER.info("Creating your account...");
        createAccount(login);
    }

    private void createOtherAccounts() throws ApiException {
        LOGGER.info("Creating accounts");

        for (String pLogin : LOGINS) {
            try {
                createAccount(pLogin);
            } catch (ApiException e) {
                LOGGER.info("Cannot create account for " + pLogin + ", might already exist");
            }
            addUserToWorkspace(pLogin);
        }
    }

    private void createGroups() throws ApiException {
        LOGGER.info("Creating groups...");

        UserGroupDTO group = new UserGroupDTO();
        group.setWorkspaceId(workspaceId);

        group.setId(GROUP_1);
        WorkspacesApi workspacesApi = new WorkspacesApi(client);
        workspacesApi.createGroup(workspaceId, group);

        group.setId(GROUP_2);
        workspacesApi.createGroup(workspaceId, group);

        group.setId(GROUP_3);
        workspacesApi.createGroup(workspaceId, group);

        group.setId(GROUP_4);
        workspacesApi.createGroup(workspaceId, group);

        group.setId(GROUP_5);
        workspacesApi.createGroup(workspaceId, group);

        int numGroup = 1;
        String groupName = "Group" + numGroup;

        for (int i = 0; i < LOGINS.length; i++) {
            UserDTO userDTO = new UserDTO();
            userDTO.setLogin(LOGINS[i]);
            if (i % 2 != 0) {//restricted to only two user by group ( one read-only and one full access )
                userDTO.setMembership(UserDTO.MembershipEnum.READ_ONLY);
                workspacesApi.addUser(workspaceId, userDTO, groupName);
                numGroup += 1;
                groupName = "Group" + numGroup;
            } else {
                userDTO.setMembership(UserDTO.MembershipEnum.FULL_ACCESS);
                workspacesApi.addUser(workspaceId, userDTO, groupName);
            }
        }

        UserDTO userDTO = new UserDTO();
        userDTO.setLogin(login);
        userDTO.setMembership(UserDTO.MembershipEnum.FULL_ACCESS);
        workspacesApi.addUser(workspaceId, userDTO, GROUP_1);
    }


    private void subscribeGroupToTag() throws ApiException {
        LOGGER.info("subscribe GROUP_1 and GROUP_2 to tag: API...");

        WorkspacesApi workspacesApi = new WorkspacesApi(client);
        List<UserGroupDTO> groupDTOs = workspacesApi.getGroups(workspaceId);
        List<TagDTO> tags = workspacesApi.getTagsInWorkspace(workspaceId);

        TagSubscriptionDTO tagSubscriptionDTO = new TagSubscriptionDTO();
        tagSubscriptionDTO.setOnIterationChange(true);
        tagSubscriptionDTO.setOnStateChange(true);

        for (UserGroupDTO ugdto : groupDTOs) {
            if (GROUP_1.equals(ugdto.getId()) || GROUP_2.equals(ugdto.getId())) {
                tagSubscriptionDTO.setTag(tags.get(0).getLabel());
                workspacesApi.updateUserGroupSubscription(workspaceId, ugdto.getId(), tagSubscriptionDTO.getTag(), tagSubscriptionDTO);
            } else if (GROUP_3.equals(ugdto.getId())) {
                tagSubscriptionDTO.setTag(tags.get(1).getLabel());
                workspacesApi.updateUserGroupSubscription(workspaceId, ugdto.getId(), tagSubscriptionDTO.getTag(), tagSubscriptionDTO);
            }
        }
    }

    private void enableUserInWorkspace() throws ApiException {
        LOGGER.info("enable user in workspace...");

        UserDTO userDTO = new UserDTO();
        WorkspacesApi wksApi = new WorkspacesApi(client);

        for (String LOGIN : LOGINS) {
            userDTO.setLogin(LOGIN);
            wksApi.enableUser(workspaceId, userDTO);
        }

        userDTO.setLogin(login);
        wksApi.enableUser(workspaceId, userDTO);
    }

    private void setAccessPermissionsForUser() throws ApiException {
        LOGGER.info("Setting the access permissions of User...");

        WorkspacesApi wksApi = new WorkspacesApi(client);

        for (int i = 0; i < LOGINS.length; i++) {
            if (i % 2 != 0) {
                UserDTO userDTO = new UserDTO();
                userDTO.setLogin(LOGINS[i]);
                userDTO.setMembership(UserDTO.MembershipEnum.READ_ONLY);
                wksApi.setUserAccess(workspaceId, userDTO);
            }
        }
    }

    private void setAccessPermissionForGroups() throws ApiException {
        LOGGER.info("Setting the access permissions of groups...");

        WorkspaceUserGroupMemberShipDTO wksGrpMemberShipDTO = new WorkspaceUserGroupMemberShipDTO();
        WorkspacesApi wksApi = new WorkspacesApi(client);

        wksGrpMemberShipDTO.setWorkspaceId(workspaceId);
        wksGrpMemberShipDTO.setMemberId(GROUP_1);
        wksGrpMemberShipDTO.setReadOnly(false);
        wksApi.setGroupAccess(workspaceId, wksGrpMemberShipDTO);

        wksGrpMemberShipDTO.setMemberId(GROUP_2);
        wksGrpMemberShipDTO.setReadOnly(false);
        wksApi.setGroupAccess(workspaceId, wksGrpMemberShipDTO);

        wksGrpMemberShipDTO.setMemberId(GROUP_3);
        wksGrpMemberShipDTO.setReadOnly(true);
        wksApi.setGroupAccess(workspaceId, wksGrpMemberShipDTO);

        wksGrpMemberShipDTO.setMemberId(GROUP_4);
        wksGrpMemberShipDTO.setReadOnly(true);
        wksApi.setGroupAccess(workspaceId, wksGrpMemberShipDTO);

        wksGrpMemberShipDTO.setMemberId(GROUP_5);
        wksGrpMemberShipDTO.setReadOnly(true);
        wksApi.setGroupAccess(workspaceId, wksGrpMemberShipDTO);
    }

    private void createAccount(String accountLogin) throws ApiException {
        LOGGER.info("Creating account [" + accountLogin + "]");

        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setName(accountLogin);

        try {
            accountDTO.setEmail(login + "@" + SampleLoaderUtils.getDomainName(url));
        } catch (URISyntaxException e) {
            LOGGER.warning("Cannot parse domain from url, using localhost as domain for email");
            accountDTO.setEmail(login + "@localhost");
        }

        accountDTO.setTimeZone("CET");
        accountDTO.setLanguage("en");
        accountDTO.setLogin(accountLogin);
        accountDTO.setNewPassword(password);
        new AccountsApi(guestClient).createAccount(accountDTO);
    }

    private void addUserToWorkspace(String pLogin) throws ApiException {
        LOGGER.info("Adding user " + pLogin + " to workspace " + workspaceId);

        UserDTO userDTO = new UserDTO();
        userDTO.setLogin(pLogin);
        new WorkspacesApi(client).addUser(workspaceId, userDTO, null);
    }

    private void createWorkspace() throws ApiException {
        LOGGER.info("Creating workspace...");

        WorkspaceDTO workspaceDTO = new WorkspaceDTO();
        workspaceDTO.setId(workspaceId);
        workspaceDTO.setDescription("Some workspaceId created from sample loader");
        new WorkspacesApi(client).createWorkspace(workspaceDTO, login);
    }


    private void createDocumentTemplates() throws ApiException {
        LOGGER.info("Creating document templates...");

        DocumentTemplateCreationDTO template = new DocumentTemplateCreationDTO();
        DocumentTemplatesApi documentTemplatesApi = new DocumentTemplatesApi(client);

        template.setWorkspaceId(workspaceId);
        template.setReference("Letter");
        template.setDocumentType("Paper");
        template.setMask("LETTER-###");
        documentTemplatesApi.createDocumentMasterTemplate(workspaceId, template);

        template.setReference("Invoice");
        template.setDocumentType("Paper");
        template.setMask("INVOICE-###");
        documentTemplatesApi.createDocumentMasterTemplate(workspaceId, template);

        template.setReference("UserManuals");
        template.setDocumentType("Documentation");
        template.setMask("USER-MAN-###");
        documentTemplatesApi.createDocumentMasterTemplate(workspaceId, template);

        template.setReference("APIDocuments");
        template.setDocumentType("APIManuals");
        template.setMask("API-###");
        documentTemplatesApi.createDocumentMasterTemplate(workspaceId, template);

        template.setReference("OfficeDocuments");
        template.setDocumentType("OfficeWriter");
        template.setMask("OFFICE-###");
        documentTemplatesApi.createDocumentMasterTemplate(workspaceId, template);

        template.setReference("SPREADSHEET");
        template.setDocumentType("SPREADSHEET");
        template.setMask("SPREADSHEET-###");
        documentTemplatesApi.createDocumentMasterTemplate(workspaceId, template);
    }

    private void createFolders() throws ApiException {
        LOGGER.info("Creating folders...");

        FoldersApi foldersApi = new FoldersApi(client);
        FolderDTO folderDTO = new FolderDTO();

        folderDTO.setName("Letters");
        foldersApi.createSubFolder(workspaceId, workspaceId, folderDTO);

        folderDTO.setName("Invoices");
        foldersApi.createSubFolder(workspaceId, workspaceId, folderDTO);

        folderDTO.setName("Documentation");
        foldersApi.createSubFolder(workspaceId, workspaceId, folderDTO);

        folderDTO.setName("APIManuals");
        foldersApi.createSubFolder(workspaceId, workspaceId, folderDTO);

        folderDTO.setName("OfficeDocuments");
        foldersApi.createSubFolder(workspaceId, workspaceId, folderDTO);
    }

    private void createTags() throws ApiException {
        LOGGER.info("Creating tags...");

        TagListDTO tagListDTO = new TagListDTO();
        List<TagDTO> tags = new ArrayList<>();
        tagListDTO.setTags(tags);

        String[] tagNames = {"internal", "important", "2018", "archive", "API"};

        for (String tagName : tagNames) {
            TagDTO tagDTO = new TagDTO();
            tagDTO.setId(tagName);
            tagDTO.setWorkspaceId(workspaceId);
            tagDTO.setLabel(tagName);
            tags.add(tagDTO);
        }

        new TagsApi(client).createTags(workspaceId, tagListDTO);
    }



    private List<ACLEntryDTO> generateACLEntries(
            ACLEntryDTO.ValueEnum group1Value,
            ACLEntryDTO.ValueEnum group2Value,
            ACLEntryDTO.ValueEnum group3Value,
            ACLEntryDTO.ValueEnum group4Value,
            ACLEntryDTO.ValueEnum group5Value) {

        List<ACLEntryDTO> acls = new ArrayList<>();

        ACLEntryDTO aclEntryDTOGrp1 = new ACLEntryDTO();
        aclEntryDTOGrp1.setKey(GROUP_1);
        aclEntryDTOGrp1.setValue(group1Value);
        acls.add(aclEntryDTOGrp1);

        ACLEntryDTO aclEntryDTOGrp2 = new ACLEntryDTO();
        aclEntryDTOGrp2.setKey(GROUP_2);
        aclEntryDTOGrp2.setValue(group2Value);
        acls.add(aclEntryDTOGrp2);

        ACLEntryDTO aclEntryDTOGrp3 = new ACLEntryDTO();
        aclEntryDTOGrp3.setKey(GROUP_3);
        aclEntryDTOGrp3.setValue(group3Value);
        acls.add(aclEntryDTOGrp3);

        ACLEntryDTO aclEntryDTOGrp4 = new ACLEntryDTO();
        aclEntryDTOGrp4.setKey(GROUP_4);
        aclEntryDTOGrp4.setValue(group4Value);
        acls.add(aclEntryDTOGrp4);

        ACLEntryDTO aclEntryDTOGrp5 = new ACLEntryDTO();
        aclEntryDTOGrp5.setKey(GROUP_5);
        aclEntryDTOGrp5.setValue(group5Value);
        acls.add(aclEntryDTOGrp5);

        return acls;
    }

    private List<ACLEntryDTO> generateACLEntriesFullAccessForGroupContainingAdmin() {
        return generateACLEntries(
                ACLEntryDTO.ValueEnum.FULL_ACCESS,
                ACLEntryDTO.ValueEnum.READ_ONLY,
                ACLEntryDTO.ValueEnum.READ_ONLY,
                ACLEntryDTO.ValueEnum.READ_ONLY,
                ACLEntryDTO.ValueEnum.FORBIDDEN
        );
    }

    private List<ACLEntryDTO> generateACLEntriesReadAccessGroup5() {
        return generateACLEntries(
                ACLEntryDTO.ValueEnum.FULL_ACCESS,
                ACLEntryDTO.ValueEnum.READ_ONLY,
                ACLEntryDTO.ValueEnum.READ_ONLY,
                ACLEntryDTO.ValueEnum.READ_ONLY,
                ACLEntryDTO.ValueEnum.READ_ONLY
        );
    }

    private List<ACLEntryDTO> generateACLEntriesFullAccessGroup1And2() {
        return generateACLEntries(
                ACLEntryDTO.ValueEnum.FULL_ACCESS,
                ACLEntryDTO.ValueEnum.FULL_ACCESS,
                ACLEntryDTO.ValueEnum.FORBIDDEN,
                ACLEntryDTO.ValueEnum.FORBIDDEN,
                ACLEntryDTO.ValueEnum.FORBIDDEN
        );
    }

    private List<ACLEntryDTO> generateACLEntriesFullAccessGroup1And3() {
        return generateACLEntries(
                ACLEntryDTO.ValueEnum.FULL_ACCESS,
                ACLEntryDTO.ValueEnum.FORBIDDEN,
                ACLEntryDTO.ValueEnum.FULL_ACCESS,
                ACLEntryDTO.ValueEnum.FORBIDDEN,
                ACLEntryDTO.ValueEnum.FORBIDDEN
        );
    }

    private void createDocuments() throws ApiException, IOException {
        LOGGER.info("Creating documents...");

        FoldersApi foldersApi = new FoldersApi(client);
        ACLDTO aclDto = new ACLDTO();
        aclDto.setGroupEntries(generateACLEntriesFullAccessGroup1And3());

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

        documentCreationDTO.setReference("OFFICE-001");
        documentCreationDTO.setTitle("My first office writer document");
        documentCreationDTO.setWorkspaceId(workspaceId);
        documentCreationDTO.setTemplateId("OfficeDocuments");
        documentCreationDTO.setDescription("An office document created with sample loader");
        documentCreationDTO.setWorkflowModelId(workflowModelDTO.getId());
        documentCreationDTO.setRoleMapping(roleMappingDTOs);

        documentCreationDTO.setAcl(aclDto);
        foldersApi.createDocumentMasterInFolder(workspaceId, documentCreationDTO, workspaceId + ":OfficeDocuments");

        documentCreationDTO.setReference("SPREADSHEET-001");
        documentCreationDTO.setTitle("My first office calcule document");
        documentCreationDTO.setWorkspaceId(workspaceId);
        documentCreationDTO.setTemplateId("SPREADSHEET");
        documentCreationDTO.setDescription("An office calcule document created with sample loader");
        documentCreationDTO.setWorkflowModelId(workflowModelDTO.getId());
        documentCreationDTO.setRoleMapping(roleMappingDTOs);

        documentCreationDTO.setAcl(aclDto);
        foldersApi.createDocumentMasterInFolder(workspaceId, documentCreationDTO, workspaceId + ":OfficeDocuments");

        documentCreationDTO.setReference("LETTER-002");
        documentCreationDTO.setTitle("My second letter");
        documentCreationDTO.setWorkspaceId(workspaceId);
        documentCreationDTO.setTemplateId("Letter");
        documentCreationDTO.setDescription("Some letter created with sample loader");
        foldersApi.createDocumentMasterInFolder(workspaceId, documentCreationDTO, workspaceId + ":Letters");

        aclDto.setGroupEntries(generateACLEntriesFullAccessForGroupContainingAdmin());
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

        aclDto.setGroupEntries(generateACLEntriesReadAccessGroup5());
        documentCreationDTO.setReference("API-001");
        documentCreationDTO.setTitle("API V1.0");
        documentCreationDTO.setWorkspaceId(workspaceId);
        documentCreationDTO.setTemplateId("APIDocuments");
        documentCreationDTO.setAcl(aclDto);
        documentCreationDTO.setDescription("First version of API description ");

        foldersApi.createDocumentMasterInFolder(workspaceId, documentCreationDTO, workspaceId + ":APIManuals");

        LOGGER.info("Uploading document files...");
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
        documentBinaryApi.uploadDocumentFiles(workspaceId, "OFFICE-001", "A", 1, SampleLoaderUtils.getFile("test_officeWriter.odt"));
        documentBinaryApi.uploadDocumentFiles(workspaceId, "SPREADSHEET-001", "A", 1, SampleLoaderUtils.getFile("spreadsheet.ods"));

        // Check in
        LOGGER.info("Checking in documents...");
        DocumentApi documentApi = new DocumentApi(client);
        documentApi.checkInDocument(workspaceId, "LETTER-001", "A");
        documentApi.checkInDocument(workspaceId, "LETTER-002", "A");
        documentApi.checkInDocument(workspaceId, "INVOICE-001", "A");
        documentApi.checkInDocument(workspaceId, "INVOICE-002", "A");
        documentApi.checkInDocument(workspaceId, "USER-MAN-001", "A");
        documentApi.checkInDocument(workspaceId, "API-001", "A");
        documentApi.checkInDocument(workspaceId, "OFFICE-001", "A");
        documentApi.checkInDocument(workspaceId, "SPREADSHEET-001", "A");
    }


    private void createMilestones() throws ApiException {
        LOGGER.info("Creating milestones...");
        
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, 15);

        MilestonesApi milestonesApi = new MilestonesApi(client);

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
        LOGGER.info("create access for milestones");
        
        MilestonesApi milestonesApi = new MilestonesApi(client);
        List<MilestoneDTO> milestoneDTOs = milestonesApi.getMilestones(workspaceId);
        ACLDTO aclDto = new ACLDTO();
        aclDto.setGroupEntries(generateACLEntriesFullAccessForGroupContainingAdmin());

        for (MilestoneDTO milestoneDTO : milestoneDTOs) {
            milestonesApi.updateMilestoneACL(workspaceId, milestoneDTO.getId(), aclDto);
            LOGGER.info("updated milestone:" + milestoneDTO.getId());
        }
    }

    private void createRequests() throws ApiException {
        LOGGER.info("Creating requests...");
        
        ChangeItemsApi changeItemsApi = new ChangeItemsApi(client);
        ChangeRequestDTO changeRequestDTO = new ChangeRequestDTO();
        changeRequestDTO.setWorkspaceId(workspaceId);

        changeRequestDTO.setName("REQ-001");
        changeRequestDTO.setDescription("Something needs to be corrected");
        changeRequestDTO.setCategory(ChangeRequestDTO.CategoryEnum.CORRECTIVE);
        changeRequestDTO.setAssignee("joe");
        changeItemsApi.createRequest(workspaceId, changeRequestDTO);

        changeRequestDTO.setName("REQ-002");
        changeRequestDTO.setDescription("Something needs to be perfected");
        changeRequestDTO.setAssignee("bill");
        changeRequestDTO.setCategory(ChangeRequestDTO.CategoryEnum.PERFECTIVE);
        changeItemsApi.createRequest(workspaceId, changeRequestDTO);
    }

    private void createIssues() throws ApiException {
        LOGGER.info("Creating issues...");
        
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
        LOGGER.info("create access for issues");
        
        ChangeItemsApi changeItemApi = new ChangeItemsApi(client);
        List<ChangeIssueDTO> changeIssueDTOs = changeItemApi.getIssues(workspaceId);
        ACLDTO aclDto = new ACLDTO();
        aclDto.setGroupEntries(generateACLEntriesFullAccessForGroupContainingAdmin());
        
        for (ChangeIssueDTO changeIssueDTO : changeIssueDTOs) {
            changeItemApi.updateChangeIssueACL(workspaceId, changeIssueDTO.getId(), aclDto);
        }
    }

    private void setRequestsAcl() throws ApiException {
        LOGGER.info("create access for request");
        
        ChangeItemsApi changeItemApi = new ChangeItemsApi(client);
        List<ChangeRequestDTO> changeRequestDTOs = changeItemApi.getRequests(workspaceId);
        ACLDTO aclDto = new ACLDTO();
        aclDto.setGroupEntries(generateACLEntriesFullAccessForGroupContainingAdmin());
        
        for (ChangeRequestDTO changeRequestDTO : changeRequestDTOs) {
            changeItemApi.updateChangeRequestACL(workspaceId, changeRequestDTO.getId(), aclDto);
        }
    }

    private void setOrdersAcl() throws ApiException {
        LOGGER.info("create access for orders");
        
        ChangeItemsApi changeItemApi = new ChangeItemsApi(client);
        List<ChangeOrderDTO> changeOrderDTOs = changeItemApi.getOrders(workspaceId);
        ACLDTO aclDto = new ACLDTO();
        aclDto.setGroupEntries(generateACLEntriesFullAccessForGroupContainingAdmin());
        
        for (ChangeOrderDTO changeOrderDTO : changeOrderDTOs) {
            changeItemApi.updateChangeOrderACL(workspaceId, changeOrderDTO.getId(), aclDto);
        }
    }


    private void updateAffectedPartInOrder() throws ApiException {
        LOGGER.info("Affect some parts to orders");
        
        ChangeItemsApi changeItemApi = new ChangeItemsApi(client);
        List<ChangeOrderDTO> changeOrderDTOs = changeItemApi.getOrders(workspaceId);

        WorkspacesApi workspacesApi = new WorkspacesApi(client);

        PartRevisionDTO wheelRevision = workspacesApi.getLatestPartRevision(workspaceId, "WHEEL-001");
        PartIterationDTO wheelIteration = LastIterationHelper.getLastIteration(wheelRevision);

        //affect parts
        PartRevisionDTO amortizerRevision = new WorkspacesApi(client).getLatestPartRevision(workspaceId, "AMORTIZER-001");
        PartIterationDTO amortizerIteration = LastIterationHelper.getLastIteration(amortizerRevision);

        List<PartIterationDTO> iterationDTOs = new ArrayList<>();
        iterationDTOs.add(amortizerIteration);
        iterationDTOs.add(wheelIteration);

        PartIterationListDTO partIterationListDTO = new PartIterationListDTO();
        partIterationListDTO.setParts(iterationDTOs);

        for (ChangeOrderDTO changeOrderDTO : changeOrderDTOs) {
            changeItemApi.saveChangeOrderAffectedParts(workspaceId, changeOrderDTO.getId(), partIterationListDTO);
        }

        LOGGER.info("Affect some requests to orders");
        //affect request
        List<ChangeRequestDTO> changeRequestDTOs = workspacesApi.getRequests(workspaceId);
        ChangeRequestListDTO changeRequestListDTO = new ChangeRequestListDTO();
        changeRequestListDTO.setRequests(changeRequestDTOs);

        for (ChangeOrderDTO changeOrderDTO : changeOrderDTOs) {
            changeItemApi.saveAffectedRequests(workspaceId, changeOrderDTO.getId(), changeRequestListDTO);
        }
    }

    private void createOrders() throws ApiException {
        LOGGER.info("Creating orders...");

        ChangeItemsApi changeItemsApi = new ChangeItemsApi(client);
        ChangeOrderDTO changeOrderDTO = new ChangeOrderDTO();
        changeOrderDTO.setWorkspaceId(workspaceId);

        changeOrderDTO.setName("ORDER-001");
        changeOrderDTO.setDescription("Order for some documents");
        changeOrderDTO.setCategory(ChangeOrderDTO.CategoryEnum.PERFECTIVE);
        changeOrderDTO.setAssignee("mickey");
        changeOrderDTO.setPriority(ChangeOrderDTO.PriorityEnum.EMERGENCY);
        changeItemsApi.createOrder(workspaceId, changeOrderDTO);

        changeOrderDTO.setName("ORDER-002");
        changeOrderDTO.setDescription("Order for some parts");
        changeOrderDTO.setAssignee("rob");
        changeOrderDTO.setPriority(ChangeOrderDTO.PriorityEnum.MEDIUM);
        changeOrderDTO.setCategory(ChangeOrderDTO.CategoryEnum.OTHER);
        changeItemsApi.createOrder(workspaceId, changeOrderDTO);
    }

    private void setWorkFlowACL() throws ApiException {
        LOGGER.info("Setting acl for created workflows...");

        WorkflowModelsApi workflowModelsApi = new WorkflowModelsApi(client);
        List<WorkflowModelDTO> workflowModelDTOs = workflowModelsApi.getWorkflowModelsInWorkspace(workspaceId);
        ACLDTO aclDto = new ACLDTO();
        aclDto.setGroupEntries(generateACLEntriesFullAccessForGroupContainingAdmin());

        for (WorkflowModelDTO workflowModelDTO : workflowModelDTOs) {
            workflowModelsApi.updateWorkflowModelACL(workspaceId, workflowModelDTO.getId(), aclDto);
            LOGGER.info("workflow with id " + workflowModelDTO.getId() + " was updated...");
        }
    }

    private void createRolesAndWorkflow() throws ApiException {
        LOGGER.info("Creating roles...");

        RolesApi rolesApi = new RolesApi(client);
        // Roles
        List<UserDTO> designers = new ArrayList<>();
        List<UserDTO> technicians = new ArrayList<>();
        List<UserGroupDTO> groupsAvailable = new WorkspacesApi(client).getGroups(workspaceId);

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
        List<UserGroupDTO> tmpArrays = new ArrayList<>();

        roleGroupDTO.setDefaultAssignedUsers(null);
        tmpArrays.add(groupsAvailable.get(0));

        roleGroupDTO.setName("ceo");
        roleGroupDTO.setDefaultAssignedGroups(tmpArrays);
        RoleDTO ceo = rolesApi.createRole(workspaceId, roleGroupDTO);

        tmpArrays = new ArrayList<>();
        tmpArrays.add(groupsAvailable.get(1));
        tmpArrays.add(groupsAvailable.get(2));

        roleGroupDTO.setName("engineers");
        roleGroupDTO.setDefaultAssignedGroups(tmpArrays);
        RoleDTO engineers = rolesApi.createRole(workspaceId, roleGroupDTO);

        tmpArrays = new ArrayList<>();
        tmpArrays.add(groupsAvailable.get(3));

        roleGroupDTO.setName("support");
        roleGroupDTO.setDefaultAssignedGroups(tmpArrays);
        RoleDTO support = rolesApi.createRole(workspaceId, roleGroupDTO);

        // Workflow
        LOGGER.info("Creating workflow...");

        TaskModelDTO firstTask = new TaskModelDTO();
        firstTask.setNum(0);
        firstTask.setTitle("Organise Milestones");
        firstTask.setInstructions("Check customer's requests and plan a workshop with engineers");
        firstTask.setRole(ceo);

        TaskModelDTO secondTask = new TaskModelDTO();
        secondTask.setNum(1);
        secondTask.setTitle("Build architecture diagrams");
        secondTask.setInstructions("Build the prototype and validate the task");
        secondTask.setRole(engineers);

        TaskModelDTO thirdTask = new TaskModelDTO();
        thirdTask.setNum(2);
        thirdTask.setTitle("Start first iteration");
        thirdTask.setInstructions("Plan a workshop with technicians and define next iterations");
        thirdTask.setRole(technicianRole);

        TaskModelDTO fourthTask = new TaskModelDTO();
        fourthTask.setNum(3);
        fourthTask.setTitle("Design some prototypes");
        fourthTask.setInstructions("Create a new prototype design, then validate the task");
        fourthTask.setRole(designerRole);

        TaskModelDTO fifthTask = new TaskModelDTO();
        fifthTask.setNum(4);
        fifthTask.setTitle("Project review");
        fifthTask.setInstructions("Run quality assurance phase");
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
        LOGGER.info("Creating part templates...");

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

        partTemplateCreationDTO.setReference("WHEEL");
        partTemplateCreationDTO.setMask("WHEEL-###");
        partTemplateCreationDTO.setAttributeTemplates(attributes);
        partTemplatesApi.createPartMasterTemplate(workspaceId, partTemplateCreationDTO);

        partTemplateCreationDTO.setReference("AMORTIZER");
        partTemplateCreationDTO.setMask("AMORTIZER-###");
        partTemplateCreationDTO.setAttributeTemplates(attributes);
        partTemplatesApi.createPartMasterTemplate(workspaceId, partTemplateCreationDTO);
    }


    private void createCarProduct() throws ApiException, IOException, InterruptedException {
        LOGGER.info("Creating car product...");

        PartsApi partsApi = new PartsApi(client);
        ProductsApi productsApi = new ProductsApi(client);
        PartCreationDTO part = new PartCreationDTO();

        part.setWorkspaceId(workspaceId);
        part.setDescription("Sample part create with sample loader");

        //ACLS set up
        ACLDTO aclDto = new ACLDTO();
        aclDto.setGroupEntries(generateACLEntriesFullAccessGroup1And2());

        //Workflow model creation
        WorkflowModelDTO workflowModelDTO = new WorkspacesApi(client)
                .getWorkflowModelInWorkspace(workspaceId, "My first workflow");
        List<RoleMappingDTO> roleMappingDTOs = resolveDefaultRoles(workflowModelDTO);

        //Parts creations
        part.setTemplateId("SEATS");
        part.setNumber("SEAT-010");
        part.setName("Front seat");
        part.setWorkflowModelId(workflowModelDTO.getId());
        part.setRoleMapping(roleMappingDTOs);
        part.setAcl(aclDto);
        PartRevisionDTO frontSeat = partsApi.createNewPart(workspaceId, part);
        addAttributes(partsApi, frontSeat);
        part.setNumber("SEAT-020");
        part.setName("Back seat");
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
        String assemblyNumber = "CAR-001";
        part.setNumber(assemblyNumber);
        part.setName("Car assembly");
        part.setTemplateId(null);
        PartRevisionDTO assembly = partsApi.createNewPart(workspaceId, part);
        PartIterationDTO lastIteration = LastIterationHelper.getLastIteration(assembly);

        List<PartUsageLinkDTO> links = new ArrayList<>();

        PartUsageLinkDTO driverSeatLink = new PartUsageLinkDTO();
        PartUsageLinkDTO backPassengersSeatsLink = new PartUsageLinkDTO();
        PartUsageLinkDTO passengerSeatLink = new PartUsageLinkDTO();
        PartUsageLinkDTO engineLink = new PartUsageLinkDTO();

        ComponentDTO seat = new ComponentDTO();
        seat.setNumber("SEAT-010");
        seat.setVersion("A");

        ComponentDTO seat2 = new ComponentDTO();
        seat2.setNumber("SEAT-020");
        seat2.setVersion("A");

        ComponentDTO engine = new ComponentDTO();
        engine.setNumber("ENGINE-050");
        engine.setVersion("A");

        ComponentDTO engine2 = new ComponentDTO();
        engine2.setNumber("ENGINE-100");
        engine2.setVersion("A");

        driverSeatLink.setComponent(seat);
        driverSeatLink.setAmount(1.0);
        driverSeatLink.setReferenceDescription("Driver seat");
        driverSeatLink.setOptional(false);

        passengerSeatLink.setComponent(seat);
        passengerSeatLink.setAmount(1.0);
        passengerSeatLink.setReferenceDescription("Passenger seat");
        passengerSeatLink.setOptional(false);

        backPassengersSeatsLink.setComponent(seat2);
        backPassengersSeatsLink.setAmount(2.0);
        backPassengersSeatsLink.setReferenceDescription("Back seats");
        backPassengersSeatsLink.setOptional(true);

        PartSubstituteLinkDTO engineSubstitute = new PartSubstituteLinkDTO();
        engineSubstitute.setSubstitute(engine2);
        engineSubstitute.setAmount(1.0);
        engineLink.setReferenceDescription("Engine");

        engineLink.setComponent(engine);
        engineLink.setAmount(1.0);
        engineLink.setReferenceDescription("The engine of this car");
        engineLink.setOptional(false);
        engineLink.setSubstitutes(Collections.singletonList(engineSubstitute));

        links.add(backPassengersSeatsLink);
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
        passengerSeatCadInstance.setTx(0.0);
        passengerSeatCadInstance.setTy(0.0);
        passengerSeatCadInstance.setTz(0.0);

        CADInstanceDTO engineCadInstance = new CADInstanceDTO();
        engineCadInstance.setRx(0.0);
        engineCadInstance.setRy(0.0);
        engineCadInstance.setRz(0.0);
        engineCadInstance.setTx(0.0);
        engineCadInstance.setTy(0.0);
        engineCadInstance.setTz(0.0);

        List<CADInstanceDTO> driverSeatCadInstances = new ArrayList<>();
        driverSeatCadInstances.add(driverSeatCadInstance);
        driverSeatLink.setCadInstances(driverSeatCadInstances);

        List<CADInstanceDTO> backSeatsCadInstances = new ArrayList<>();
        backSeatsCadInstances.add(driverSeatCadInstance);
        backPassengersSeatsLink.setCadInstances(backSeatsCadInstances);

        List<CADInstanceDTO> passengerSeatCadInstances = new ArrayList<>();
        passengerSeatCadInstances.add(passengerSeatCadInstance);
        passengerSeatLink.setCadInstances(passengerSeatCadInstances);

        List<CADInstanceDTO> engineCadInstances = new ArrayList<>();
        engineCadInstances.add(engineCadInstance);
        engineLink.setCadInstances(engineCadInstances);
        engineSubstitute.setCadInstances(engineCadInstances);

        lastIteration.setComponents(links);
        lastIteration.setIterationNote("Creating assembly");

        partsApi.updatePartIteration(workspaceId, assemblyNumber, "A", 1, lastIteration);

        // Upload 3D files
        LOGGER.info("Uploading 3D files...");

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

        LOGGER.info("Waiting for conversion...");
        // Let the conversion finish
        Thread.sleep(SLEEPTIME);

        LOGGER.info("Checking in parts...");

        PartApi partApi = new PartApi(client);
        partApi.checkIn(workspaceId, "SEAT-010", "A");
        partApi.checkIn(workspaceId, "SEAT-020", "A");
        partApi.checkIn(workspaceId, "ENGINE-050", "A");
        partApi.checkIn(workspaceId, "ENGINE-100", "A");
        partApi.checkIn(workspaceId, "CAR-001", "A");

        LOGGER.info("Creating product...");

        ConfigurationItemDTO configurationItemDTO = new ConfigurationItemDTO();
        configurationItemDTO.setWorkspaceId(workspaceId);
        configurationItemDTO.setDesignItemNumber("CAR-001");
        configurationItemDTO.setId("CAR-001");

        productsApi.createConfigurationItem(workspaceId, configurationItemDTO);
    }

    private void createNewVersionsAndReleasedParts() throws ApiException {
        LOGGER.info("Checking in parts...");

        PartCreationDTO part = new PartCreationDTO();
        part.setWorkspaceId(workspaceId);
        part.setDescription("Sample part create with sample loader");
        ACLDTO aclDto = new ACLDTO();
        aclDto.setGroupEntries(generateACLEntriesFullAccessGroup1And2());
        WorkflowModelDTO workflowModelDTO = new WorkspacesApi(client)
                .getWorkflowModelInWorkspace(workspaceId, "My first workflow");
        List<RoleMappingDTO> roleMappingDTOs = resolveDefaultRoles(workflowModelDTO);
        part.setWorkflowModelId(workflowModelDTO.getId());
        part.setRoleMapping(roleMappingDTOs);
        part.setAcl(aclDto);

        PartApi partApi = new PartApi(client);
        PartsApi partsApi = new PartsApi(client);

        part.setTemplateId("SEATS");
        part.setNumber("SEAT-010");
        part.setName("Front seat");
        partsApi.createNewPartVersion(workspaceId, "SEAT-010", "A", part);
        part.setNumber("SEAT-020");
        part.setName("Back seat");
        partsApi.createNewPartVersion(workspaceId, "SEAT-020", "A", part);
        part.setTemplateId("ENGINES");
        part.setNumber("ENGINE-050");
        part.setName("50cc engine");
        partsApi.createNewPartVersion(workspaceId, "ENGINE-050", "A", part);
        part.setNumber("ENGINE-100");
        part.setName("100cc engine");
        partsApi.createNewPartVersion(workspaceId, "ENGINE-100", "A", part);
        part.setNumber("CAR-001");
        part.setName("Car assembly");
        part.setTemplateId(null);
        partsApi.createNewPartVersion(workspaceId, "CAR-001", "A", part);

        partApi.releasePartRevision(workspaceId, "SEAT-010", "A");
        partApi.releasePartRevision(workspaceId, "SEAT-020", "A");
        partApi.releasePartRevision(workspaceId, "ENGINE-050", "A");
        partApi.releasePartRevision(workspaceId, "ENGINE-100", "A");
        partApi.releasePartRevision(workspaceId, "CAR-001", "A");

        partApi.checkIn(workspaceId, "SEAT-010", "B");
        partApi.checkIn(workspaceId, "SEAT-020", "B");
        partApi.checkIn(workspaceId, "ENGINE-050", "B");
        partApi.checkIn(workspaceId, "ENGINE-100", "B");
        partApi.checkIn(workspaceId, "CAR-001", "B");

        partApi.releasePartRevision(workspaceId, "SEAT-010", "B");
        partApi.releasePartRevision(workspaceId, "SEAT-020", "B");
        partApi.releasePartRevision(workspaceId, "ENGINE-050", "B");
        partApi.releasePartRevision(workspaceId, "ENGINE-100", "B");
        partApi.releasePartRevision(workspaceId, "CAR-001", "B");

        partsApi.createNewPartVersion(workspaceId, "CAR-001", "B", part);
        partsApi.createNewPartVersion(workspaceId, "ENGINE-100", "B", part);
        partsApi.createNewPartVersion(workspaceId, "ENGINE-050", "B", part);
        partsApi.createNewPartVersion(workspaceId, "SEAT-020", "B", part);
        partsApi.createNewPartVersion(workspaceId, "SEAT-010", "B", part);

        partApi.checkIn(workspaceId, "SEAT-010", "C");
        partApi.checkIn(workspaceId, "SEAT-020", "C");
        partApi.checkIn(workspaceId, "ENGINE-050", "C");
        partApi.checkIn(workspaceId, "ENGINE-100", "C");
        partApi.checkIn(workspaceId, "CAR-001", "C");
    }

    private void createEffectivities() throws ApiException {
        LOGGER.info("Creating effectivities...");

        PartsApi partsApi = new PartsApi(client);
        ConfigurationItemKey configurationItemKey = new ConfigurationItemKey();
        configurationItemKey.setId("CAR-001");
        configurationItemKey.setWorkspace(workspaceId);

        EffectivityDTO effectivityDTO;
        effectivityDTO = new EffectivityDTO();
        effectivityDTO.setConfigurationItemKey(configurationItemKey);
        effectivityDTO.setDescription("Generated effectivity by tests");
        effectivityDTO.setTypeEffectivity(EffectivityDTO.TypeEffectivityEnum.DATEBASEDEFFECTIVITY);

        effectivityDTO.setName("Effect date 1");
        Calendar c = Calendar.getInstance();
        c.add(Calendar.YEAR, -2);
        effectivityDTO.setStartDate(c.getTime());
        c.add(Calendar.YEAR, 1);
        effectivityDTO.setEndDate(c.getTime());

        partsApi.createEffectivity(effectivityDTO, workspaceId, "SEAT-010", "A");
        partsApi.createEffectivity(effectivityDTO, workspaceId, "SEAT-020", "A");
        partsApi.createEffectivity(effectivityDTO, workspaceId, "ENGINE-050", "A");
        partsApi.createEffectivity(effectivityDTO, workspaceId, "ENGINE-100", "A");

        effectivityDTO.setName("Effect date 2");
        effectivityDTO.setStartDate(c.getTime());
        c.add(Calendar.YEAR, 4);
        effectivityDTO.setEndDate(c.getTime());

        partsApi.createEffectivity(effectivityDTO, workspaceId, "SEAT-010", "B");
        partsApi.createEffectivity(effectivityDTO, workspaceId, "SEAT-020", "B");
        partsApi.createEffectivity(effectivityDTO, workspaceId, "ENGINE-050", "B");
        partsApi.createEffectivity(effectivityDTO, workspaceId, "ENGINE-100", "B");
    }

    private void addAttributes(PartsApi partsApi, PartRevisionDTO partRevision) throws ApiException {
        PartIterationDTO lastIteration = LastIterationHelper.getLastIteration(partRevision);

        // Define weight and price
        List<InstanceAttributeDTO> attributes = lastIteration.getInstanceAttributes();

        if (attributes.size() == 2) {
            attributes.get(0).setValue(String.valueOf(Math.random() * 20));
            attributes.get(1).setValue(String.valueOf(Math.random() * 20));
        } else {
            LOGGER.warning("Attributes have not been found");
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

    private void createBaseline() throws ApiException {
        ProductBaselineApi productBaselineApi = new ProductBaselineApi(client);
        ProductBaselineCreationDTO baseline = new ProductBaselineCreationDTO();
        baseline.setConfigurationItemId("CAR-001");

        baseline.setName("Basic");
        baseline.setType(ProductBaselineCreationDTO.TypeEnum.RELEASED);
        productBaselineApi.createProductBaseline(workspaceId, baseline, false);
        baseline.setName("Medium");
        baseline.setType(ProductBaselineCreationDTO.TypeEnum.RELEASED);

        List<String> links = new ArrayList<>();

        PartApi partApi = new PartApi(client);
        PartRevisionDTO partRevisionDTO = partApi.getPartRevision(workspaceId, "CAR-001", "B");
        PartIterationDTO lastIteration = LastIterationHelper.getLastIteration(partRevisionDTO);

        for (PartUsageLinkDTO puldto : lastIteration.getComponents()) {
            if ("SEAT-020".equals(puldto.getComponent().getNumber())) {
                links.add("-1-" + puldto.getFullId());
            }
        }

        baseline.setOptionalUsageLinks(links);

        productBaselineApi.createProductBaseline(workspaceId, baseline, false);
    }

    private void createProductInstance() throws ApiException {
        ProductInstancesApi productInstancesApi = new ProductInstancesApi(client);

        List<ProductBaselineDTO> baselines = new ProductBaselineApi(client).getProductBaselinesForProduct(workspaceId, "CAR-001");
        ProductBaselineDTO firstBaselineFound = baselines.get(0);
        ProductBaselineDTO secondBaselineFound = baselines.get(1);

        for (int i = 0; i < 10; i++) {
            ProductInstanceCreationDTO productInstance = new ProductInstanceCreationDTO();

            productInstance.setConfigurationItemId("CAR-001");
            productInstance.setSerialNumber(String.format("CB9025-%08d", i + 1));
            productInstance.setBaselineId(firstBaselineFound.getId());
            productInstancesApi.createProductInstanceMaster(workspaceId, productInstance);

            productInstance.setConfigurationItemId("CAR-001");
            productInstance.setSerialNumber(String.format("CM9095-%08d", i + 1));
            productInstance.setBaselineId(secondBaselineFound.getId());
            productInstancesApi.createProductInstanceMaster(workspaceId, productInstance);
        }
    }

    private void createConfiguration() throws ApiException {
        LOGGER.info("Creating configuration...");

        ProductConfigurationsApi productConfigurationsApi = new ProductConfigurationsApi(client);

        List<String> useOptionalLinks = new ArrayList<>();
        ACLDTO aclDto = new ACLDTO();
        aclDto.setGroupEntries(generateACLEntriesFullAccessForGroupContainingAdmin());

        PartRevisionDTO doorRevisionDto = new PartApi(client).getPartRevision(workspaceId, "DOOR-001", "A");
        PartIterationDTO doorIterationDto = LastIterationHelper.getLastIteration(doorRevisionDto);

        for (PartUsageLinkDTO puldto : doorIterationDto.getComponents()) {

            useOptionalLinks.add("-1-" + puldto.getFullId());
        }

        ProductConfigurationDTO productConfigurationDTO = new ProductConfigurationDTO();
        productConfigurationDTO.setName("cfg-001");
        productConfigurationDTO.setConfigurationItemId("DOOR-001");
        productConfigurationDTO.setDescription("configuration created from sample");
        productConfigurationDTO.setOptionalUsageLinks(useOptionalLinks);
        productConfigurationDTO.setAcl(aclDto);
        productConfigurationsApi.createConfiguration(workspaceId, productConfigurationDTO);
    }


    private void createDoorProduct() throws ApiException, IOException, InterruptedException {
        LOGGER.info("Creating the door product...");

        PartsApi partsApi = new PartsApi(client);
        PartApi partApi = new PartApi(client);
        String[] partsNumber = {"DOOR-001", "WHEEL-001", "AMORTIZER-001"};

        //ACLS set up
        ACLDTO aclDto = new ACLDTO();
        aclDto.setGroupEntries(generateACLEntriesFullAccessGroup1And2());

        //Workflow model creation
        WorkflowModelDTO workflowModelDTO = new WorkspacesApi(client)
                .getWorkflowModelInWorkspace(workspaceId, "Workflow-door-creation");
        List<RoleMappingDTO> roleMappingDTOs = resolveDefaultRoles(workflowModelDTO);

        //Create Parts for door structure
        PartCreationDTO partCreationDTO = new PartCreationDTO();
        List<String> useOptionalLinks = new ArrayList<>();

        partCreationDTO.setTemplateId("DOOR");
        partCreationDTO.setNumber(partsNumber[0]);
        partCreationDTO.setName("Door part");
        partCreationDTO.setVersion("A");
        partCreationDTO.setWorkflowModelId(workflowModelDTO.getId());
        partCreationDTO.setRoleMapping(roleMappingDTOs);
        partCreationDTO.setAcl(aclDto);

        PartRevisionDTO leftDoor = partsApi.createNewPart(workspaceId, partCreationDTO);
        addAttributes(partsApi, leftDoor);

        partCreationDTO.setTemplateId("WHEEL");
        partCreationDTO.setName("Wheel part");
        partCreationDTO.setNumber(partsNumber[1]);
        partCreationDTO.setDescription("Left front wheel");

        PartRevisionDTO leftWindow = partsApi.createNewPart(workspaceId, partCreationDTO);
        addAttributes(partsApi, leftWindow);

        partCreationDTO.setTemplateId("AMORTIZER");
        partCreationDTO.setName("Amortizer part");
        partCreationDTO.setNumber(partsNumber[2]);
        partCreationDTO.setDescription("Left front amortizer");

        PartRevisionDTO leftLock = partsApi.createNewPart(workspaceId, partCreationDTO);
        addAttributes(partsApi, leftLock);

        //Create structure product
        // 1 - DOOR
        //     1.1 - LOCK
        //     1.2 - WINDOW

        PartRevisionDTO doorRevisionDto = partApi.getPartRevision(workspaceId, partsNumber[0], "A");
        PartIterationDTO doorIterationDto = LastIterationHelper.getLastIteration(doorRevisionDto);

        List<PartUsageLinkDTO> components = new ArrayList<>();
        PartUsageLinkDTO windowLeftLink = new PartUsageLinkDTO();

        ComponentDTO windowLeftComponent = new ComponentDTO();
        windowLeftComponent.setNumber(partsNumber[1]);
        windowLeftComponent.setAmount(1.0);
        windowLeftComponent.setVersion("A");
        windowLeftComponent.setPartUsageLinkReferenceDescription("left front wheel");
        windowLeftComponent.setDescription("left front wheel");
        windowLeftLink.setComponent(windowLeftComponent);
        windowLeftLink.setReferenceDescription("Left front wheel");
        windowLeftLink.setAmount(1.0);
        windowLeftLink.setOptional(true);

        ComponentDTO windowRightComponent = new ComponentDTO();
        PartUsageLinkDTO windowRightLink = new PartUsageLinkDTO();
        windowRightComponent.setNumber(partsNumber[1]);
        windowRightComponent.setAmount(1.0);
        windowRightComponent.setVersion("A");
        windowRightComponent.setPartUsageLinkReferenceDescription("Right front wheel");
        windowLeftComponent.setDescription("Right front wheel");
        windowRightLink.setComponent(windowLeftComponent);
        windowRightLink.setAmount(1.0);
        windowRightLink.setReferenceDescription("Right front wheel");
        windowRightLink.setOptional(true);

        PartUsageLinkDTO lockLeftLink = new PartUsageLinkDTO();
        ComponentDTO lockLeftComponent = new ComponentDTO();

        lockLeftComponent.setNumber(partsNumber[2]);
        lockLeftComponent.setAmount(1.0);
        lockLeftComponent.setVersion("A");
        lockLeftComponent.setPartUsageLinkReferenceDescription("Left front amortizer");
        lockLeftLink.setAmount(1.0);
        lockLeftLink.setComponent(lockLeftComponent);
        lockLeftLink.setOptional(true);
        lockLeftLink.setReferenceDescription("Left front amortizer");

        PartUsageLinkDTO lockRightLink = new PartUsageLinkDTO();
        ComponentDTO lockRightComponent = new ComponentDTO();

        lockRightComponent.setNumber(partsNumber[2]);
        lockRightComponent.setAmount(1.0);
        lockRightComponent.setVersion("A");
        lockRightComponent.setPartUsageLinkReferenceDescription("Right front amortizer");
        lockRightLink.setAmount(1.0);
        lockRightLink.setComponent(lockRightComponent);
        lockRightLink.setOptional(true);
        lockRightLink.setReferenceDescription("Right front amortizer");


        CADInstanceDTO windowsCadInstance = new CADInstanceDTO();
        windowsCadInstance.setRx(0.0);
        windowsCadInstance.setRy(0.0);
        windowsCadInstance.setRz(0.0);
        windowsCadInstance.setTx(20.0);
        windowsCadInstance.setTy(0.0);
        windowsCadInstance.setTz(0.0);

        CADInstanceDTO lockCadInstance = new CADInstanceDTO();
        lockCadInstance.setRx(0.0);
        lockCadInstance.setRy(0.0);
        lockCadInstance.setRz(0.0);
        lockCadInstance.setTx(0.0);
        lockCadInstance.setTy(0.0);
        lockCadInstance.setTz(-50.0);

        List<CADInstanceDTO> windowsCadInstances = new ArrayList<>();
        windowsCadInstances.add(windowsCadInstance);
        windowLeftLink.setCadInstances(windowsCadInstances);
        windowRightLink.setCadInstances(windowsCadInstances);

        List<CADInstanceDTO> lockCadInstances = new ArrayList<>();
        lockCadInstances.add(lockCadInstance);
        lockLeftLink.setCadInstances(lockCadInstances);
        lockRightLink.setCadInstances(lockCadInstances);

        components.add(windowLeftLink);
        components.add(windowRightLink);
        components.add(lockLeftLink);
        components.add(lockRightLink);

        doorIterationDto.setComponents(components);
        partApi.updatePartIteration(workspaceId, partsNumber[0], "A", 1, doorIterationDto);

        LOGGER.info("Uploading 3D files...");

        doorIterationDto.setNumber(partsNumber[1]);
        uploadNativeCADFile(doorIterationDto, client, SampleLoaderUtils.getFile("BassBoat-TrollingMotor.obj"));
        uploadAttachedFile(doorIterationDto, client, SampleLoaderUtils.getFile("BassBoat-FrontSeat.mtl"));

        doorIterationDto.setNumber(partsNumber[2]);
        uploadNativeCADFile(doorIterationDto, client, SampleLoaderUtils.getFile("BassBoat-OutboardMotor.obj"));
        uploadAttachedFile(doorIterationDto, client, SampleLoaderUtils.getFile("BassBoat-BackSeat.mtl"));

        for (String s : partsNumber) {
            partApi.checkIn(workspaceId, s, "A");
        }

        ProductsApi productsApi = new ProductsApi(client);

        //Create the product
        ConfigurationItemDTO product = new ConfigurationItemDTO();
        product.setId(partsNumber[0]);
        product.setDesignItemNumber(partsNumber[0]);
        product.setDescription("Generated from sample data for test");
        product.setWorkspaceId(workspaceId);

        productsApi.createConfigurationItem(workspaceId, product);

        doorRevisionDto = partApi.getPartRevision(workspaceId, partsNumber[0], "A");
        doorIterationDto = LastIterationHelper.getLastIteration(doorRevisionDto);

        //Create the baseline
        ProductBaselineCreationDTO baseline = new ProductBaselineCreationDTO();
        baseline.setType(ProductBaselineCreationDTO.TypeEnum.LATEST);
        baseline.setName("DOOR-BASELINE");
        baseline.setConfigurationItemId(product.getId());

        for (PartUsageLinkDTO puldto : doorIterationDto.getComponents()) {

            useOptionalLinks.add("-1-" + puldto.getFullId());
        }
        baseline.setOptionalUsageLinks(useOptionalLinks);
        baseline.setSubstituteLinks(useOptionalLinks);

        ComponentDTO structure = productsApi.filterProductStructure(workspaceId,
                product.getId(), "wip", "-1", -1, null, true);

        List<ComponentDTO> structureComponents = structure.getComponents();

        //Create a typed links
        LightPathToPathLinkDTO link = new LightPathToPathLinkDTO();
        link.setType("Mechanical");
        link.setDescription("a typed link created from sample data");
        link.setSourcePath(structureComponents.get(0).getPath());
        link.setTargetPath(structureComponents.get(1).getPath());
        productsApi.createPathToPathLink(workspaceId, product.getId(), link);

        new ProductBaselineApi(client).createProductBaseline(workspaceId, baseline, false);
    }

    private void createRolesAndWorkflowForDoorProduct() throws ApiException {
        LOGGER.info("Creating roles for door product...");

        RolesApi rolesApi = new RolesApi(client);
        List<UserGroupDTO> groupsAvailable = new WorkspacesApi(client).getGroups(workspaceId);

        List<UserGroupDTO> tmpArrays = new ArrayList<>();
        tmpArrays.add(groupsAvailable.get(1));
        tmpArrays.add(groupsAvailable.get(2));

        RoleDTO roleGroupDTO = new RoleDTO();
        roleGroupDTO.setWorkspaceId(workspaceId);
        roleGroupDTO.setName("Assembly engineers");
        roleGroupDTO.setDefaultAssignedGroups(tmpArrays);
        RoleDTO engineers = rolesApi.createRole(workspaceId, roleGroupDTO);

        // Workflow
        LOGGER.info("Setting workflow...");

        TaskModelDTO firstTask = new TaskModelDTO();
        firstTask.setNum(0);
        firstTask.setTitle("design door prototype");
        firstTask.setInstructions("Create door's design");
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
        fifthTask.setTitle("Build window");
        fifthTask.setInstructions("Build window");
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

        LOGGER.info("Creating workflow...");

        WorkflowModelDTO workflowModelDTO = new WorkflowModelDTO();
        workflowModelDTO.setActivityModels(activities);
        workflowModelDTO.setReference("Workflow-door-creation");
        workflowModelDTO.setFinalLifeCycleState("Terminated");
        workflowModelDTO.setId("Workflow-door-creation");

        new WorkflowModelsApi(client).createWorkflowModel(workspaceId, workflowModelDTO);
    }

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
        LOGGER.info("Checking out some parts and documents...");

        PartApi partApi = new PartApi(client);
        partApi.checkOut(workspaceId, "DOOR-001", "A");
        partApi.checkOut(workspaceId, "WHEEL-001", "A");
        partApi.checkOut(workspaceId, "AMORTIZER-001", "A");

        ApiClient joe = DocDokuPLMClientFactory.createJWTClient(url, "joe", "test");
        partApi.setApiClient(joe);

        partApi.checkOut(workspaceId, "CAR-001", "C");
        partApi.checkOut(workspaceId, "ENGINE-100", "C");

        ApiClient rob = DocDokuPLMClientFactory.createJWTClient(url, "rob", "test");
        partApi.setApiClient(rob);
        partApi.checkOut(workspaceId, "ENGINE-050", "C");
        partApi.checkOut(workspaceId, "SEAT-010", "C");

        ApiClient steve = DocDokuPLMClientFactory.createJWTClient(url, "steve", "test");
        partApi.setApiClient(steve);
        partApi.checkOut(workspaceId, "SEAT-020", "C");

        DocumentApi documentApi = new DocumentApi(client);
        documentApi.checkOutDocument(workspaceId, "USER-MAN-001", "A");
        documentApi.checkOutDocument(workspaceId, "INVOICE-002", "A");
        documentApi.checkOutDocument(workspaceId, "INVOICE-001", "A");

        documentApi.setApiClient(joe);
        documentApi.checkOutDocument(workspaceId, "API-001", "A");

        ApiClient bill = DocDokuPLMClientFactory.createJWTClient(url, "bill", "test");
        documentApi.setApiClient(bill);
        documentApi.checkOutDocument(workspaceId, "LETTER-001", "A");
        documentApi.checkOutDocument(workspaceId, "LETTER-002", "A");
    }

    private void createOrganization() throws ApiException {
        LOGGER.info("Creating organization for " + login + "...");

        OrganizationsApi organizationsApi = new OrganizationsApi(client);
        OrganizationDTO organizationDTO = organizationsApi.getOrganization();
        if (organizationDTO != null) {
            if (!login.equals(organizationDTO.getOwner())) {
                LOGGER.info("You are member of an existing organization. Cannot create yours.");
                return;
            }
            organizationsApi.deleteOrganization();
        }

        //create the organization
        organizationDTO = new OrganizationDTO();
        organizationDTO.setName("organization-" + UUID.randomUUID().toString().substring(0, 8));
        organizationDTO.setDescription("Organization created from sample");
        organizationDTO.setOwner(login);
        organizationsApi.createOrganization(organizationDTO);
        //add members
        LOGGER.info("add members to organization...");
        UserDTO userDTO = new UserDTO();
        int limit = 0;
        for (String user : LOGINS) {
            userDTO.setName(user);
            userDTO.setLogin(user);
            organizationsApi.addMember(userDTO);
            if (limit == 2) break;
            limit++;
        }
    }

}
