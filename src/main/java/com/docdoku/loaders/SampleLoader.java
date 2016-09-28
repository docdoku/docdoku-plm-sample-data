package com.docdoku.loaders;

import com.docdoku.api.DocdokuPLMBasicClient;
import com.docdoku.api.DocdokuPLMClient;
import com.docdoku.api.client.ApiClient;
import com.docdoku.api.client.ApiException;
import com.docdoku.api.models.*;
import com.docdoku.api.models.utils.LastIterationHelper;
import com.docdoku.api.models.utils.UploadDownloadHelper;
import com.docdoku.api.services.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO
 * - add Jenkins job
 *
 * @author Morgan GUIMARD
 */
public class SampleLoader {

    private static final Logger LOGGER = Logger.getLogger(SampleLoader.class.getName());
    private static final boolean debug = false;

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
        guestClient = new DocdokuPLMClient(this.url, debug).getClient();
        client = new DocdokuPLMBasicClient(url, login, password).getClient();

    }

    public void load() throws ApiException, IOException, InterruptedException {

        LOGGER.info("Starting load process ... ");

        checkServerAvailability();
        createCallerAccount();
        createWorkspace();
        createOtherAccounts();
        createGroups();

        createMilestones();
        createRolesAndWorkflow();

        createDocumentTemplates();
        createFolders();
        createTags();
        createDocuments();

        createPartTemplates();
        createParts();
        createProducts();
        createBaseline();
        createProductInstace();

        createRequests();
        createIssues();
        createOrders();

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
        String[] logins = {"rob", "joe", "bill"};
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
        group.setId("Group1");
        new WorkspacesApi(client).createGroup(workspaceId, group);
        group.setId("Group2");
        new WorkspacesApi(client).createGroup(workspaceId, group);
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
    }

    private void addUserToWorkspace(String pLogin) throws ApiException {
        LOGGER.log(Level.INFO, "Adding user " + pLogin + " to workspace " + workspaceId);
        UserDTO userDTO = new UserDTO();
        userDTO.setLogin(pLogin);
        new WorkspacesApi(client).addUser(workspaceId, userDTO, null);
    }

    private void createWorkspace() throws ApiException {
        LOGGER.log(Level.INFO, "Creating workspaceId ...");
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
        new DocumenttemplatesApi(client).createDocumentMasterTemplate(workspaceId, template);

        template.setReference("Invoice");
        template.setDocumentType("Paper");
        template.setMask("INVOICE-###");
        new DocumenttemplatesApi(client).createDocumentMasterTemplate(workspaceId, template);

        template.setReference("UserManuals");
        template.setDocumentType("Documentation");
        template.setMask("USER-MAN-###");
        new DocumenttemplatesApi(client).createDocumentMasterTemplate(workspaceId, template);
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
    }

    private void createTags() throws ApiException {
        LOGGER.log(Level.INFO, "Creating tags ...");
        TagListDTO tagListDTO = new TagListDTO();
        List<TagDTO> tags = new ArrayList<>();
        tagListDTO.setTags(tags);

        String[] tagNames = {"internal", "important", "2016", "archive"};

        for (String tagName : tagNames) {
            TagDTO tagDTO = new TagDTO();
            tagDTO.setId(tagName);
            tagDTO.setWorkspaceId(workspaceId);
            tagDTO.setLabel(tagName);
            tags.add(tagDTO);
        }

        new TagsApi(client).createTags(workspaceId, tagListDTO);
    }

    private void createDocuments() throws ApiException, IOException {
        LOGGER.log(Level.INFO, "Creating documents ...");

        // Creation
        DocumentCreationDTO documentCreationDTO = new DocumentCreationDTO();
        documentCreationDTO.setReference("LETTER-001");
        documentCreationDTO.setTitle("My first letter");
        documentCreationDTO.setWorkspaceId(workspaceId);
        documentCreationDTO.setTemplateId("Letter");
        documentCreationDTO.setDescription("Some letter created with sample loader");
        new FoldersApi(client).createDocumentMasterInFolder(workspaceId, documentCreationDTO, workspaceId + ":Letters");

        documentCreationDTO.setReference("LETTER-002");
        documentCreationDTO.setTitle("An other letter");
        new FoldersApi(client).createDocumentMasterInFolder(workspaceId, documentCreationDTO, workspaceId + ":Letters");

        documentCreationDTO.setReference("INVOICE-001");
        documentCreationDTO.setTitle("My first invoice");
        documentCreationDTO.setWorkspaceId(workspaceId);
        documentCreationDTO.setTemplateId("Invoice");
        documentCreationDTO.setDescription("Some invoice created with sample loader");
        new FoldersApi(client).createDocumentMasterInFolder(workspaceId, documentCreationDTO, workspaceId + ":Invoices");

        documentCreationDTO.setReference("INVOICE-002");
        documentCreationDTO.setTitle("A second invoice");
        new FoldersApi(client).createDocumentMasterInFolder(workspaceId, documentCreationDTO, workspaceId + ":Invoices");

        documentCreationDTO.setReference("USER-MAN-001");
        documentCreationDTO.setTitle("User documentation");
        documentCreationDTO.setWorkspaceId(workspaceId);
        documentCreationDTO.setTemplateId("UserManuals");
        documentCreationDTO.setDescription("Some end-user documentation");
        new FoldersApi(client).createDocumentMasterInFolder(workspaceId, documentCreationDTO, workspaceId + ":Documentation");


        LOGGER.log(Level.INFO, "Uploading document files...");
        // Upload
        DocumentIterationDTO documentIterationDTO = new DocumentIterationDTO();
        documentIterationDTO.setWorkspaceId(workspaceId);
        documentIterationDTO.setVersion("A");
        documentIterationDTO.setIteration(1);

        documentIterationDTO.setDocumentMasterId("LETTER-001");
        UploadDownloadHelper.uploadAttachedFile(documentIterationDTO, client, SampleLoaderUtils.getFile("letter-001.docx"));

        documentIterationDTO.setDocumentMasterId("LETTER-002");
        UploadDownloadHelper.uploadAttachedFile(documentIterationDTO, client, SampleLoaderUtils.getFile("letter-002.docx"));

        documentIterationDTO.setDocumentMasterId("INVOICE-001");
        UploadDownloadHelper.uploadAttachedFile(documentIterationDTO, client, SampleLoaderUtils.getFile("invoice-001.xlsx"));

        documentIterationDTO.setDocumentMasterId("INVOICE-002");
        UploadDownloadHelper.uploadAttachedFile(documentIterationDTO, client, SampleLoaderUtils.getFile("invoice-002.xlsx"));

        documentIterationDTO.setDocumentMasterId("USER-MAN-001");
        UploadDownloadHelper.uploadAttachedFile(documentIterationDTO, client, SampleLoaderUtils.getFile("user-man-001.txt"));

        // Check in
        LOGGER.log(Level.INFO, "Checking in documents ...");
        new DocumentApi(client).checkInDocument(workspaceId, "LETTER-001", "A", "");
        new DocumentApi(client).checkInDocument(workspaceId, "LETTER-002", "A", "");
        new DocumentApi(client).checkInDocument(workspaceId, "INVOICE-001", "A", "");
        new DocumentApi(client).checkInDocument(workspaceId, "INVOICE-002", "A", "");
        new DocumentApi(client).checkInDocument(workspaceId, "USER-MAN-001", "A", "");
    }


    private void createMilestones() throws ApiException {
        LOGGER.log(Level.INFO, "Creating milestones ...");
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, 15);

        MilestoneDTO milestoneDTO = new MilestoneDTO();
        milestoneDTO.setWorkspaceId(workspaceId);
        milestoneDTO.setTitle("1.0");
        milestoneDTO.setDescription("First release");
        milestoneDTO.setDueDate(c.getTime());

        new MilestonesApi(client).createMilestone(workspaceId, milestoneDTO);

        c.add(Calendar.DATE, 90);
        milestoneDTO.setTitle("2.0");
        milestoneDTO.setDescription("Second release");
        milestoneDTO.setDueDate(c.getTime());
        new MilestonesApi(client).createMilestone(workspaceId, milestoneDTO);


    }

    private void createRequests() throws ApiException {
        LOGGER.log(Level.INFO, "Creating requests ...");

        ChangeRequestDTO changeRequestDTO = new ChangeRequestDTO();
        changeRequestDTO.setWorkspaceId(workspaceId);

        changeRequestDTO.setName("REQ-001");
        changeRequestDTO.setDescription("Something needs to be corrected");
        changeRequestDTO.setCategory(ChangeRequestDTO.CategoryEnum.CORRECTIVE);
        new ChangeitemsApi(client).createRequest(workspaceId, changeRequestDTO);

        changeRequestDTO.setName("REQ-002");
        changeRequestDTO.setDescription("Something needs to be perfected");
        changeRequestDTO.setCategory(ChangeRequestDTO.CategoryEnum.PERFECTIVE);
        new ChangeitemsApi(client).createRequest(workspaceId, changeRequestDTO);
    }

    private void createIssues() throws ApiException {
        LOGGER.log(Level.INFO, "Creating issues ...");

        ChangeIssueDTO changeIssueDTO = new ChangeIssueDTO();
        changeIssueDTO.setWorkspaceId(workspaceId);

        changeIssueDTO.setName("ISSUE-001");
        changeIssueDTO.setDescription("Something is wrong");
        changeIssueDTO.setPriority(ChangeIssueDTO.PriorityEnum.HIGH);
        new ChangeitemsApi(client).createIssue(workspaceId, changeIssueDTO);

        changeIssueDTO.setName("ISSUE-002");
        changeIssueDTO.setDescription("Something is terribly wrong");
        changeIssueDTO.setPriority(ChangeIssueDTO.PriorityEnum.EMERGENCY);
        new ChangeitemsApi(client).createIssue(workspaceId, changeIssueDTO);
    }

    private void createOrders() throws ApiException {
        LOGGER.log(Level.INFO, "Creating orders ...");

        ChangeOrderDTO changeOrderDTO = new ChangeOrderDTO();
        changeOrderDTO.setWorkspaceId(workspaceId);

        changeOrderDTO.setName("ORDER-001");
        changeOrderDTO.setDescription("Order for some documents");
        changeOrderDTO.setCategory(ChangeOrderDTO.CategoryEnum.OTHER);
        new ChangeitemsApi(client).createOrder(workspaceId, changeOrderDTO);

        changeOrderDTO.setName("ORDER-002");
        changeOrderDTO.setDescription("Order for some parts");
        changeOrderDTO.setCategory(ChangeOrderDTO.CategoryEnum.OTHER);
        new ChangeitemsApi(client).createOrder(workspaceId, changeOrderDTO);
    }

    private void createRolesAndWorkflow() throws ApiException {
        LOGGER.log(Level.INFO, "Creating roles ...");

        // Roles
        List<UserDTO> designers = new ArrayList<>();
        List<UserDTO> technicians = new ArrayList<>();

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
        RoleDTO designerRole = new RolesApi(client).createRole(workspaceId, roleDTO);

        roleDTO.setName("technicians");
        roleDTO.setDefaultAssignedUsers(technicians);
        RoleDTO technicianRole = new RolesApi(client).createRole(workspaceId, roleDTO);


        // Workflow
        LOGGER.log(Level.INFO, "Creating workflow ...");

        TaskModelDTO firstTask = new TaskModelDTO();
        firstTask.setNum(0);
        firstTask.setTitle("Design something");
        firstTask.setInstructions("Create a new design, then validate the task");
        firstTask.setRole(designerRole);

        TaskModelDTO secondTask = new TaskModelDTO();
        firstTask.setNum(1);
        secondTask.setTitle("Build something");
        firstTask.setInstructions("Build the prototype and validate the task");
        secondTask.setRole(technicianRole);

        List<TaskModelDTO> tasks = new ArrayList<>();
        tasks.add(firstTask);
        tasks.add(secondTask);

        ActivityModelDTO firstActivity = new ActivityModelDTO();
        firstActivity.setStep(0);
        firstActivity.setTaskModels(tasks);
        firstActivity.setType(ActivityModelDTO.TypeEnum.SEQUENTIAL);
        firstActivity.setLifeCycleState("First Step");

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

        PartTemplateCreationDTO partTemplateCreationDTO = new PartTemplateCreationDTO();
        partTemplateCreationDTO.setWorkspaceId(workspaceId);

        partTemplateCreationDTO.setReference("SEATS");
        partTemplateCreationDTO.setMask("SEAT-###");
        new ParttemplatesApi(client).createPartMasterTemplate(workspaceId, partTemplateCreationDTO);

        partTemplateCreationDTO.setReference("ENGINES");
        partTemplateCreationDTO.setMask("ENGINE-###");
        new ParttemplatesApi(client).createPartMasterTemplate(workspaceId, partTemplateCreationDTO);
    }


    private void createParts() throws ApiException, IOException, InterruptedException {

        LOGGER.log(Level.INFO, "Creating parts ...");

        PartCreationDTO part = new PartCreationDTO();

        part.setWorkspaceId(workspaceId);
        part.setDescription("Sample part create with sample loader");


        part.setTemplateId("SEATS");
        part.setNumber("SEAT-010");
        part.setName("front seat");
        new PartsApi(client).createNewPart(workspaceId, part);
        part.setNumber("SEAT-020");
        part.setName("back seat");
        new PartsApi(client).createNewPart(workspaceId, part);

        part.setTemplateId("ENGINES");
        part.setNumber("ENGINE-050");
        part.setName("50cc engine");
        new PartsApi(client).createNewPart(workspaceId, part);
        part.setNumber("ENGINE-100");
        part.setName("100cc engine");
        new PartsApi(client).createNewPart(workspaceId, part);

        // Create an assembly

        part.setNumber("CAR-001");
        part.setName("A sample assembly");
        part.setTemplateId(null);
        PartRevisionDTO assembly = new PartsApi(client).createNewPart(workspaceId, part);
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

        passengerSeatLink.setComponent(seat);
        passengerSeatLink.setAmount(1.0);
        passengerSeatLink.setReferenceDescription("Passenger seat");

        engineLink.setComponent(engine);
        engineLink.setAmount(1.0);
        engineLink.setReferenceDescription("The engine of this car");

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

        new PartsApi(client).updatePartIteration(workspaceId, assembly.getNumber(), "A", 1, lastIteration);

        // Upload 3D files
        LOGGER.log(Level.INFO, "Uploading 3D files...");

        PartIterationDTO partIterationDTO = new PartIterationDTO();
        partIterationDTO.setWorkspaceId(workspaceId);
        partIterationDTO.setVersion("A");
        partIterationDTO.setIteration(1);

        partIterationDTO.setNumber("SEAT-010");
        UploadDownloadHelper.uploadNativeCADFile(partIterationDTO, client, SampleLoaderUtils.getFile("BassBoat-FrontSeat.obj"));
        UploadDownloadHelper.uploadAttachedFile(partIterationDTO, client, SampleLoaderUtils.getFile("BassBoat-FrontSeat.mtl"));

        partIterationDTO.setNumber("SEAT-020");
        UploadDownloadHelper.uploadNativeCADFile(partIterationDTO, client, SampleLoaderUtils.getFile("BassBoat-BackSeat.obj"));
        UploadDownloadHelper.uploadAttachedFile(partIterationDTO, client, SampleLoaderUtils.getFile("BassBoat-BackSeat.mtl"));

        partIterationDTO.setNumber("ENGINE-050");
        UploadDownloadHelper.uploadNativeCADFile(partIterationDTO, client, SampleLoaderUtils.getFile("BassBoat-OutboardMotor.obj"));
        UploadDownloadHelper.uploadAttachedFile(partIterationDTO, client, SampleLoaderUtils.getFile("BassBoat-OutboardMotor.mtl"));

        partIterationDTO.setNumber("ENGINE-100");
        UploadDownloadHelper.uploadNativeCADFile(partIterationDTO, client, SampleLoaderUtils.getFile("BassBoat-TrollingMotor.obj"));
        UploadDownloadHelper.uploadAttachedFile(partIterationDTO, client, SampleLoaderUtils.getFile("BassBoat-TrollingMotor.mtl"));

        LOGGER.log(Level.INFO, "Waiting for conversion...");
        // Let the conversion finish
        Thread.sleep(5000);

        LOGGER.log(Level.INFO, "Checking in parts...");
        new PartApi(client).checkIn(workspaceId, "SEAT-010", "A", "");
        new PartApi(client).checkIn(workspaceId, "SEAT-020", "A", "");
        new PartApi(client).checkIn(workspaceId, "ENGINE-050", "A", "");
        new PartApi(client).checkIn(workspaceId, "ENGINE-100", "A", "");
        new PartApi(client).checkIn(workspaceId, "CAR-001", "A", "");

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
        new ProductbaselineApi(client).createBaseline(workspaceId,"CAR-001",baseline);
    }

    private void createProductInstace() throws ApiException {

        List<ProductBaselineDTO> baselines = new ProductbaselineApi(client).getBaselines(workspaceId, "CAR-001");
        ProductBaselineDTO firstBaselineFound = baselines.get(0);

        ProductInstanceCreationDTO productInstance = new ProductInstanceCreationDTO();
        productInstance.setConfigurationItemId("CAR-001");
        productInstance.setSerialNumber("CAR-001-XX001");

        productInstance.setBaselineId(firstBaselineFound.getId());
        new ProductinstancesApi(client).createProductInstanceMaster(workspaceId, productInstance);
    }
}
