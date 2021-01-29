package com.varra.jenkins.tools.codecoverage;

/**
 * @author Rajakrishna Reddy
 *
 */
public interface ReportGenerator<Request, Response>
{
	
	Response generate(Request request);
}
