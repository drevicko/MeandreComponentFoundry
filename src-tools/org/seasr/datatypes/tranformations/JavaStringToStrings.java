/**
 * 
 */
package org.seasr.datatypes.tranformations;

import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.Component.FiringPolicy;
import org.meandre.annotations.Component.Licenses;
import org.meandre.annotations.Component.Mode;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextException;
import org.meandre.core.ComponentContextProperties;
import org.meandre.core.ComponentExecutionException;
import org.meandre.core.ExecutableComponent;
import org.meandre.core.system.components.ext.StreamDelimiter;
import org.seasr.datatypes.BasicDataTypesTools;
import org.seasr.meandre.components.tools.Names;


/** This component transforms a token count into a map.
 * 
 * @author Xavier Llor�
 *
 */
@Component(
		name = "Java String To Strings",
		creator = "Xavier Llora",
		baseURL = "meandre://seasr.org/components/tools/",
		firingPolicy = FiringPolicy.all,
		mode = Mode.compute,
		rights = Licenses.UofINCSA,
		dependency = {"protobuf-java-2.0.3.jar"},
		tags = "tools, tokenizer, counting, transformations",
		description = "Converts a Java string into an equivalent string protocol buffer wrapper."
)
public class JavaStringToStrings implements ExecutableComponent  {
	
	//--------------------------------------------------------------------------------------------
	
	//--------------------------------------------------------------------------------------------
		
	@ComponentInput(
			name = Names.PORT_JAVA_STRING,
			description = "The java string to convert"
		)
	private final static String INPUT_JAVA_STRING = Names.PORT_JAVA_STRING;
	
	@ComponentOutput(
			name = Names.PORT_TEXT,
			description = "The converted text"
		)
	private final static String OUPUT_TEXT = Names.PORT_TEXT;
		
	//--------------------------------------------------------------------------------------------
	
	//--------------------------------------------------------------------------------------------
	
	/**
	 * @see org.meandre.core.ExecutableComponent#execute(org.meandre.core.ComponentContext)
	 */
	public void execute(ComponentContext cc)
			throws ComponentExecutionException, ComponentContextException {
		Object obj = cc.getDataComponentFromInput(INPUT_JAVA_STRING);
		if ( obj instanceof StreamDelimiter ) 
			cc.pushDataComponentToOutput(OUPUT_TEXT, obj);
		else {
			try {
				String s = (String)obj;
				cc.pushDataComponentToOutput(OUPUT_TEXT, BasicDataTypesTools.stringToStrings(s));
			} catch (ClassCastException e ) {
				String sMessage = "Input data is not a sequence of token countss";
				cc.getLogger().warning(sMessage);
				cc.getOutputConsole().println("WARNING: "+sMessage);
				throw new ComponentExecutionException(e);
			}
		}
	}

	public void dispose(ComponentContextProperties arg0)
			throws ComponentExecutionException, ComponentContextException {
		// TODO Auto-generated method stub
		
	}

	public void initialize(ComponentContextProperties arg0)
			throws ComponentExecutionException, ComponentContextException {
		// TODO Auto-generated method stub
		
	}

	//--------------------------------------------------------------------------------------------

}
