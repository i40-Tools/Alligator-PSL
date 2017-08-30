package main;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.script.ScriptException;

import org.codehaus.groovy.control.CompilationFailedException;

import groovy.lang.Script;
import groovy.util.ResourceException;
import util.ConfigManager;

/**
 * Main class of the SemCPS project
 * 
 * @author Irlan
 * @author Omar
 * 
 */

public class SemCPSMain {

	private Similar similar = new Similar();

	public static void main(String[] args) throws Throwable {

		//Report.getReport(ConfigManager.getExperimentFolder());
		SemCPSMain main = new SemCPSMain();
		main.readConvertStandardFiles();
		main.generatePSLDataModel();
		main.executePSLAproach();
		// main.integrate();
		// main.executePSLAproach();
		// main.integrate();
	}


	/**
	 * Models similar.txt in to GoldStandard format.
	 * @throws Exception 
	 */
	public void modelSimilar() throws Exception {
		similar.readFiles(ConfigManager.getFilePath(), ".ttl", ".rdf", ".owl");
		similar.convertSimilar();
	}

	/**
	 * Method that read standard files and convert then to RDF
	 * @throws Exception 
	 * TODO create more specific exceptions
	 */
	public void readConvertStandardFiles() throws Exception {
	}

	/**
	 * Generate the PSL datamodel out of the existing standard documents
	 * 
	 * @throws Exception
	 */
	public void generatePSLDataModel() throws Exception {
		ConfigManager.createDataPath();// creates folders if not there
		similar.generatePSLPredicates(ConfigManager.getFilePath());
	}

	/**
	 * General method to execute the PSL-based approach
	 * 
	 * @throws CompilationFailedException
	 * @throws IOException
	 * @throws ScriptException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws groovy.util.ScriptException
	 * @throws ResourceException
	 */
	public void executePSLAproach() throws CompilationFailedException, IOException, ScriptException,
	IllegalAccessException, IllegalArgumentException, InvocationTargetException,
	NoSuchMethodException, SecurityException, InstantiationException, ResourceException,
	groovy.util.ScriptException {
		// Needed to run the PSL rules part
		Script script = new Script() {
			@Override
			public Object run() {
				return null;
			}
		};
		try {
			script.evaluate(new File("src/main/java/pslApproach/KGAlignment.groovy"));
		} catch (Exception e) {
		}
	}


}
