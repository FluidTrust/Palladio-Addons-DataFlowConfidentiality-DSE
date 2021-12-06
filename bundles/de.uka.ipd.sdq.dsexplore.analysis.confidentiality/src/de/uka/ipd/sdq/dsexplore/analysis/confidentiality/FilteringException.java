package de.uka.ipd.sdq.dsexplore.analysis.confidentiality;

import org.eclipse.core.runtime.CoreException;

/**
 * This exception should be thrown when an exception occurs during {@link FilteringAnalysis#runAnalysis}.
 * @author Oliver
 *
 */
public class FilteringException extends Exception {

	private static final long serialVersionUID = 3972201576579538188L;

	public FilteringException() {
		super();
	}
	
	public FilteringException(String string) {
		super(string);
	}
	
	public FilteringException(String string, Throwable e) {
		super(string, e);
	}
	
	public FilteringException(CoreException e) {
		super(e);
	}
}
