
package pslApproach
import java.text.DecimalFormat

import edu.umd.cs.psl.application.inference.MPEInference
import edu.umd.cs.psl.application.learning.weight.maxlikelihood.MaxLikelihoodMPE
import edu.umd.cs.psl.config.*
import edu.umd.cs.psl.database.DataStore
import edu.umd.cs.psl.database.Database
import edu.umd.cs.psl.database.Partition
import edu.umd.cs.psl.database.rdbms.RDBMSDataStore
import edu.umd.cs.psl.database.rdbms.driver.H2DatabaseDriver
import edu.umd.cs.psl.database.rdbms.driver.H2DatabaseDriver.Type
import edu.umd.cs.psl.evaluation.statistics.DiscretePredictionComparator
import edu.umd.cs.psl.evaluation.statistics.DiscretePredictionStatistics
import edu.umd.cs.psl.groovy.*
import edu.umd.cs.psl.model.argument.ArgumentType
import edu.umd.cs.psl.model.argument.GroundTerm
import edu.umd.cs.psl.model.argument.type.*
import edu.umd.cs.psl.model.atom.GroundAtom
import edu.umd.cs.psl.model.atom.RandomVariableAtom
import edu.umd.cs.psl.model.predicate.Predicate
import edu.umd.cs.psl.model.predicate.type.*
import edu.umd.cs.psl.ui.functions.textsimilarity.*
import edu.umd.cs.psl.ui.loading.InserterUtils
import edu.umd.cs.psl.util.database.Queries
import main.SemCPSMain

/**
 * @author Omar Rana
 * @author Irlan Grangel
 * Computes the alignment of two entities based on Probabilistic Soft Logic(PSL)
 *
 */
public class DocumentAligment
{
	private ConfigManager cm
	private ConfigBundle config
	private Database testDB
	private Database trainDB
	private Database truthDB
	private PSLModel model
	private DataStore data
	private Partition testObservations
	private Partition testPredictions
	private Partition targetsPartition
	private Partition truthPartition
	private String threshold
	def dir
	def testDir
	def trainDir

	public static void main(String[] args)
	{
		DocumentAligment docAlign = new DocumentAligment()
		docAlign.execute()
	}

	/**
	 * TODO to write comments
	 */
	public void run()
	{
		config()
		definePredicates()
		defineOntoPredicates()
		defineSetPredicates()
		defineFunctions()
		defineRules()
		defineOntoRules()
		defineSetRules()
		if (util.ConfigManager.getNegativeRules().equals("true")){
		defineNotSimilarRules()
		}
		threshold = util.ConfigManager.getThreshold()
		setUpData()
		runInference()
		SemCPSMain main = new SemCPSMain();
		main.modelSimilar()
		evalResults()
	}

	public void execute()
	{
		DocumentAligment documentAligment = new DocumentAligment()
		documentAligment.run()
	}

	public void config()
	{
		cm = ConfigManager.getManager()
		config = cm.getBundle("document-alignment")
		def defaultPath = System.getProperty("java.io.tmpdir")
		String dbpath = config.getString("dbpath", defaultPath + File.separator + "document-alignment")
		data = new RDBMSDataStore(new H2DatabaseDriver(Type.Disk, dbpath, true), config)
		model = new PSLModel(this, data)
	}

	/**
	 * Defines the name and the arguments of predicates that are used in the rules
	 */
	public void definePredicates(){

		model.add predicate: "hasDocument", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

		model.add predicate: "hasAttribute"     , types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

		model.add predicate: "hasInternalElement"     , types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

		model.add predicate: "hasInstanceHierarchy"     , types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

		model.add predicate: "hasRefSemantic"     , types: [ArgumentType.UniqueID, ArgumentType.String]

		model.add predicate: "hasInternalElementID"     , types: [ArgumentType.UniqueID, ArgumentType.String]

		model.add predicate: "hasInstanceHierarchyID"     , types: [ArgumentType.UniqueID, ArgumentType.String]

		model.add predicate: "hasExternalInterfaceID"     , types: [ArgumentType.UniqueID, ArgumentType.String]

		model.add predicate: "hasAttributeID"     , types: [ArgumentType.UniqueID, ArgumentType.String]

		model.add predicate: "hasInternalLink"     , types: [ArgumentType.UniqueID, ArgumentType.String]

		model.add predicate: "hasEClassVersion"     , types: [ArgumentType.UniqueID, ArgumentType.String]

		model.add predicate: "hasEClassClassificationClass"     , types: [ArgumentType.UniqueID, ArgumentType.String]

		model.add predicate: "hasEClassIRDI"     , types: [ArgumentType.UniqueID, ArgumentType.String]

		model.add predicate: "similar"     , types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

		model.add predicate: "similarType"     , types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

		model.add predicate: "eval", types: [ArgumentType.String, ArgumentType.String]

		model.add predicate: "hasRoleClassLib"     , types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

		model.add predicate: "hasRoleClass"     , types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

		model.add predicate: "hasInterfaceClass"     , types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

		model.add predicate: "hasSystemUnitClass"     , types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

		model.add predicate: "hasAttributeName"     , types: [ArgumentType.UniqueID, ArgumentType.String]

		model.add predicate: "hasInstanceHierarchyAttributeName"     , types: [ArgumentType.UniqueID, ArgumentType.String]

		model.add predicate: "hasInterfaceClassAttributeName"     , types: [ArgumentType.UniqueID, ArgumentType.String]
		
		model.add predicate: "hasInterfaceClassLibAttributeName"     , types: [ArgumentType.UniqueID, ArgumentType.String]
		
		model.add predicate: "hasInternalElementAttributeName"     , types: [ArgumentType.UniqueID, ArgumentType.String]

		model.add predicate: "hasSystemUnitClassAttributeName"     , types: [ArgumentType.UniqueID, ArgumentType.String]

		model.add predicate: "hasSystemUnitClassLibAttributeName"     , types: [ArgumentType.UniqueID, ArgumentType.String]

		model.add predicate: "hasRoleClassAttributeName"     , types: [ArgumentType.UniqueID, ArgumentType.String]

		model.add predicate: "hasRoleClassLibAttributeName"     , types: [ArgumentType.UniqueID, ArgumentType.String]

		model.add predicate: "hasInternalLinkAttributeName"     , types: [ArgumentType.UniqueID, ArgumentType.String]

		model.add predicate: "hasAttributeValue"     , types: [ArgumentType.UniqueID, ArgumentType.String]

		model.add predicate: "hasCAEXFile"     , types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

		model.add predicate: "hasExternalReference"     , types: [ArgumentType.UniqueID, ArgumentType.String]

		model.add predicate: "hasType"     , types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

		model.add predicate: "notSimilar"     , types: [ArgumentType.UniqueID, ArgumentType.UniqueID]


	}

