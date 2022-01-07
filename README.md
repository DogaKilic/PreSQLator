# PreSQLator

A tool that transforms java code that contains SQL statements into function equivalent code by mimicking the connection to the database in an object oriented manner.
Through this process, static information flow analysis becomes possible to perform on java code that contains database interactions.
soot framework is used in this project to create and modify classes.


===========================================================================

First time setup

compile the project using maven:

> mvn clean compile assembly:single

Jar-file of the project should be built in the \target folder.
===========================================================================

Usage


The Jar-file expects 2 or 3 arguments to run:

1. Classfile that you want to process.

2. Folder where the results of the processing should be written to.

3. (Optional) A text file containing the SQL create table statements that are not contained in the classfile but still interacted with.
    For example if a table "person" was retrieved using a select statement but not created in the same classfile, the textfile should contain: "create table person(...)" with the     tables right column names and types in the braces.
    
    
Then the Jar-file can be called with:

> java -jar testProj.jar Scratch.class ScratchResultFolder

or if one or more create table statements have to be given:

>java -jar testProj.jar Scratch.class ScratchResultFolder ScratchCreateText.txt


After running the Jar-file the processed class-file and all the classes required to run it should be contained in a folder called \Scratch in the given result folder.

===========================================================================
