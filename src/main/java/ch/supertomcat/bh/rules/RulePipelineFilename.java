package ch.supertomcat.bh.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jdom2.Element;

import ch.supertomcat.bh.pic.Pic;


/**
 * RulePipeline
 */
public class RulePipelineFilename extends RulePipeline {
	/**
	 * Logger for this class
	 */
	private static Logger logger = LoggerFactory.getLogger(RulePipelineFilename.class);
	
	/**
	 * Defines which source for filename search and replace should be used (Only used when mode is 2)
	 */
	private int filenameMode = RULEPIPELINE_MODE_FILENAME_CONTAINER_URL_FILENAME_PART;
	
	/**
	 * Defines which source for filename for download selection search and replace should be used (Only used when mode is 3)
	 */
	private int filenameDownloadSelectionMode = RULEPIPELINE_MODE_FILENAME_CONTAINER_URL_FILENAME_PART;
	
	/**
	 * Constructor
	 * @param mode Rule-Mode
	 */
	public RulePipelineFilename(int mode) {
		super(mode);
	}
	
	/**
	 * Constructor
	 * @param e Element
	 */
	public RulePipelineFilename(Element e) {
		super(e);
		try {
			if (this.mode == Rule.RULE_MODE_FILENAME) {
				try {
					this.setFilenameMode(Integer.parseInt(e.getAttributeValue("filenamemode")));
				} catch (Exception exx) {
				}
			} else if (this.mode == Rule.RULE_MODE_FILENAME_ON_DOWNLOAD_SELECTION) {
				try {
					this.setFilenameDownloadSelectionMode(Integer.parseInt(e.getAttributeValue("filenameDownloadSelectionMode")));
				} catch (Exception exx) {
				}
			}
		} catch (Exception ex) {
		}
	}
	
	/**
	 * Returns the Element for creating the XML-File
	 * @return Element
	 */
	@Override
	public Element getXmlElement() {
		Element e = super.getXmlElement();
		if (this.mode == Rule.RULE_MODE_FILENAME) {
			e.setAttribute("filenamemode", String.valueOf(this.filenameMode));
		} else if (this.mode == Rule.RULE_MODE_FILENAME_ON_DOWNLOAD_SELECTION) {
			e.setAttribute("filenameDownloadSelectionMode", String.valueOf(this.filenameDownloadSelectionMode));
		}
		return e;
	}
	
	/**
	 * Returns the filenameMode
	 * @return filenameMode
	 */
	@Override
	public int getFilenameMode() {
		return filenameMode;
	}

	/**
	 * Sets the filenameMode
	 * @param filenameMode filenameMode
	 */
	@Override
	public void setFilenameMode(int filenameMode) {
		if ( (filenameMode == RULEPIPELINE_MODE_FILENAME_CONTAINER_URL_FILENAME_PART) || 
				(filenameMode == RULEPIPELINE_MODE_FILENAME_CONTAINER_URL) || 
				(filenameMode == RULEPIPELINE_MODE_FILENAME_THUMBNAIL_URL_FILENAME_PART) ||
				(filenameMode == RULEPIPELINE_MODE_FILENAME_THUMBNAIL_URL) || 
				(filenameMode == RULEPIPELINE_MODE_FILENAME_CONTAINER_PAGE_SOURCECODE) ||
				(filenameMode == RULEPIPELINE_MODE_FILENAME_DOWNLOAD_URL) ||
				(filenameMode == RULEPIPELINE_MODE_FILENAME_DOWNLOAD_URL_FILENAME_PART) ||
				(filenameMode == RULEPIPELINE_MODE_FILENAME_LAST_CONTAINER_URL) ||
				(filenameMode == RULEPIPELINE_MODE_FILENAME_LAST_CONTAINER_URL_FILENAME_PART) ||
				(filenameMode == RULEPIPELINE_MODE_FILENAME_LAST_CONTAINER_PAGE_SOURCECODE) ) {
			this.filenameMode = filenameMode;
		}
	}
	
	/**
	 * Returns the filenameDownloadSelectionMode
	 * @return filenameDownloadSelectionMode
	 */
	@Override
	public int getFilenameDownloadSelectionMode() {
		return filenameDownloadSelectionMode;
	}

	/**
	 * Sets the filenameDownloadSelectionMode
	 * @param filenameDownloadSelectionMode filenameDownloadSelectionMode
	 */
	@Override
	public void setFilenameDownloadSelectionMode(int filenameDownloadSelectionMode) {
		if ( (filenameDownloadSelectionMode == RULEPIPELINE_MODE_FILENAME_CONTAINER_URL_FILENAME_PART) || 
				(filenameDownloadSelectionMode == RULEPIPELINE_MODE_FILENAME_CONTAINER_URL) ) {
			this.filenameDownloadSelectionMode = filenameDownloadSelectionMode;
		}
	}
	
	/**
	 * Returns the filename after replacement
	 * @param url URL
	 * @param thumbURL Thumbnail-URL
	 * @param htmlcode Container-Page-Sourcecode
	 * @param pic Pic
	 * @return Filename
	 */
	@Override
	public String getCorrectedFilename(String url, String thumbURL, String htmlcode, Pic pic) {
		String retval = "";
		if (this.mode == Rule.RULE_MODE_FILENAME) {
			String result = url;
			boolean bSourcecode = false;
			if ( (this.filenameMode == RULEPIPELINE_MODE_FILENAME_THUMBNAIL_URL_FILENAME_PART) || (this.filenameMode == RULEPIPELINE_MODE_FILENAME_THUMBNAIL_URL) ) {
				result = thumbURL;
			} else if (this.filenameMode == RULEPIPELINE_MODE_FILENAME_CONTAINER_PAGE_SOURCECODE) {
				result = htmlcode;
				bSourcecode = true;
			}
			if (bSourcecode == false) {
				for (int i = 0; i < regexps.size(); i++) {
					result = regexps.get(i).doURLReplace(result, pic);
					logger.debug(url + " -> Filename Replace done -> Step " + i + " -> Result: " + result);
				}
			} else {
				int start = 0;
				for (int i = 0; i < regexps.size(); i++) {
					if (i < (regexps.size() - 1)) {
						start = regexps.get(i).doPageSourcecodeSearch(htmlcode, start);
						if (start > 0) {
							logger.debug(url + " -> Filename Search done -> Step " + i + " -> Pattern found at: " + start);
						} else {
							logger.debug(url + " -> Filename Search done -> Step " + i + " -> Pattern not found!");
						}
					} else {
						result = regexps.get(i).doPageSourcecodeReplace(htmlcode, start, url, pic);
						logger.debug(url + " -> Filename Replace done -> Step " + i + " -> Result: " + result);
					}
				}
			}
			retval = result;
		}
		return retval;
	}
	
	/**
	 * Returns the filename after replacement
	 * @param url URL
	 * @return Filename
	 */
	@Override
	public String getCorrectedFilenameOnDownloadSelection(String url) {
		String retval = "";
		if (this.mode == Rule.RULE_MODE_FILENAME_ON_DOWNLOAD_SELECTION) {
			String result = url;
			for (int i = 0; i < regexps.size(); i++) {
				result = regexps.get(i).doURLReplace(result, null);
				logger.debug(url + " -> Filename on Download Selection Replace done -> Step " + i + " -> Result: " + result);
			}
			retval = result;
		}
		return retval;
	}
}