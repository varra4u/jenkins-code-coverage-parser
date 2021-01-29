package com.varra.jenkins.tools.codecoverage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import com.varra.jenkins.tools.codecoverage.ConcurrentResultParser.PackageEntryInfo;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/**
 * @author Rajakrishna Reddy
 *
 */
public class HTMLFileReportGenerator implements ReportGenerator<List<PackageEntryInfo>, Void>
{
	private static final String HEADER_COLOR = "headerColor";
	private static final String LIGHT_BLUE = "#c8e3ff";
	private static final String LIGHT_YELLOW = "#f9f3c2";
	private static final String LIGHT_RED = "#ffcccc";
	private static final String LIGHT_GREEN = "#ccffcc";
	private static final String PERCENTAGE = "percentage";
	private static final String PACKAGE_COLOR = "packageColor";
	private static final String CLASS_COLOR = "classColor";
	private static final String NAME = "name";
	private static final String HREF_LINK = "link";
	private static final String HTML_START = "<html><body><table style=\"height: 72px; width: 418px;\" border=\"1\"><tbody><tr style=\"height: 28px;\"><th style=\"background-color: headerColor;\">Package Or Class Name</th><th style=\"background-color: headerColor;\">Line Percentage</th></tr>";
	private static final String HTML_END = "</tbody></table></body></html>";
	private static final String PACKAGE_ROW = "<tr style=\"height: 28px;\"><th style=\"background-color: packageColor;\"><a href = \"link\">name</a></th><th style=\"background-color: packageColor;\">percentage%</th></tr>";
	private static final String CLASS_ROW = "<tr style=\"height: 18.8px;\"><th style=\"background-color: classColor;text-align: left;\"><a href = \"link\">name</a></th><th style=\"background-color: classColor;\">percentage%</th></tr>";
	private static final String EMPTY_ROW = "<tr style=\"height: 50.0px;\"><th style=\"background-color: #ffffff;\" colspan=\"2\">&nbsp;</th></tr>";
	
	private Path path;

	HTMLFileReportGenerator(Path path)
	{
		this.path = path;
	}

	@Override
	public Void generate(List<PackageEntryInfo> request)
	{
		saveAsHtmlFile(this.path, request.stream());
		return null;
	}

	private static void saveAsHtmlFile(Path path, Stream<PackageEntryInfo> packageStream)
	{
		final StringBuilder builder = new StringBuilder();
		builder.append(HTML_START.replaceAll(HEADER_COLOR, LIGHT_BLUE));
		packageStream.map(HTMLFileReportGenerator::formatPackageEntry).forEach(builder::append);
		builder.append(HTML_END);
		
		try
		{
			Files.write(path, builder.toString().getBytes(), CREATE, TRUNCATE_EXISTING);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	private static String formatPackageEntry(PackageEntryInfo p)
	{
		final String packageRow = applyColor(PACKAGE_ROW.replace(NAME, p.name).replace(HREF_LINK, p.link).replace(PERCENTAGE, String.valueOf(p.perCovered)), PACKAGE_COLOR, p.perCovered);
		final StringBuilder builder = new StringBuilder();
		p.missedClasses.forEach(cei -> builder.append(applyColor(CLASS_ROW.replace(NAME, cei.name).replace(HREF_LINK, cei.link).replace(PERCENTAGE, String.valueOf(cei.linePerCovered)), CLASS_COLOR, cei.linePerCovered)));
		return packageRow + builder.toString() + EMPTY_ROW;
	}

	private static String applyColor(String content, String key, int perCovered) 
	{
		return content.replaceAll(key, perCovered >= 80 ? LIGHT_GREEN : perCovered >= 60 ? LIGHT_YELLOW : LIGHT_RED);
	}

}