	/**
	 * TODO to write comments
	 */
	public void defineFunctions()
	{
		try{
			model.add function: "similarValue"  , implementation: new DiceSimilarity()
		}catch(Exception e){

		}
	}

	/**
	 * TODO to write comments
	 */
	public void defineRules()
	{

		// Rule:1  Two AML CAEX files are the same if they have the same path
		model.add rule : (hasCAEXFile(A,X) & hasCAEXFile(B,Y) & hasExternalReference(X,Z)
		& hasExternalReference(Y,W) & similarValue(Z,W) & hasDocument(A,O1) & hasDocument(B,O2) &
		(O1-O2)) >> similar(X,Y) , weight : 8

		// Rule:2 Two AML hasAttributes are the same if their RefSemantic are the same
		model.add rule : (hasAttribute(A,X) & hasAttribute(B,Y) & hasRefSemantic(X,Z)
		& hasRefSemantic(Y,W) & similarValue(Z,W) & hasDocument(A,O1) & hasDocument(B,O2) &
		(O1-O2)) >> similar(A,B) , weight : 10

		// Rule:3 Two AMl hasAttributes are the same if they share the same ID
		model.add rule : (hasAttributeID(A,Z) & hasAttributeID(B,W) & similarValue(Z,W)
		& hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> similar(A,B), weight : 10

		// Rule:4 Two AML Attributes are the same if they have the same name
		model.add rule : (hasAttributeName(A,Z) & hasAttributeName(B,W) & similarValue(Z,W) &
		hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> similar(A,B), weight : 1

		// Rule:5 Two InterfaceClass are the same if they have the same name
		model.add rule : (hasInterfaceClassAttributeName(A,Z) & hasInterfaceClassAttributeName(B,W) & similarValue(Z,W) &
		hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> similar(A,B) , weight : 4

    	        // Rule:6 Two InterfaceClassLib are the same if they have the same name
	        model.add rule : (hasInterfaceClassLibAttributeName(R,T) & hasInterfaceClassLibAttributeName(Z,U) & similarValue(T,U) &
	        hasDocument(R,O1) & hasDocument(Z,O2) & (O1-O2)) >> similar(R,Z) , weight : 4

		// Rule:7 Two InstanceHierarichy  are the same if they have the same name
		model.add rule : (hasInstanceHierarchyAttributeName(A,Z) & hasInstanceHierarchyAttributeName(B,W) & similarValue(Z,W) &
		hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> similar(A,B) , weight : 4

		// Rule:8 Two InternalLink are the same if they have the same name
		model.add rule : (hasInternalLinkAttributeName(A,Z) & hasInternalLinkAttributeName(B,W) & similarValue(Z,W) &
		hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> similar(A,B) , weight : 4

		// Rule:9 Two InternalElement are the same if they have the same name
		model.add rule : (hasInternalElementAttributeName(A,Z) & hasInternalElementAttributeName(B,W) & similarValue(Z,W) &
		hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> similar(A,B) , weight : 1


		// Rule:10 Two RoleClassLib are the same if they have the same name
		model.add rule : (hasRoleClassLibAttributeName(A,Z) & hasRoleClassLibAttributeName(B,W) & similarValue(Z,W) &
		hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> similar(A,B) , weight : 4

		// Rule:11 Two RoleClass are the same if they have the same name
		model.add rule : (hasRoleClassAttributeName(A,Z) & hasRoleClassAttributeName(B,W) & similarValue(Z,W) &
		hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> similar(A,B) , weight : 4

		// Rule:12 Two SystemUnitClassLib are the same if they have the same name
		model.add rule : (hasSystemUnitClassLibAttributeName(M,P) & hasSystemUnitClassLibAttributeName(D,N) & similarValue(P,N) &
		hasDocument(M,O1) & hasDocument(D,O2) & (O1-O2)) >> similar(M,D) , weight : 4

		// Rule:13 Two SystemUnitClass are the same if they have the same name
		model.add rule : (hasSystemUnitClassAttributeName(A,Z) & hasSystemUnitClassAttributeName(B,W) & similarValue(Z,W) &
		hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> similar(A,B) , weight : 4

		// Rule:14 Two AML Attributes are the same if they have the same values
		model.add rule : (hasAttributeValue(A,Z) & hasAttributeValue(B,W) & similarValue(Z,W) &
		hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> similar(A,B) , weight : 1

		// Rule:15 Two AMl InternalElement are the same if they share the same ID
		model.add rule : (hasInternalElementID(A,Z) & hasInternalElementID(B,W)
		& similarValue(Z,W) &hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> similar(A,B) ,
		weight : 10

		// Rule:16 Two AMl ExternalElement are the same if they share the same ID
		model.add rule : (hasExternalInterfaceID(A,Z) & hasExternalInterfaceID(B,W)
		& similarValue(Z,W) &hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> similar(A,B) ,
		weight : 10

		// Rule:17 Two AMl InstanceHierarchy are the same if they share the same ID
		model.add rule : (hasInstanceHierarchyID(A,Z) & hasInstanceHierarchyID(B,W)
		& similarValue(Z,W) &hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> similar(A,B) ,
		weight : 10
		
		
		// Rule:18 Two Roles Class are same if its eclass,IRDI and classification class are the same
		model.add rule :( hasRoleClass(A1,B1) & hasRoleClass(A2,B2)& hasEClassIRDI(B1,Z)
		& hasEClassIRDI(B2,W) & similarValue(Z,W) & hasRoleClass(A1,C1) & hasRoleClass(A2,D2)
		&hasEClassVersion(C1,M) & hasEClassVersion(D2,N) & similarValue(M,N)& hasRoleClass(A1,E1) &
		hasRoleClass(A2,F2) & hasEClassVersion(E1,O) &hasEClassVersion(F2,L) & similarValue(O,L)
		& hasDocument(A1,O1) & hasDocument(A2,O2) & (O1-O2)) >> similar(A1,A2) , weight : 10

		// Rule:19 Two Interface Class are same if its eclass,IRDI and classification class are the same
		model.add rule :( hasInterfaceClass(A1,B1) & hasInterfaceClass(A2,B2)& hasEClassIRDI(B1,Z)
		& hasEClassIRDI(B2,W) & similarValue(Z,W)& hasInterfaceClass(A1,C1)
		& hasInterfaceClass(A2,D2)  & hasEClassVersion(C1,M) & hasEClassVersion(D2,N)
		& similarValue(M,N)& hasInterfaceClass(A1,E1) & hasInterfaceClass(A2,F2)
		& hasEClassVersion(E1,O) & hasEClassVersion(F2,L) & similarValue(O,L)
		& hasDocument(A1,O1) & hasDocument(A2,O2) & (O1-O2)) >> similar(A1,A2) , weight : 10

		// Rule:20 Two SystemUnit Class are same if its eclass,IRDI and classification class are the same
		model.add rule :( hasSystemUnitClass(A1,B1) & hasSystemUnitClass(A2,B2) & hasEClassIRDI(B1,Z)
		& hasEClassIRDI(B2,W) & similarValue(Z,W) & hasSystemUnitClass(A1,C1)
		& hasSystemUnitClass(A2,D2) & hasEClassVersion(C1,M) & hasEClassVersion(D2,N)
		& similarValue(M,N)& hasSystemUnitClass(A1,E1) & hasSystemUnitClass(A2,F2)
		& hasEClassVersion(E1,O) & hasEClassVersion(F2,L) & similarValue(O,L)
		& hasDocument(A1,O1) & hasDocument(A2,O2) & (O1-O2)) >> similar(A1,A2) , weight : 10

		// Rule:21 1Internal Elements Set is same if it has same InternalLink
		model.add rule : (hasInternalElement(A,Z) & hasInternalElement(B,W)
		& hasInternalLink(Z,C) & hasInternalElementID(B,D)  & similarValue(C,D)
		& hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> similar(A,B) ,
		weight : 6

		// constraints
		model.add PredicateConstraint.PartialFunctional , on : similar
		model.add PredicateConstraint.PartialInverseFunctional , on : similar
		model.add PredicateConstraint.Symmetric , on : similar

		// prior
		model.add rule : ~similar(A,B), weight: 1
	}

	/**
	 * Rules for not similar
	 */
	public void defineNotSimilarRules(){

		// Two AML CAEX files are the not same if they have the not same path
		model.add rule : (hasCAEXFile(A,X) & hasCAEXFile(B,Y) & hasExternalReference(X,Z)
		& hasExternalReference(Y,W) & ~similarValue(Z,W) & hasDocument(A,O1) & hasDocument(B,O2) &
		(O1-O2)) >> notSimilar(X,Y) , weight : 8

		// Two AML hasAttributes are the not same if their RefSemantic are the not same
		model.add rule : (hasAttribute(A,X) & hasAttribute(B,Y) & hasRefSemantic(X,Z)
		& hasRefSemantic(Y,W) & ~similarValue(Z,W) & hasDocument(A,O1) & hasDocument(B,O2) &
		(O1-O2)) >> notSimilar(A,B) , weight : 10

		// Two AML Attributes are not the same if they have the same name
		model.add rule : (hasAttributeName(A,Z) & hasAttributeName(B,W) & ~similarValue(Z,W) &
		hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> ~similar(A,B), weight : 10


		// Two AMl hasAttributes are the not same if they share the not same ID
		model.add rule : (hasAttributeID(A,Z) & hasAttributeID(B,W) & ~similarValue(Z,W)
		& hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> notSimilar(A,B) ,weight : 10

		// Two InterfaceClass are not the same if they have the same name
		model.add rule : (hasInterfaceClassAttributeName(A,Z) & hasInterfaceClassAttributeName(B,W) & ~similarValue(Z,W) &
		hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> notSimilar(A,B) , weight : 4

		// Two InternalElement are not the same if they have the same name
		model.add rule : (hasInternalElementAttributeName(A,Z) & hasInternalElementAttributeName(B,W) & ~similarValue(Z,W) &
		hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> notSimilar(A,B) , weight : 4

		// Two RoleClass are not the same if they have the same name
		model.add rule : (hasRoleClassAttributeName(A,Z) & hasRoleClassAttributeName(B,W) & ~similarValue(Z,W) &
		hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> notsimilar(A,B) , weight : 4

		// Two RoleClassLib are not the same if they have the same name
		model.add rule : (hasRoleClassLibAttributeName(A,Z) & hasRoleClassLibAttributeName(B,W) & ~similarValue(Z,W) &
		hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> notsimilar(A,B) , weight : 4

		// Two SystemUnitClassLib are the same if they have the same name
		model.add rule : (hasSystemUnitClassLibAttributeName(A,Z) & hasSystemUnitClassLibAttributeName(B,W) & ~similarValue(Z,W) &
		hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> notSimilar(A,B) , weight : 4

		// Two SystemUnitClass are not the same if they have the same name
		model.add rule : (hasSystemUnitClassAttributeName(A,Z) & hasSystemUnitClassAttributeName(B,W) & ~similarValue(Z,W) &
		hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> notSimilar(A,B) , weight : 4

		// Two AML Attributes are the not same if they have the not same values
		model.add rule : (hasAttribute(A,X) & hasAttribute(B,Y) & hasAttributeValue(A,Z) &
		hasAttributeValue(B,W) & ~similarValue(Z,W) & hasDocument(A,O1) & hasDocument(B,O2) &
		(O1-O2)) >> notSimilar(A,B) , weight : 1

		// Two AMl hasInternalElement are the not same if they have a different ID
		model.add rule : (hasInternalElementID(A,Z) & hasInternalElementID(B,W)
		& ~similarValue(Z,W) &hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> notSimilar(A,B) ,
		weight : 10

		// Internal Elements Set is not same if it has same InternalLink
		model.add rule : (hasInternalElement(A,Z) & hasInternalElement(B,W)
		& hasInternalLink(Z,C) & hasInternalElementID(B,D)  & ~similarValue(C,D)
		& hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> notSimilar(A,B) ,
		weight : 10

		// Two Roles Class are not same if its eclass,IRDI and classification class are the same
		model.add rule :( hasRoleClass(A1,B1) & hasRoleClass(A2,B2)& hasEClassIRDI(B1,Z)
		& hasEClassIRDI(B2,W) & ~similarValue(Z,W) & hasRoleClass(A1,C1) & hasRoleClass(A2,D2)
		&hasEClassVersion(C1,M) & hasEClassVersion(D2,N) & ~similarValue(M,N)& hasRoleClass(A1,E1)&
		hasRoleClass(A2,F2) & hasEClassVersion(E1,O) &hasEClassVersion(F2,L) & ~similarValue(O,L)
		& hasDocument(A1,O1) & hasDocument(A2,O2) & (O1-O2)) >> notSimilar(A1,A2) , weight : 10

		// Two Interface Class are not same if its eclass,IRDI and classification class are the same
		model.add rule :( hasInterfaceClass(A1,B1) & hasInterfaceClass(A2,B2)& hasEClassIRDI(B1,Z)
		& hasEClassIRDI(B2,W) & ~similarValue(Z,W)& hasInterfaceClass(A1,C1)
		& hasInterfaceClass(A2,D2)  & hasEClassVersion(C1,M) & hasEClassVersion(D2,N)
		& ~similarValue(M,N)& hasInterfaceClass(A1,E1) & hasInterfaceClass(A2,F2)
		& hasEClassVersion(E1,O) & hasEClassVersion(F2,L) & ~similarValue(O,L)
		& hasDocument(A1,O1) & hasDocument(A2,O2) & (O1-O2)) >> notSimilar(A1,A2) , weight : 10

		// Two SystemUnit Class are not same if its eclass,IRDI and classification class are the same
		model.add rule :( hasSystemUnitClass(A1,B1) & hasSystemUnitClass(A2,B2) & hasEClassIRDI(B1,Z)
		& hasEClassIRDI(B2,W) & ~similarValue(Z,W) & hasSystemUnitClass(A1,C1)
		& hasSystemUnitClass(A2,D2) & hasEClassVersion(C1,M) & hasEClassVersion(D2,N)
		& ~similarValue(M,N)& hasSystemUnitClass(A1,E1) & hasSystemUnitClass(A2,F2)
		& hasEClassVersion(E1,O) & hasEClassVersion(F2,L) & ~similarValue(O,L)
		& hasDocument(A1,O1) & hasDocument(A2,O2) & (O1-O2)) >> notSimilar(A1,A2) , weight : 10

		// Two AML InstanceHierarchy are not the same if they do not share the same ID
		model.add rule : (hasInstanceHierarchyID(A,Z) & hasInstanceHierarchyID(B,W)
		& ~similarValue(Z,W) & hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> notSimilar(A,B) ,
		weight : 10

		// Two AMl ExternalElement are not the same if they dont share the same ID
		model.add rule : (hasExternalInterfaceID(A,Z) & hasExternalInterfaceID(B,W)
		& ~similarValue(Z,W) &hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> notSimilar(A,B) ,
		weight : 10

		// Two InstanceHierarichy  are not the same if they dont have the same name
		model.add rule : (hasInstanceHierarchyAttributeName(A,Z) & hasInstanceHierarchyAttributeName(B,W)
		& ~similarValue(Z,W) &
		hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> notSimilar(A,B) , weight : 4

		// Two InternalLink are not the same if they dont have the same name
		model.add rule : (hasInternalLinkAttributeName(A,Z) & hasInternalLinkAttributeName(B,W) &
		~similarValue(Z,W) &hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> notSimilar(A,B) ,
		weight : 4


	}

	/**
	 * Predicates for set or or collective inference
	 */
	public void defineSetPredicates(){
		model.add predicate: "setSimilar"     , types: [ArgumentType.UniqueID]
		model.add predicate: "setNotSimilar"     , types: [ArgumentType.UniqueID, ArgumentType.UniqueID]
		model.add setcomparison: "similarAttributes", using: SetComparison.Equality, on : setNotSimilar
	}

	/**
	 * Rules for sets, or collective inference
	 */
	public void defineSetRules(){

	}


	/**
	 * Defines typical ontology predicates
	 */
	public void defineOntoPredicates(){
		model.add predicate: "subclass", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

		model.add predicate: "hasDomain", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

		model.add predicate: "hasRange", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

		model.add predicate: "fromOntology", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]
	}

	/**
	 * Defines basic ontology rules
	 */
	public void defineOntoRules(){

		model.add rule : (hasDomain(R,A) & hasDomain(T,B) & similar(A,B) &
		hasDocument(A,O1) & hasDocument(B,O2) & (O1-O2)) >> similar(R,T), weight : 2;

		model.add rule : (hasRange(R,A) & hasRange(T,B) & similar(A,B) & hasDocument(A,O1) &
		hasDocument(B,O2) & (O1-O2)) >> similar(R,T), weight : 2;

		model.add rule : (hasDomain(R,A) & hasDomain(T,B) & similar(R,T) & hasDocument(A,O1) &
		hasDocument(B,O2) & (O1-O2)) >> similar(A,B), weight : 2;

		model.add rule : (hasRange(R,A)  & hasRange(T,B) & similar(R,T) & hasDocument(A,O1) &
		hasDocument(B,O2) & (O1-O2)) >> similar(A,B), weight : 2;

		GroundTerm classID = data.getUniqueID("class");
		model.add rule : (similar(A,B) & hasType(A, classID) & hasType(B, classID) & subclass(A, S1)
		& subclass(B, S2)& hasDocument(A,O1) & hasDocument(B,O2) &
		(O1-O2)) >> similar(S1, S2), weight: 3;
	}

	/**
	 * TODO to write comments 
	 */
	public void setUpData()
	{
		// Loads data 
		dir = 'data' + java.io.File.separator + 'document' + java.io.File.separator

		// train setup 

		testDir = util.ConfigManager.getFilePath() + "PSL/test/"
		trainDir = util.ConfigManager.getFilePath() + "PSL/train/"

		Partition trainObservations = new Partition(0)
		Partition trainPredictions = new Partition(1)
		Partition truth = new Partition(2)
		Partition other = new Partition(7)

		if(util.ConfigManager.getExecutionMethod() == "true"){

			createFiles(trainDir + "similar.txt")

			for (Predicate p : [
				hasDocument,
				hasAttribute,
				hasRefSemantic,
				hasInternalElementID,
				hasAttributeID,
				hasInternalLink,
				hasEClassVersion,
				hasEClassClassificationClass,
				hasEClassIRDI,
				hasRoleClass,
				hasInternalElement,
				hasSystemUnitClass,
				hasInterfaceClass,
				hasAttributeValue,
				hasAttributeName,
				hasInterfaceClassAttributeName,
				hasInternalElementAttributeName,
				hasRoleClassAttributeName,
				hasSystemUnitClassAttributeName,
				hasExternalReference,
				hasCAEXFile,
				hasInstanceHierarchyID,
				hasInstanceHierarchy,
				hasDomain,
				hasRange,
				hasType,
				hasInstanceHierarchyAttributeName,
				hasRoleClassLib,
				hasRoleClassLibAttributeName,
				hasSystemUnitClassLibAttributeName,
				hasExternalInterfaceID,
				hasInternalLinkAttributeName,
				hasInterfaceClassLibAttributeName
			]
			){
				createFiles(trainDir + p.getName().toLowerCase() + ".txt")
				def insert = data.getInserter(p, trainObservations)
				InserterUtils.loadDelimitedData(insert, trainDir + p.getName().toLowerCase()  +  ".txt")
			}

			def insert  =  data.getInserter(similar, truth)
			InserterUtils.loadDelimitedDataTruth(insert, trainDir + "similar.txt")

			trainDB = data.getDatabase(trainPredictions, [
				hasDocument,
				hasAttribute,
				hasRefSemantic,
				hasInternalElementID,
				hasAttributeID,
				hasInternalLink,
				hasEClassVersion,
				hasEClassClassificationClass,
				hasEClassIRDI,
				hasRoleClass,
				hasInternalElement,
				hasSystemUnitClass,
				hasInterfaceClass,
				hasAttributeValue,
				hasAttributeName,
				hasInterfaceClassAttributeName,
				hasInternalElementAttributeName,
				hasRoleClassAttributeName,
				hasSystemUnitClassAttributeName,
				hasExternalReference,
				hasCAEXFile,
				hasInstanceHierarchyID,
				hasInstanceHierarchy,
				hasDomain,
				hasRange,
				hasType,
				hasInstanceHierarchyAttributeName,
				hasRoleClassLib,
				hasRoleClassLibAttributeName,
				hasSystemUnitClassLibAttributeName,
				hasExternalInterfaceID,
				hasInternalLinkAttributeName,
				hasInterfaceClassLibAttributeName
			]
			as Set, trainObservations)

			populateSimilar(trainDB)
			truthDB = data.getDatabase(truth, [similar] as Set)

			println "before training" + model

			println "LEARNING WEIGHTS..."
			MaxLikelihoodMPE weightLearning = new MaxLikelihoodMPE(model, trainDB, truthDB, config)
			weightLearning.learn()
			weightLearning.close()
			println "LEARNING WEIGHTS DONE"
			println "after training" + model
		}

		// test setup 

		Partition testObservations = new Partition(3)
		Partition testPredictions = new Partition(4)

		for (Predicate p : [
			hasDocument,
			hasAttribute,
			hasRefSemantic,
			hasInternalElementID,
			hasAttributeID,
			hasInternalLink,
			hasEClassVersion,
			hasEClassClassificationClass,
			hasEClassIRDI,
			hasRoleClass,
			hasInternalElement,
			hasSystemUnitClass,
			hasInterfaceClass,
			hasAttributeValue,
			hasAttributeName,
			hasInterfaceClassAttributeName,
			hasInternalElementAttributeName,
			hasRoleClassAttributeName,
			hasSystemUnitClassAttributeName,
			hasExternalReference,
			hasCAEXFile,
			hasInstanceHierarchyID,
			hasInstanceHierarchy,
			hasDomain,
			hasRange,
			hasType,
			hasInstanceHierarchyAttributeName,
			hasRoleClassLib,
			hasRoleClassLibAttributeName,
			hasSystemUnitClassLibAttributeName,
			hasExternalInterfaceID,
			hasInternalLinkAttributeName,
			hasInterfaceClassLibAttributeName

		])
		{
			createFiles(testDir + p.getName().toLowerCase() + ".txt")
		}
		createFiles(testDir + "similar.txt")
		createFiles(testDir + "similarwithConfidence.txt")
		createFiles(testDir + "GoldStandard.txt")

		for (Predicate p : [
			hasDocument,
			hasAttribute,
			hasRefSemantic,
			hasInternalElementID,
			hasAttributeID,
			hasInternalLink,
			hasEClassVersion,
			hasEClassClassificationClass,
			hasEClassIRDI,
			hasRoleClass,
			hasInternalElement,
			hasSystemUnitClass,
			hasInterfaceClass,
			hasAttributeValue,
			hasAttributeName,
			hasInterfaceClassAttributeName,
			hasInternalElementAttributeName,
			hasRoleClassAttributeName,
			hasSystemUnitClassAttributeName,
			hasExternalReference,
			hasCAEXFile,
			hasInstanceHierarchyID,
			hasInstanceHierarchy,
			hasDomain,
			hasRange,
			hasType,
			hasInstanceHierarchyAttributeName,
			hasRoleClassLib,
			hasRoleClassLibAttributeName,
			hasSystemUnitClassLibAttributeName,
			hasExternalInterfaceID,
			hasInternalLinkAttributeName,
			hasInterfaceClassLibAttributeName
		])
		{

			def insert  =  data.getInserter(p, testObservations)

			InserterUtils.loadDelimitedData(insert, testDir  +  p.getName().toLowerCase()  +  ".txt")
		}


		testDB  =  data.getDatabase(testPredictions, [
			hasDocument,
			hasAttribute,
			hasRefSemantic,
			hasInternalElementID,
			hasAttributeID,
			hasInternalLink,
			hasEClassVersion,
			hasEClassClassificationClass,
			hasRoleClass,
			hasEClassIRDI,
			hasInternalElement,
			hasSystemUnitClass,
			hasInterfaceClass,
			hasAttributeValue,
			hasAttributeName,
			hasInterfaceClassAttributeName,
			hasInternalElementAttributeName,
			hasRoleClassAttributeName,
			hasSystemUnitClassAttributeName,
			hasExternalReference,
			hasCAEXFile,
			hasInstanceHierarchyID,
			hasInstanceHierarchy,
			hasDomain,
			hasRange,
			hasType,
			hasInstanceHierarchyAttributeName,
			hasRoleClassLib,
			hasRoleClassLibAttributeName,
			hasSystemUnitClassLibAttributeName,
			hasExternalInterfaceID,
			hasInternalLinkAttributeName,
			hasInterfaceClassLibAttributeName

		] as Set, testObservations)

		populateSimilar(testDB)
	}

	/**
	 * TODO to write comments
	 * @param testDir
	 */
	public void createFiles(String testDir){
		def dataFile = new File(testDir)
		if(!dataFile.exists()){
			dataFile.createNewFile()
		}
	}

	/**
	 * TODO to write comments
	 * @param matchResult
	 * @param symResult
	 * @return
	 */
	int removeSymetric(File matchResult,String symResult){
		def flag = 0
		def lineNo = 1
		def line
		matchResult.withReader { reader ->
			while ((line = reader.readLine())!=null) {
				if(line.replace("\t","").replace(" ","")
					   .contains(symResult.replace("\t","").replace(" ",""))){
					return flag = 1
				}
				lineNo++
			}
		}
		return flag
	}

	/**
	 * Checks if negative values have more confidence or not.
	 * @param confResult
	 * @param symResult
	 * @param value
	 * @return TODO to write what returns
	 */
	int checkConfidence(File confResult, String symResult, double value){
		def flag = 0
		def lineNo = 1
		def line
		confResult.withReader{ reader ->
			while ((line = reader.readLine()) != null) {
				if(line.replace("\t","").contains(symResult.replace("\t",""))){
					String temp = line.replace(symResult,"").trim();
					try{
						double trueValue = temp.toDouble()
						if(trueValue > value){
							return flag = 1;
						}
					}catch(Exception e){
					   // TODO to handle this exception
					}
				}
				lineNo++
			}
		}
		return flag
	}

	/**
	 * Adds rules explanation
	 * @param predicate
	 * @param filename
	 */
	public void ruleExplaination(Predicate predicate,String filename){

		def matchResult = new File(testDir + "Precision/" + filename)
		matchResult.write('')

		for (GroundAtom atom : Queries.getAllAtoms(testDB, predicate)){
			Iterator igk = atom.getRegisteredGroundKernels().iterator();
			while(igk.hasNext()) {
				Iterator<GroundAtom> iga = igk.next().getAtoms().iterator();
				while (iga.hasNext()) { //gets predicate
					GroundAtom gatom = iga.next();
					if(gatom.getValue() > 1.0){
						matchResult.append(gatom.getRegisteredGroundKernels() + "\t"
								    + gatom.getValue() + "\n \n")
					}
				}}}
	}


	/**
	 * TODO to write comments
	 */
	public void runInference(){
		println "INFERRING..."

		MPEInference inference = new MPEInference(model, testDB, config)
		inference.mpeInference()
		inference.close()

		println "INFERENCE DONE"
		def matchResult = new File(testDir + 'similar.txt')
		matchResult.write('')
		String allResultConfidence = "";
		def resultConfidence = new File(testDir + 'similarwithConfidence.txt')
		resultConfidence.write('')
		DecimalFormat formatter = new DecimalFormat("#.##")
		
		// populates values with confidence required to check if not similar has more confidence
		for (GroundAtom atom : Queries.getAllAtoms(testDB, notSimilar)){
			String result = atom.toString().replaceAll("NOTSIMILAR","")
			result = result.replaceAll("[()]","")
			String[] text = result.split(",")
			String result2 = text[0].trim() + "\t" + text[1].trim() + " " + atom.getValue()
			def symResult2 = text[1].trim() + "," + text[0].trim() + " " + atom.getValue()
			
			if(formatter.format(atom.getValue()) > threshold){
				if(text[0].toString().contains("aml1")){
					allResultConfidence += result2 + '\n';
				}
				else{
					allResultConfidence += symResult2 + '\n';
				}
			}
		}

		resultConfidence.append(allResultConfidence + '\n')

		String allResultsPositive = "";
		String allResultsPositiveConf = "";
		for (GroundAtom atom : Queries.getAllAtoms(testDB, similar)){
			//println atom.toString()  +  ": "  +  formatter.format(atom.getValue())

			// only writes if its equal to 1 or u can set the threshold
			if(formatter.format(atom.getValue()) > threshold){
				// converting to format for evaluation
				String result  =  atom.toString().replaceAll("SIMILAR","")
				result  = result.replaceAll("[()]","")
				String[] text  = result.split(",")
				result = text[0].trim()  +  ","  +  text[1].trim() +  "," + "truth:1"
				def symResult = text[1].trim()  +  ","  +  text[0].trim() +  "," + "truth:1"
				def symResult2 = text[1].trim()  +  "\t"  +  text[0].trim()
				String result2 = text[0].trim()  +  "\t"  +  text[1].trim()

				// adding elements with aml1: at start for correctness
				if(!removeSymetric(matchResult,symResult)&&
				!removeSymetric(matchResult,result)&&
				!checkConfidence(resultConfidence,symResult2,atom.getValue())&&
				!checkConfidence(resultConfidence,result2,atom.getValue())){
					if(text[0].toString().contains("aml1")){
						allResultsPositive+=result+'\n'
						allResultsPositiveConf+=result2+ " " + atom.getValue()+'\n';
					}
					else{
						allResultsPositive+=symResult  +  '\n'
						allResultsPositiveConf+=symResult2+ " " + atom.getValue()	  +  '\n';
					}
				}}
		}
		matchResult.append(allResultsPositive  +  '\n')
		resultConfidence.append(allResultsPositiveConf+ '\n')


		String allResultNegative="";
		for (GroundAtom atom : Queries.getAllAtoms(testDB, notSimilar)){
			//	println atom.toString()  +  ": "  +  formatter.format(atom.getValue())

			// only writes if its equal to 1 or u can set the threshold
			if(formatter.format(atom.getValue()) > threshold){

				// converting to format for evaluation
				String result = atom.toString().replaceAll("NOTSIMILAR","")
				result = result.replaceAll("[()]","")
				String[] text  =  result.split(",")
				result =  text[0].trim() + "," + text[1].trim() + "," + "0"
				def symResult = text[1].trim() + "," + text[0].trim() + "," + "0"
				def trueResult = text[0].trim() + "," + text[1].trim() + "," + "truth:1"
				def trueSymResult = text[1].trim() + "," + text[0].trim() + "," + "truth:1"

				if(!removeSymetric(matchResult,symResult)&&
				!removeSymetric(matchResult,result)	&&
				!removeSymetric(matchResult,trueSymResult)&&
				!removeSymetric(matchResult,trueResult)){
					if(text[0].toString().contains("aml1")){
						allResultNegative += result + '\n'
					}

					else{
						allResultNegative += symResult + '\n'
					}
				}
			}
		}

		matchResult.append(allResultNegative)
		//ruleExplaination(notSimilar,"notSimilarRules.txt");
		//ruleExplaination(similar,"SimilarRules.txt");
	}

	/**
	 * Evaluates the results of inference versus expected truth values
	 */
	public void evalResults() {
		targetsPartition = new Partition(5)
		truthPartition = new Partition(6)

		File file = new File(util.ConfigManager.getFilePath()+"GoldStandard.txt");
		if (!file.exists()) {
			System.out.println("Error :: GoldStandard Missing in" + testDir + "model");
			System.exit(0);
		}

		
		def insert = data.getInserter(eval, targetsPartition)
		InserterUtils.loadDelimitedDataTruth(insert, testDir + "similar.txt")

		insert  =  data.getInserter(eval, truthPartition)

		InserterUtils.loadDelimitedDataTruth(insert, util.ConfigManager.getFilePath() + "GoldStandard.txt")

		Database resultsDB = data.getDatabase(targetsPartition, [eval] as Set)
		Database truthDB = data.getDatabase(truthPartition, [eval] as Set)
		DiscretePredictionComparator dpc = new DiscretePredictionComparator(resultsDB)
		dpc.setBaseline(truthDB)
		DiscretePredictionStatistics stats = dpc.compare(eval)

		Double F1 = stats.getF1(DiscretePredictionStatistics.BinaryClass.POSITIVE)
		Double precision = stats.getPrecision(DiscretePredictionStatistics.
				BinaryClass.POSITIVE)
		Double recall = stats.getRecall(DiscretePredictionStatistics.
				BinaryClass.POSITIVE)

		System.out.println("Accuracy:" + stats.getAccuracy().round(2))
		System.out.println("Error:" + stats.getError())
		System.out.println("True Positive:" + stats.tp)
		System.out.println("True Negative:" + stats.tn)
		System.out.println("False Positive:" + stats.fp)
		System.out.println("False Negative:" + stats.fn)
		System.out.println("Precision:" + precision.round(2))
		System.out.println("Recall:" + recall.round(2))
		System.out.println("Fmeasure:" + F1.round(2))

		// Saving Precision and Recall results to file
		def resultsFile

		//Creating an unique name for the file
		def arrDir = testDir.split("/")
		def het = arrDir[4]
		def number = arrDir[5].split("-")

		if(util.ConfigManager.getExecutionMethod() == "true"){
			resultsFile = new File(testDir + "Precision/" + arrDir[4] + "-" +
					number[1] + "F1Training.txt")
		}
		else{
			resultsFile = new File(testDir + "Precision/" + "F1NoTraining.txt")
		}

		resultsFile.createNewFile()
		resultsFile.write("")
		resultsFile.append("Accuracy:" + stats.getAccuracy().round(2) + '\n')
		resultsFile.append("Error:" + stats.getError() + '\n')
		resultsFile.append("Fmeasure:" + F1.round(2) +  '\n')
		resultsFile.append("True Positive:" + stats.tp + '\n')
		resultsFile.append("True Negative:" + stats.tn + '\n')
		resultsFile.append("False Positive:" + stats.fp + '\n')
		resultsFile.append("False Negative:" + stats.fn + '\n')
		resultsFile.append("Precision :" + precision.round(2) + '\n')
		resultsFile.append("Recall: " + recall.round(2) + '\n')
		resultsFile.close()
		resultsDB.close()
		truthDB.close()
	}


	/**
	 * Populates all the similar atoms between the concepts of two Documents using
	 * the hasDocument predicate.
	 * @param db The database to populate. It should contain the hasDocument atoms
	 */
	void populateSimilar(Database db) {
		/* Collects the ontology concepts */
		Set<GroundAtom> concepts  =  Queries.getAllAtoms(db, hasDocument)
		Set<GroundTerm> o1  =  new HashSet<GroundTerm>()
		Set<GroundTerm> o2  =  new HashSet<GroundTerm>()
		for (GroundAtom atom : concepts) {
			if (atom.getArguments()[1].toString().equals("aml1"))
				o1.add(atom.getArguments()[0])
			else
				o2.add(atom.getArguments()[0])
		}

		/* Populates manually (as opposed to using DatabasePopulator) */
		for (GroundTerm o1Concept : o1) {
			for (GroundTerm o2Concept : o2) {
				((RandomVariableAtom) db.getAtom(similar, o1Concept, o2Concept)).commitToDB()
				((RandomVariableAtom) db.getAtom(similar, o2Concept, o1Concept)).commitToDB()
				((RandomVariableAtom) db.getAtom(notSimilar, o1Concept, o2Concept)).commitToDB()
				((RandomVariableAtom) db.getAtom(notSimilar, o2Concept, o1Concept)).commitToDB()

			}
		}
	}

}
