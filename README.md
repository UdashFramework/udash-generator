# Udash Generator [![Build Status](https://travis-ci.org/tstangenberg/udash-generator.svg?branch=master)](https://travis-ci.org/tstangenberg/udash-generator) [![Join the chat at https://gitter.im/UdashFramework/udash-generator](https://badges.gitter.im/UdashFramework/udash-generator.svg)](https://gitter.im/UdashFramework/udash-generator?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [<img align="right" height="50px" src="http://www.avsystem.com/avsystem_logo.png">](http://www.avsystem.com/)

If you want to start developing with Udash as soon as possible, you may find the Udash project generator very helpful. Basing on a few configuration settings, it will generate a complete, runnable Udash project ready to work with.

### Command line interface

To use the project generator in the command line, download it from [here](https://github.com/UdashFramework/udash-generator/releases). Then use *run.sh* or *run.bat* script to start it.

Now you will have to configure a few settings:
* **Project root directory** - a directory where project files will be placed.
* **Clear root directory** - if true, the generator will remove all files and directories from the selected project root directory.
* **Project name** - a name of your project.
* **Organization** - a domain of your organization like: io.udash or com.avsystem.
* **Root package** - a root package of project source files like: io.udash.project or com.avsystem.project.name.
* **Project type** - you can select a frontend-only project without modules or a standard Udash project with three modules: "backend", "shared", "frontend".
* **Module names** - if you selected the default Udash project, then you should select names of the modules.
* **Create basic frontend application** - decide if you want to generate a base of the frontend application.
 * **Create frontend demo views** - decide if you want to generate frontend demo views.
 * **Create ScalaCSS demo views** - decide if you want to add ScalaCSS to your project and generate demo views.
* **Create Jetty launcher** - decide if you want to use a Jetty server for serving the frontend files.
 * **Create RPC communication layer** - decide if you want to generate the RPC base for your project.
 * **Create RPC communication layer demos** - decide if you want to generate RPC demo views.
* **Start generation** - decide if you want to start project generation based on the above configuration.
 
### Project compilation and running
In case of the frontend-only project, you can use the sbt compile command to compile sources of your web application. When compilation is finished, you can find generated files in the *target/UdashStatic* directory.

If you decided to create the standard project with the Jetty launcher, you can use the sbt run command to compile sources of your web application and start the Jetty server. When the server is started, you can see your web application on http://127.0.0.1:8080/.

While developing, you can use the *~compile* task, which will automatically recompile changed source files.

Read more in the [Udash Developer's Guide](http://guide.udash.io/#/bootstrapping/generators).
