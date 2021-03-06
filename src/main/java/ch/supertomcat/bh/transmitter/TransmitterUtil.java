package ch.supertomcat.bh.transmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.supertomcat.bh.gui.adder.AdderPanel;
import ch.supertomcat.bh.importexport.Import;
import ch.supertomcat.bh.importexport.ImportLinkList;
import ch.supertomcat.bh.pic.URL;
import ch.supertomcat.bh.pic.URLList;

/**
 * Utility class for Transmitter
 */
public final class TransmitterUtil {
	/**
	 * Logger for this class
	 */
	private static Logger logger = LoggerFactory.getLogger(TransmitterUtil.class);

	/**
	 * Parse Transmitter Input and open Download-Selection-Dialog
	 * 
	 * @param in InputStream
	 * @param encoding Encoding
	 * @return True if successful, false otherwise
	 */
	public static boolean parseTransmitterInput(InputStream in, Charset encoding) {
		/*
		 * There are 3 ways, how URLs could be transferred to BH.
		 * The first one is to send all the URLs by the stream (The Firefox-Extension does this)
		 * The second one is that the plugin/extension writes the URLs to a file and send
		 * the path to file by the stream (The IE-Plugin does this)
		 * The third one is that the plugin/extension sends only the URL which contains the URLs
		 * by the stream (The Opera-Plugin does this)
		 */
		try {
			logger.debug("Opening InputStream");
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, encoding));

			String file = "";
			String url = "";
			String title = "";
			String referrer = "";
			List<URL> urls = new ArrayList<>();
			boolean fullList = false;
			boolean withThumbs = false;
			boolean eof = false;
			boolean imgs = false;

			String line = null;
			logger.debug("Reading InputStream");

			/*
			 * Read first line
			 * Must describe which information is provided:
			 * FULLLISTTHUMBS (Full Link List with Thumbs, Link and Thumb seperated by tab)
			 * FULLLIST (Full Link list without Thumbs)
			 * SOF (Only one Link to a page containing the Links which BH will read out)
			 */
			line = reader.readLine();
			logger.debug("Line read: {}", line);
			if (line.equals("FULLLISTTHUMBS")) {
				fullList = true;
				withThumbs = true;
				logger.debug("Recieving Full List with Thumbs");
			} else if (line.equals("FULLLIST")) {
				fullList = true;
				logger.debug("Recieving Full List");
			} else if (line.equals("SOF")) {
				logger.debug("SOF");
			} else {
				logger.error("First line did not match expected values: {}", line);
				return false;
			}

			/*
			 * Read next line in fullList Mode
			 * Line must be SOF
			 */
			if (fullList) {
				line = reader.readLine();
				logger.debug("Line read: {}", line);
				if (line.equals("SOF")) {
					logger.debug("SOF");
				} else {
					logger.error("Line should have been SOF, but was: {}", line);
					return false;
				}
			}

			/*
			 * Read rest of the lines
			 */
			if (fullList) {
				/*
				 * Read next line
				 * Must be BH{af2f0750-c598-4826-8e5f-bb98aab519a5}
				 */
				line = reader.readLine();
				logger.debug("Line read: {}", line);
				if (!line.startsWith("BH{af2f0750-c598-4826-8e5f-bb98aab519a5}")) {
					logger.error("Line did not match BH serial number: {}", line);
					return false;
				}

				/*
				 * Read title
				 */
				line = reader.readLine();
				logger.debug("Line read: {}", line);
				title = line;

				/*
				 * Read referrer url
				 */
				line = reader.readLine();
				logger.debug("Line read: {}", line);
				referrer = line;

				while ((line = reader.readLine()) != null) {
					logger.debug("Line read: {}", line);

					// Break the loop when recieving the EOF
					if (line.equals("EOF")) {
						eof = true;
						logger.debug("EOF");
						break;
					}

					URL urlToAdd = null;
					// If we recieving a full list
					if (withThumbs) {
						// If we recieving also thumbs
						int last = line.length();
						int seperator = line.indexOf("\t");
						if ((seperator > 0) && (seperator < last)) {
							urlToAdd = new URL(line.substring(0, seperator), line.substring(seperator + 1, last));
						} else {
							urlToAdd = new URL(line, "");
						}
					} else {
						urlToAdd = new URL(line, "");
					}
					urlToAdd.setThreadURL(referrer);
					urls.add(urlToAdd);
				}
			} else {
				if ((line = reader.readLine()) != null) {
					logger.debug("Line read: {}", line);
					if (line.startsWith("URL:")) {
						url = line.substring(line.indexOf(":") + 1);
						file = "";
					} else if (line.startsWith("IMG:")) {
						url = line.substring(line.indexOf(":") + 1);
						file = "";
						imgs = true;
					} else {
						file = line;
						url = "";
					}
					if ((line = reader.readLine()) != null) {
						logger.debug("Line read: {}", line);
						if (line.equals("EOF")) {
							logger.debug("EOF");
							eof = true;
						} else {
							logger.error("Line should have been EOF, but was: {}", line);
							return false;
						}
					}
				}
			}

			if ((fullList == false) && (file.length() > 0) && (eof)) {
				// If we recieved only a path to a file, we must read it
				ImportLinkList.importLinkList(file, true);
				logger.info("Handled Connection successfully");
			} else if ((fullList == false) && (url.length() > 0) && (eof)) {
				// If we recieved only a URL which contains all the URLs, we download the URL
				if (url.matches("^https?://.*/?.*")) {
					logger.debug("Recieved URL to Download: {}", url);
					// ImportHTML.importHTML(url, url, imgs);
					Import.importURL(url, url, imgs);
				} else {
					logger.error("URL did not match URL-Pattern: {}", url);
				}
				logger.info("Handled Connection successfully");
			} else if ((fullList) && (urls.size() > 0) && (eof)) {
				// If we recieved all the URLs
				logger.debug("Recieved {} Links", urls.size());
				// Open the Download-Selection-Dialog
				AdderPanel adderpnl = new AdderPanel(new URLList(title, referrer, urls));
				adderpnl.init();
				logger.info("Handled Connection successfully");
			}
			return true;
		} catch (IOException e) {
			logger.error("Could not handle connection", e);
			return false;
		}
	}
}
