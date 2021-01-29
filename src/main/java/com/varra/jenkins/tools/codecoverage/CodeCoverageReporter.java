package com.varra.jenkins.tools.codecoverage;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.varra.util.StopWatch;

import static java.lang.System.setProperty;

/**
 * @author Rajakrishna Reddy
 *
 */
public class CodeCoverageReporter 
{
	public static void main(String... args) throws IOException
	{
		setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "50");
		final String url = "https://localhost:9273/view/1.%20Main/job/JaCoCo%20Code%20Coverage%20-%20main/lastBuild/jacoco";
		final StopWatch watch = new StopWatch();
		final Path path = Paths.get("./code-coverage-report.html");
		final List<ConcurrentResultParser.PackageEntryInfo> list = new ConcurrentResultParser().getParsedPackageDetails(new URL(url));
		list.sort(null);
		new HTMLFileReportGenerator(path).generate(list);
		System.out.println("Report has been created at: "+path);
		System.out.println(watch);
	}
}
