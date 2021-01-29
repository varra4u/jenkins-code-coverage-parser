package com.varra.jenkins.tools.codecoverage;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;

/**
 * @author Rajakrishna Reddy
 *
 */
class ConcurrentResultParser
{
	
	private String url = "";

	List<PackageEntryInfo> getParsedPackageDetails(URL url) throws IOException
	{
		this.url = url.toString();
		disableSSLVerification();
		final Document doc = getDocument(url.toString());
		try
		{
			final List<Node> tables = doc.getElementById("page-body").getElementById("main-panel").childNodes().stream().filter(n -> "table".equalsIgnoreCase(n.nodeName())).collect(toList());
			if (isMissingClassesAvailable(tables.get(0)))
			{
				final List<PackageEntryInfo> list = processPackagesTable(tables.get(1));
				list.sort(null);
				return list;
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			System.exit(0);
		}
		return Collections.emptyList();
	}
	
	/*protected void processTables(List<Node> tables) throws IOException
	{
		if (isMissingClassesAvailable(tables.get(0)))
		{
			final StopWatch watch = new StopWatch();
			final Path path = Paths.get("./code-coverage-report.html");
			final List<PackageEntryInfo> list = processPackagesTable(tables.get(1));
			list.sort(null);
			System.out.println(watch);
			HTMLFileReportGenerator.saveAsHtmlFile(path, list.stream());
			System.out.println("Report has been created at: "+path);
			System.out.println(watch);
		}
		else
		{
			System.out.println("Oops, no missing classes found, all the classes are covered!");
		}
	}*/

	private static boolean isMissingClassesAvailable(Node summaryTable)
	{
		if (!summaryTable.childNodes().isEmpty())
		{
			final Node tBody = summaryTable.childNodes().get(0);
			final List<Node> lastRowChildren = tBody.childNodes().get(1).childNodes();
			final Node tableInLastColumn = lastRowChildren.get(lastRowChildren.size()-2).childNodes().get(1);
			final Node classInfo = tableInLastColumn.childNodes().get(0).childNodes().get(1);
			final List<Node> mAndCInfo = classInfo.childNodes().get(0).childNodes().get(0).childNodes();
			final int mValue = Integer.parseInt(mAndCInfo.get(1).toString().trim()); 
			final int cValue = Integer.parseInt(mAndCInfo.get(3).toString().trim()); 
			System.out.println("Covered classes are: "+cValue+", missed classes are: "+mValue);
			return mValue != 0;
		}
		return false;
	}
	
	private List<PackageEntryInfo> processPackagesTable(Node packagesTable)
	{
		if (!packagesTable.childNodes().isEmpty())
		{
			final List<Node> tBodyChildren = packagesTable.childNode(0).childNodes();
			/*
			 * For missed classes alone
			 */
			//return tBodyChildren.subList(1, tBodyChildren.size()).parallelStream().map(ConcurrentResultParser::getRowInfo).filter(ri -> ri.mValue > 0).map(ConcurrentResultParser::getPackageInfo).collect(Collectors.toList());
			return tBodyChildren.subList(1, tBodyChildren.size()).parallelStream().map(ConcurrentResultParser::getRowInfo).map(this::getPackageInfo).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}
	
	private PackageEntryInfo getPackageInfo(RowInfo rInfo) 
	{
		final String packageUrl = url+"/"+rInfo.name;
		try {
			final Document doc = getDocument(packageUrl);
			final List<Node> tables = doc.getElementById("page-body").getElementById("main-panel").childNodes().stream().filter(n -> "table".equalsIgnoreCase(n.nodeName())).collect(toList());
			final int perCovered = getPercentageCovered(tables.get(0));
			final List<ClassEntryInfo> missedClasses = getMissedClasses(packageUrl, tables.get(1));
			missedClasses.sort(null);
			return new PackageEntryInfo(rInfo.name, packageUrl, perCovered, missedClasses);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new PackageEntryInfo(rInfo.name, packageUrl, 100, Collections.emptyList());
	}

	private static Document getDocument(String url) throws IOException {
		return Jsoup.parse(getHtml(url));
	}

	private static String getHtml(String location) throws IOException {
		return IOUtils.toString(new URL(location), Charset.defaultCharset());
	}
	
	private static List<ClassEntryInfo> getMissedClasses(final String packageUrl, Node classesTable) {
		final List<Node> tBodyChildren = classesTable.childNodes().get(0).childNodes();
		return tBodyChildren.subList(1, tBodyChildren.size()).parallelStream().map(n -> getClassEntryInfo(packageUrl, n)).filter(cei -> cei.linePerCovered <= 100).collect(toList());
	}

	private static int getPercentageCovered(Node summaryTable) {
		final Node tBody = summaryTable.childNodes().get(0);
		final List<Node> lastRowChildren = tBody.childNodes().get(1).childNodes();
		final Node tableInLastColumn = lastRowChildren.get(lastRowChildren.size()-2).childNodes().get(1);
		final String pCovered = tableInLastColumn.childNodes().get(0).childNodes().get(1).childNodes().get(0).childNodes().get(0).toString();
		return Integer.parseInt(pCovered.substring(0, pCovered.length()-1));
	}

	private static RowInfo getRowInfo(Node row) {
		final List<Node> rowChildren = row.childNodes();
		final String name = rowChildren.get(0).childNodes().get(0).childNodes().get(0).toString();
		final Node tableInLastColumn = rowChildren.get(rowChildren.size()-2).childNodes().get(1);
		final Node classInfo = tableInLastColumn.childNodes().get(0).childNodes().get(0);
		final List<Node> mAndCInfo = classInfo.childNodes().get(0).childNodes().get(0).childNodes();
		final int mValue = Integer.parseInt(mAndCInfo.get(1).toString().trim()); 
		final int cValue = Integer.parseInt(mAndCInfo.get(3).toString().trim()); 
		return new RowInfo(name, mValue, cValue);
	}
	
	private static ClassEntryInfo getClassEntryInfo(String packageUrl, Node row) 
	{
		final List<Node> rowChildren = row.childNodes();
		final String name = rowChildren.get(0).childNodes().get(0).childNodes().get(0).toString();
		final String classUrl = packageUrl + "/" + String.valueOf(rowChildren.get(0).childNodes().get(0).attr("href"));
		final int classPerCovered = getPercentageAsInt(rowChildren.get(rowChildren.size()-2).childNode(1).childNode(0).childNode(1).childNode(0).childNode(0).toString());
		final int methodPerCovered = getPercentageAsInt(rowChildren.get(rowChildren.size()-4).childNode(1).childNode(0).childNode(1).childNode(0).childNode(0).toString());
		final int linePerCovered = getPercentageAsInt(rowChildren.get(rowChildren.size()-6).childNode(1).childNode(0).childNode(1).childNode(0).childNode(0).toString());
		final int complexityPerCovered = getPercentageAsInt(rowChildren.get(rowChildren.size()-8).childNode(1).childNode(0).childNode(1).childNode(0).childNode(0).toString());
		final int branchPerCovered = getPercentageAsInt(rowChildren.get(rowChildren.size()-10).childNode(1).childNode(0).childNode(1).childNode(0).childNode(0).toString());
		final int instructionsPerCovered = getPercentageAsInt(rowChildren.get(rowChildren.size()-12).childNode(1).childNode(0).childNode(1).childNode(0).childNode(0).toString());
		return new ClassEntryInfo(name, classUrl, instructionsPerCovered, branchPerCovered, complexityPerCovered, linePerCovered, methodPerCovered, classPerCovered);
	}
	
	/**
	 * Converts the String percentage with as integer percentage number. i.e "60%" --> 60
	 * 
	 * @param perWithSymbol Percentage with the symbol
	 * @return percentage
	 */
	private static int getPercentageAsInt(String perWithSymbol)
	{
		return Integer.parseInt(perWithSymbol.substring(0, perWithSymbol.length()-1));
	}
	
	@SuppressWarnings("unused")
	private static class RowInfo
	{
		RowInfo(String name, int mValue, int cValue) {
			this.name = name;
			this.mValue = mValue;
			this.cValue = cValue;
		}
		String name;
		int mValue;
		int cValue;
	}
	
	public static class PackageEntryInfo implements Comparable<PackageEntryInfo>
	{
		String name;
		String link;
		int perCovered;
		final List<ClassEntryInfo> missedClasses;
		
		PackageEntryInfo(String name, String link, int perCovered, List<ClassEntryInfo> missedClasses) {
			this.name = name;
			this.link = link;
			this.perCovered = perCovered;
			this.missedClasses = missedClasses;
		}
		
		@Override
		public String toString() {
			return "PackageEntryInfo [name=" +
					name +
					", perCovered=" +
					perCovered +
					", missedClasses=" +
					missedClasses +
					"]";
		}

		public String getName() {
			return name;
		}

		@Override
		public int compareTo(PackageEntryInfo o) {
			return perCovered - o.perCovered;
		}
	}
	
	public static class ClassEntryInfo implements Comparable<ClassEntryInfo>
	{
		final String name;
		final String link;
		final int instructionsPerCovered;
		final int branchPerCovered;
		final int complexityPerCovered;
		final int linePerCovered;
		final int methodPerCovered;
		final int classPerCovered;
		
		
		ClassEntryInfo(String name, String link, int instructionsPerCovered,
					   int branchPerCovered, int complexityPerCovered, int linePerCovered, int methodPerCovered, int classPerCovered)
		{
			this.name = name;
			this.link = link;
			this.instructionsPerCovered = instructionsPerCovered;
			this.complexityPerCovered = complexityPerCovered;
			this.branchPerCovered = branchPerCovered;
			this.linePerCovered = linePerCovered;
			this.methodPerCovered = methodPerCovered;
			this.classPerCovered = classPerCovered;
		}
		
		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ClassEntryInfo other = (ClassEntryInfo) obj;
			if (name == null) {
				return other.name == null;
			} else return name.equals(other.name);
		}

		@Override
		public int compareTo(ClassEntryInfo o) {
			return linePerCovered - o.linePerCovered;
		}
	}
	
	private static void disableSSLVerification() {
        try {
        	// Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };
     
            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            
            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = (hostname, session) -> true;

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        }
        catch (Exception e) {
        	e.printStackTrace();
		}
	}
}
