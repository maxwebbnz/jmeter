// $Header$
/*
 * Copyright 2003-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package org.apache.jmeter.protocol.java.sampler;

import java.io.IOException;

import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.util.BeanShellInterpreter;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.jorphan.util.JMeterException;
import org.apache.jorphan.util.JOrphanUtils;
import org.apache.log.Logger;

/**
 * A sampler which understands BeanShell
 * 
 * @version $Revision$ Updated on: $Date$
 */
public class BeanShellSampler extends AbstractSampler {
	private static final Logger log = LoggingManager.getLoggerForClass();

	public static final String FILENAME = "BeanShellSampler.filename"; //$NON-NLS-1$

	public static final String SCRIPT = "BeanShellSampler.query"; //$NON-NLS-1$

	public static final String PARAMETERS = "BeanShellSampler.parameters"; //$NON-NLS-1$

	public static final String INIT_FILE = "beanshell.sampler.init"; //$NON-NLS-1$

	transient private BeanShellInterpreter bshInterpreter;

	public BeanShellSampler() {
		try {
			bshInterpreter = new BeanShellInterpreter();
			String init = JMeterUtils.getProperty(INIT_FILE);
			try {
				bshInterpreter.init(init, log);
			} catch (IOException e) {
				log.warn("Could not initialise interpreter", e);
			} catch (JMeterException e) {
				log.warn("Could not initialise interpreter", e);
			}
		} catch (ClassNotFoundException e) {
			log.error("Could not establish BeanShellInterpreter: " + e);
		}
	}

	/**
	 * Returns a formatted string label describing this sampler
	 * 
	 * @return a formatted string label describing this sampler
	 */

	public String getLabel() {
		return getName();
	}

	public String getScript() {
		return this.getPropertyAsString(SCRIPT);
	}

	public String getFilename() {
		return getPropertyAsString(FILENAME);
	}

	public String getParameters() {
		return getPropertyAsString(PARAMETERS);
	}

	public SampleResult sample(Entry e)// Entry tends to be ignored ...
	{
		// log.info(getLabel()+" "+getFilename());
		SampleResult res = new SampleResult();
		boolean isSuccessful = false;
		res.setSampleLabel(getLabel());
		res.sampleStart();
		if (bshInterpreter == null) {
			res.sampleEnd();
			res.setResponseCode("503");//$NON-NLS-1$
			res.setResponseMessage("BeanShell Interpreter not found");
			res.setSuccessful(false);
			return res;
		}
		try {
			String request = getScript();
			String fileName = getFilename();
			if (fileName.length() == 0) {
				res.setSamplerData(request);
			} else {
				res.setSamplerData(fileName);
			}

			bshInterpreter.set("Label", getLabel()); //$NON-NLS-1$
			bshInterpreter.set("FileName", getFilename()); //$NON-NLS-1$
			bshInterpreter.set("SampleResult", res); //$NON-NLS-1$
			bshInterpreter.set("Parameters", getParameters());// as a single
																// line//$NON-NLS-1$
			bshInterpreter.set("bsh.args", JOrphanUtils.split(getParameters(), " "));

			// Set default values
			bshInterpreter.set("ResponseCode", "200"); //$NON-NLS-1$
			bshInterpreter.set("ResponseMessage", "OK");//$NON-NLS-1$
			bshInterpreter.set("IsSuccess", true);//$NON-NLS-1$

			// Add variables for access to context and variables
			JMeterContext jmctx = JMeterContextService.getContext();
			JMeterVariables vars = jmctx.getVariables();
			bshInterpreter.set("ctx", jmctx);//$NON-NLS-1$
			bshInterpreter.set("vars", vars);//$NON-NLS-1$

            res.setDataType(SampleResult.TEXT); // assume text output - script can override if necessary

            Object bshOut;

			if (fileName.length() == 0) {
				bshOut = bshInterpreter.eval(request);
			} else {
				bshOut = bshInterpreter.source(fileName);
			}

			if (bshOut != null) {// Set response data
                String out = bshOut.toString();
                res.setResponseData(out.getBytes());
			}
            // script can also use setResponseData() so long as it returns null
            
			res.setResponseCode(bshInterpreter.get("ResponseCode").toString());//$NON-NLS-1$
			res.setResponseMessage(bshInterpreter.get("ResponseMessage").toString());//$NON-NLS-1$
			isSuccessful = Boolean.valueOf(bshInterpreter.get("IsSuccess") //$NON-NLS-1$
					.toString()).booleanValue();
		}
		/*
		 * To avoid class loading problems when bsh,jar is missing, we don't try
		 * to catch this error separately catch (bsh.EvalError ex) {
		 * log.debug("",ex); res.setResponseCode("500");//$NON-NLS-1$
		 * res.setResponseMessage(ex.toString()); }
		 */
		// but we do trap this error to make tests work better
		catch (NoClassDefFoundError ex) {
			log.error("BeanShell Jar missing? " + ex.toString());
			res.setResponseCode("501");//$NON-NLS-1$
			res.setResponseMessage(ex.toString());
			res.setStopThread(true); // No point continuing
		} catch (Exception ex) // Mainly for bsh.EvalError
		{
			log.warn(ex.toString());
			res.setResponseCode("500");//$NON-NLS-1$
			res.setResponseMessage(ex.toString());
		}

		res.sampleEnd();

		// Set if we were successful or not
		res.setSuccessful(isSuccessful);

		return res;
	}
}