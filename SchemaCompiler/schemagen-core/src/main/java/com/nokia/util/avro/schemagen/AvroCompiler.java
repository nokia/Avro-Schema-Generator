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

import org.apache.avro.Schema;
import org.apache.avro.compiler.specific.SpecificCompiler;

import java.io.File;

/**
 * @author Ben Fagin (Nokia)
 * @version 02-03-2012
 *
 */
public class AvroCompiler {

	/**
	 * Calls the avro compiler to compile Java bindings from avro schema files.
	 * The files MUST be in a sorted order which accounts for their dependencies!
	 *
	 * @param sources JSON schema files to compile
	 * @param outputDirectory directory to write packages and files
	 */
	public static void compileSchemas(File[] sources, File outputDirectory) {
		try {
			SpecificCompiler.compileSchema(sources, outputDirectory);
		} catch (Exception ex) {
			throw new SchemagenException("An error occurred while compiling.", ex);
		}
	}

	public static String combineSchemas(File[] sources, String topLevelType) {
		try {
			Schema.Parser parser = new Schema.Parser();

			for (File source : sources) {
				parser.parse(source);
			}

			Schema top = parser.getTypes().get(topLevelType);
			if (top == null) {
				throw new SchemagenException("Could not find top level type '"+topLevelType+"'.");
			}

			return top.toString(true);
		} catch (Exception ex) {
			throw new SchemagenException("An error occurred while compiling.", ex);
		}
	}
}
