# Workflow Engine

Central workflow engine


Functional packaging constructs

#### REST API Controllers

Interface that is exposed over HTTP and define the resources for the engine

#### Application Service Code

Internal application code that uses the native Camunda API and does filtering
based on the user who is currently logged in and their team details.

Only certain Camunda REST APIs

#### Core Resources
1. Tasks
2. Process Definitions
3. Notifications
4. Shift
5. Process Instances


See swagger docs:

{env}://swagger-ui.html

If this service is talking to internal services then ensure you have added the internal CA certs as seperate certificate files
to /ca/xxx.crt. These will be loaded as part of the boot procedure into the java keystore and trusted.
