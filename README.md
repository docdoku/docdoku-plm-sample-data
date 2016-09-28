# DocDoku PLM Sample Data 

This repository allows you to populate your DocDokuPLM installation with some sample data.

## Requirements

DocdokuPLM server installed. Refer to [this page](https://github.com/docdoku/docdoku-plm) to install a DocDokuPLM server.

## Instructions
 
Clone this project

    git clone https://github.com/docdoku/docdoku-plm-sample-data.git

Run loadSample.sh or loadSample.bat depending on your OS

    ./loadSample.sh -u login -p password -h url [-w workspaceId]  
    
Parameters :

* login : your account name (will be created) - required
* password : your account password (will be created) - required
* url : url of the DocdokuPLM instance server (http://localhost:8080) - required
* workspaceId : then name of the workspace to be created - optional, will be generated if not specified

