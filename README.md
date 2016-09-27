# DocDoku PLM Sample Data 

This repository allows you to populate your DocDokuPLM installation with some sample data.

## Requirements

DocdokuPLM server installed. Refer to [this page](https://github.com/docdoku/docdoku-plm) to install a DocDokuPLM server.

## Instructions
 
Clone this project

    git clone https://github.com/docdoku/docdoku-plm-sample-data.git

Run loadSample.sh or loadSample.bat depending on your OS

    ./loadSample.sh [login] [password] [workspaceId] [url]
    
Parameters :

* workspaceId : then name of the workspace to be created
* login : your account name (will be created)
* password : your account password (will be created)
* url : url of the DocdokuPLM instance server (http://localhost:8080)

