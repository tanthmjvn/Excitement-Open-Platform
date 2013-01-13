package ac.biu.nlp.nlp.engineml.small_unit_tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import eu.excitementproject.eop.common.datastructures.immutable.ImmutableSet;
import eu.excitementproject.eop.common.representation.partofspeech.CanonicalPosTag;
import eu.excitementproject.eop.common.representation.partofspeech.PartOfSpeech;
import eu.excitementproject.eop.common.representation.partofspeech.UnspecifiedPartOfSpeech;
import eu.excitementproject.eop.common.representation.partofspeech.UnsupportedPosTagStringException;
import eu.excitementproject.eop.common.utilities.configuration.ConfigurationException;
import eu.excitementproject.eop.common.utilities.configuration.ConfigurationFileDuplicateKeyException;

import ac.biu.nlp.nlp.engineml.operations.OperationException;
import ac.biu.nlp.nlp.engineml.operations.rules.ByLemmaPosLexicalRuleBase;
import ac.biu.nlp.nlp.engineml.operations.rules.LexicalRule;
import ac.biu.nlp.nlp.engineml.operations.rules.RuleBaseException;
import ac.biu.nlp.nlp.engineml.plugin.PluginAdministrationException;
import ac.biu.nlp.nlp.engineml.rteflow.systems.ConfigurationParametersNames;
import ac.biu.nlp.nlp.engineml.rteflow.systems.SystemInitialization;
import ac.biu.nlp.nlp.engineml.script.OperationsScript;
import ac.biu.nlp.nlp.engineml.script.ScriptFactory;
import ac.biu.nlp.nlp.engineml.utilities.LogInitializer;
import ac.biu.nlp.nlp.engineml.utilities.TeEngineMlException;
import ac.biu.nlp.nlp.instruments.lemmatizer.LemmatizerException;
import ac.biu.nlp.nlp.instruments.parse.representation.basic.Info;
import ac.biu.nlp.nlp.instruments.parse.tree.dependency.basic.BasicNode;

/**
 * 
 * @author Asher Stern
 * @since Aug 13, 2012
 *
 */
public class DemoLexicalResources extends SystemInitialization
{
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		try
		{
			DemoLexicalResources app = new DemoLexicalResources(args[0],ConfigurationParametersNames.RTE_PAIRS_TRAIN_AND_TEST_MODULE_NAME);
			new LogInitializer(args[0]).init();
			
			try
			{
				app.init();
				app.go();
			}
			finally
			{
				app.cleanUp();
			}
			
		}
		catch(Exception e)
		{
			e.printStackTrace(System.out);
		}
	}

	
	public DemoLexicalResources(String configurationFileName,
			String configurationModuleName)
	{
		super(configurationFileName, configurationModuleName);
		// TODO Auto-generated constructor stub
	}

	
	@Override
	public void init() throws ConfigurationFileDuplicateKeyException, MalformedURLException, TeEngineMlException, PluginAdministrationException, ConfigurationException, LemmatizerException, IOException
	{
		super.init();
		script = new ScriptFactory(this.configurationFile,this.teSystemEnvironment.getPluginRegistry()).getDefaultScript();
		try
		{
			script.init();
			scriptInitialized = true;
		}
		catch(OperationException e)
		{
			throw new TeEngineMlException("script init failed.",e);
			
		}
		super.completeInitializationWithScript(script);
	}
	
	@Override
	public void cleanUp()
	{
		super.cleanUp();
		if (scriptInitialized)
		{
			script.cleanUp();
		}
	}
	
	public void go() throws IOException, TeEngineMlException, UnsupportedPosTagStringException, RuleBaseException
	{
		List<RuleBaseAndName> lexicalRuleBases = new LinkedList<RuleBaseAndName>();
		for (String ruleBaseName : script.getRuleBasesNames())
		{
			ByLemmaPosLexicalRuleBase<LexicalRule> ruleBase = null;
			try
			{
				ruleBase = script.getByLemmaPosLexicalRuleBase(ruleBaseName);
			}
			catch (OperationException e)
			{
				// do nothing. it is not a lexical rule base.
				ruleBase = null;
			}
			
			if (ruleBase != null)
			{
				lexicalRuleBases.add(new RuleBaseAndName(ruleBase, ruleBaseName));
			}
		}
		

		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Please type lemma:pos");
		System.out.println("Type exit for exit");
		for (String line = reader.readLine();(!"exit".equalsIgnoreCase(line));line = reader.readLine())
		{
			String[] lineSplit = line.split(":");
			if (lineSplit.length==2)
			{
				String lhsLemma = lineSplit[0];
				PartOfSpeech lhsPos = fromString(lineSplit[1]);
				if (null!=lhsPos)
				{
					for (RuleBaseAndName ruleBaseAndName : lexicalRuleBases)
					{
						ByLemmaPosLexicalRuleBase<LexicalRule> ruleBase = ruleBaseAndName.getRuleBase();
						ImmutableSet<LexicalRule> rules = ruleBase.getRules(lhsLemma, lhsPos);
						if (rules!=null){ if (rules.size()>0)
						{
							System.out.println(ruleBaseAndName.getName());
							for (LexicalRule rule : rules)
							{
								System.out.println(rule.toString());
							}
						}}
					}
				}
				else
				{
					System.out.println("Illegal part-of-speech.");
				}
			}
			else
			{
				System.out.println("You should type lemma:pos");
			}
		}
	}
	
	private PartOfSpeech fromString(String str) throws UnsupportedPosTagStringException, TeEngineMlException
	{
		PartOfSpeech ret = null;
		if (mapPos!=null)
		{
			if (mapPos.containsKey(str))
				ret = mapPos.get(str);
		}
		else
		{
			throw new TeEngineMlException("map of part-of-speech is null");
		}
		return ret;
	}
	

	private static Map<String, PartOfSpeech> mapPos = null;
	static
	{
		try
		{
			mapPos = new LinkedHashMap<String, PartOfSpeech>();
			mapPos.put("V",new UnspecifiedPartOfSpeech(CanonicalPosTag.VERB));
			mapPos.put("N",new UnspecifiedPartOfSpeech(CanonicalPosTag.NOUN));
			
			for (CanonicalPosTag cpt : CanonicalPosTag.values())
			{
				mapPos.put(cpt.name(),new UnspecifiedPartOfSpeech(cpt));
			}
		}
		catch (UnsupportedPosTagStringException e)
		{
			mapPos = null;
		}
		finally{}
		
	}
	
	private class RuleBaseAndName
	{
		public RuleBaseAndName(ByLemmaPosLexicalRuleBase<LexicalRule> ruleBase,
				String name)
		{
			super();
			this.ruleBase = ruleBase;
			this.name = name;
		}
		public ByLemmaPosLexicalRuleBase<LexicalRule> getRuleBase(){return ruleBase;}
		public String getName(){return name;}
		
		private final ByLemmaPosLexicalRuleBase<LexicalRule> ruleBase;
		private final String name;
	}

	private OperationsScript<Info, BasicNode> script;
	private boolean scriptInitialized = false;
}
