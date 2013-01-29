# What is it?
The schema generator works alongside the JAXB binding compiler to produce Avro bindings which are
very similar to the JAXB classes. The process is wrapped in a Maven plugin, which allows for manual
or automatic execution of the process. The plugin completes in one step the generation of JAXB classes,
Avro schemas, and Avro classes.

For more information about the projet, read the writeup on InfoQ:
http://www.infoq.com/articles/AVROSchemaJAXB

# How does it work?
The first part of the process utilizes XJC, the JAXB Binding Compiler, to produce a Java code model from a
series of xsd schema files. After this is finished, an XJC plugin runs which inspects the classes to
be generated and produces parallel Avro schemas in JSON format. Then the classes and schemas are written out.
Finally, the plugin calls the Java schema compiler provided by the Avro project to produce a new set of Java
classes for serialization to and from Avro.

# Where to get it?
Currently the plugin is not available from maven central, so you will have to download the source and install
it to your local Maven repository.

# How to run it.
The schema generator is wrapped into a Maven plugin, which can be executed manually or as part of a build process. The
schemagen-plugin component provides a plugin with one goal, "generate". There is no default phase to which the goal is
bound, so it will never be executed automatically without first specifying the phase when it should run. For example,
the 'validate' or 'generate-sources' phase is a good place to put such activities, as they run prior to the compile phase.

Your Maven project would include a plugin declaration similar to this:

    <plugin>
        <groupId>com.nokia.util.avro</groupId>
        <artifactId>schemagen-plugin</artifactId>
        <version>0.3</version>
        <configuration>
            <outputDirectory>..\sources</outputDirectory>
            <packageName>my.generated</packageName>
            <bindingFiles>
            <file>resources/binding1.xsd</file>
        </bindingFiles>
        <schemaFiles>
            <schema>resources/schema1.xsd</schema>
            </schemaFiles>
        </configuration>
    </plugin>
	
Currently you must provide all schema files you wish to be considered in the bindings and schemas lists.
The plugin's exposed name is "schemagen". So now from the command line you can navigate to your POM file and
execute the following command:

    mvn schemagen:generate

This will invoke the entire workflow on your input data. The artifacts produced by the process are:
* JAXB generated Java sources
* Avro schema files (JSON)
* Avro generated Java sources

You can then use the Java files needed. To have the source files compiled automatically, you will want to
either have them created in folder which is part of your Maven project's source directory, or explicitly
add an include for the generated directory using the maven-compiler plugin.