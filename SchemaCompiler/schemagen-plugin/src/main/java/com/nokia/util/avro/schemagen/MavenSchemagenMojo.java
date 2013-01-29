/*
 * Copyright 2013 Nokia Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nokia.util.avro.schemagen;

import com.sun.tools.xjc.Driver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Maven Mojo, for execution from within Maven.
 * We have one goal: generate. There is no phase binding, and so execution is invoked manually by calling
 * 'schemagen:generate'. For automatic running, the 'generate-sources' phase is a good candidate for the
 * work this plugin does.
 *
 *
 * @author Ben Fagin (Nokia)
 * @version 02-02-2012
 *
 * @goal generate
 * @requiresProject false
 */
public class MavenSchemagenMojo extends AbstractMojo {

	// Maven parameters

	/**
	 * The desired base package name for generated code.
	 * Will use xml namespace otherwise.
	 *
	 * @parameter
	 * @since 0.1
	 */
	private String packageName;

	/**
	 * The root directory to output generated files.
	 *
	 * @parameter
	 * @required
	 * @since 0.1
	 */
	private File outputDirectory;

	/**
	 * A list of schema files to process.
	 *
	 * @parameter
	 * @required
	 * @since 0.1
	 */
	private File[] schemaFiles;

	/**
	 * A list of binding files to process.
	 *
	 * @parameter
	 * @since 0.1
	 */
	private File[] bindingFiles;

	/**
	 * Should the output directory be cleared before starting work?
	 *
	 * @parameter default-value="true"
	 * @since 0.1
	 */
	private Boolean clearOutputDirectory;



	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		setup();
		String args[] = buildArguments();

		// Fire away!
		try {
			getLog().info("Beginning combined JAXB xjc and avro schema generation.");

			// XJC blindly calls System.exit when it is finished.
			// This creates a work-around.
			SecurityManager oldSecurityManager = System.getSecurityManager();
			getLog().debug("changing to delegating security manager");
			System.setSecurityManager(new DelegatingNoExitSecurityManager(oldSecurityManager));

			try {
				Driver.main(args);
			} catch (DelegatingNoExitSecurityManager.NormalExitException ex) {
				getLog().debug("Preventing XJC from terminating the JVM...");
			} finally {
				System.setSecurityManager(oldSecurityManager);
			}

			getLog().debug("restored previous security manager");

		} catch (Exception ex) {
			getLog().info("( Make sure your binding files specify the schema location relatively! )");
			throw new MojoFailureException(ex, "An error occurred during execution.", ex.getMessage());
		}

		// now consume the generated schemas
		File avroSchemaOutput = AvroSchemagenPlugin.getSchemaDirectory(outputDirectory);
		if (!avroSchemaOutput.exists()) {
			getLog().error("Could not locate schema output directory '" + outputDirectory.getAbsolutePath() + "'.");
			throw new MojoExecutionException("Could not locate schema output directory.");
		}

		// get schema files, but resort by name
		File[] avroSchemas = avroSchemaOutput.listFiles();
		Arrays.sort(avroSchemas, new Comparator<File>() {
			public @Override int compare(File o1, File o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		// compile (will place them in the same package as their namespaces define)
		getLog().info("Compiling avro classes from " + avroSchemas.length + " schema files.");
		AvroCompiler.compileSchemas(avroSchemas, outputDirectory);

		// all done
		getLog().info("Compilation completed successfully. Ending work...");
	}
	
	private void setup() throws MojoExecutionException, MojoFailureException {
		if (bindingFiles == null) { bindingFiles = new File[]{}; }

		// create output directory if it doesn't exist
		if (!outputDirectory.exists()) {
			if (!outputDirectory.mkdirs()) {
				throw new MojoFailureException("Could not create output directory.");
			}
		} else {
			// clear output directory if instructed to do so
			if (clearOutputDirectory) {
				for (File file : outputDirectory.listFiles()) {
					deleteFile(file);
				}
			}
		}
	}
	
	private void deleteFile(File f) throws MojoFailureException {
		if (f.isDirectory()) {
			for (File file : f.listFiles()) {
				deleteFile(file);
			}
		}

		boolean result = f.delete();
		if (!result) {
			throw new MojoFailureException("Could not remove file '" + f.getAbsolutePath() + "'.");
		}		
	}
	
	private String[] buildArguments() {
		List<String> args = new ArrayList<String>();

		// package name
		if (packageName != null && !packageName.trim().isEmpty()) {
			args.add("-p");
			args.add(packageName);
		}

		// binding files
		for (File bindingFile : bindingFiles) {
			args.add("-b");
			args.add(makeRelativePath(bindingFile));
		}

		// use our plugin
		args.add("-"+AvroSchemagenPlugin.PLUGIN_NAME);

		// output directory
		args.add("-d");
		args.add(outputDirectory.toString());

		// schema files
		for (File schema : schemaFiles) {
			args.add(makeRelativePath(schema));
		}

		// info output
		StringBuilder command = new StringBuilder();
		for (String arg : args) {
			command.append(arg).append(" ");
		}
		getLog().info("command string:\n\t"+command);

		return args.toArray(new String[args.size()]);
	}

	private String makeRelativePath(File file) {
		File cur = new File("");
		String curPath = cur.getAbsolutePath();
		String filePath = file.getAbsolutePath();

		if (filePath.startsWith(curPath)) {
			return filePath.substring(curPath.length()+1);
		} else {
			return filePath;
		}
	}
}
