package ch.supertomcat.bh.rules;

import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.supertomcat.bh.exceptions.HostAbortedException;
import ch.supertomcat.bh.exceptions.HostCompletedException;
import ch.supertomcat.bh.exceptions.HostException;
import ch.supertomcat.bh.exceptions.HostFileNotExistException;
import ch.supertomcat.bh.exceptions.HostFileTemporaryOfflineException;
import ch.supertomcat.bh.pic.PicState;

/**
 * RulePipeline
 */
public class RulePipelineFailures extends RulePipeline {
	/**
	 * Logger
	 */
	private Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * failureType
	 */
	private PicState failureType = PicState.FAILED;

	private boolean checkURL = false;

	private boolean checkThumbURL = false;

	private boolean checkPageSourceCode = false;

	/**
	 * Constructor
	 * 
	 * @param mode Rule-Mode
	 */
	public RulePipelineFailures(RuleMode mode) {
		super(mode);
	}

	/**
	 * Constructor
	 * 
	 * @param e Element
	 */
	public RulePipelineFailures(Element e) {
		super(e);

		if (this.mode == RuleMode.RULE_MODE_FAILURES) {
			try {
				setFailureType(PicState.getByValue(Integer.parseInt(e.getAttributeValue("failureType"))));
			} catch (Exception exx) {
			}
			try {
				checkURL = Boolean.parseBoolean(e.getAttributeValue("checkURL"));
			} catch (Exception exx) {
			}
			try {
				checkThumbURL = Boolean.parseBoolean(e.getAttributeValue("checkThumbURL"));
			} catch (Exception exx) {
			}
			try {
				checkPageSourceCode = Boolean.parseBoolean(e.getAttributeValue("checkPageSourceCode"));
			} catch (Exception exx) {
			}
		}
	}

	@Override
	public Element getXmlElement() {
		Element e = super.getXmlElement();
		if (this.mode == RuleMode.RULE_MODE_FAILURES) {
			e.setAttribute("checkURL", String.valueOf(this.checkURL));
			e.setAttribute("checkThumbURL", String.valueOf(this.checkThumbURL));
			e.setAttribute("checkPageSourceCode", String.valueOf(this.checkPageSourceCode));
			e.setAttribute("failureType", String.valueOf(this.failureType.getValue()));
		}
		return e;
	}

	@Override
	public boolean isCheckURL() {
		return checkURL;
	}

	@Override
	public void setCheckURL(boolean checkURL) {
		this.checkURL = checkURL;
	}

	@Override
	public boolean isCheckThumbURL() {
		return checkThumbURL;
	}

	@Override
	public void setCheckThumbURL(boolean checkThumbURL) {
		this.checkThumbURL = checkThumbURL;
	}

	@Override
	public boolean isCheckPageSourceCode() {
		return checkPageSourceCode;
	}

	@Override
	public void setCheckPageSourceCode(boolean checkPageSourceCode) {
		this.checkPageSourceCode = checkPageSourceCode;
	}

	@Override
	public PicState getFailureType() {
		return failureType;
	}

	@Override
	public void setFailureType(PicState failureType) {
		switch (failureType) {
			case COMPLETE:
			case SLEEPING:
			case FAILED_FILE_TEMPORARY_OFFLINE:
			case FAILED_FILE_NOT_EXIST:
			case FAILED:
				this.failureType = failureType;
				break;
			default:
				// Do nothing, other failure types are not supported
				break;
		}
	}

	private String check(String input) {
		String result = "";
		int start = 0;
		for (int i = 0; i < regexps.size(); i++) {
			if (i < (regexps.size() - 1)) {
				start = regexps.get(i).doFailureSearch(input, start);
				if (start > 0) {
					logger.debug("FailureCheck -> Search done -> Step " + i + " -> Pattern found at: " + start);
				} else {
					logger.debug("FailureCheck -> Search done -> Step " + i + " -> Pattern not found!");
				}
			} else {
				result = regexps.get(i).doFailureLastSearch(input, start);
				logger.debug("FailureCheck -> Failure found -> Step " + i + " -> Result: " + result);
			}
		}
		return result;
	}

	@Override
	public void checkForFailure(String url) throws HostException {
		String message = "";

		if (checkURL) {
			message = check(url);
		}

		if (!message.isEmpty()) {
			switch (failureType) {
				case COMPLETE:
					throw new HostCompletedException(message);
				case SLEEPING:
					throw new HostAbortedException(message);
				case FAILED_FILE_TEMPORARY_OFFLINE:
					throw new HostFileTemporaryOfflineException(message);
				case FAILED_FILE_NOT_EXIST:
					throw new HostFileNotExistException(message);
				case FAILED:
					throw new HostException(message);
				default:
					// Do nothing, other failure types are not supported
					break;
			}
		}
	}

	@Override
	public void checkForFailure(String url, String thumbURL, String htmlcode) throws HostException {
		String message = "";

		if (checkURL) {
			message = check(url);
		}

		if (checkThumbURL && message.isEmpty()) {
			message = check(thumbURL);
		}

		if (checkPageSourceCode && message.isEmpty()) {
			message = check(htmlcode);
		}

		if (!message.isEmpty()) {
			switch (failureType) {
				case COMPLETE:
					throw new HostCompletedException(message);
				case SLEEPING:
					throw new HostAbortedException(message);
				case FAILED_FILE_TEMPORARY_OFFLINE:
					throw new HostFileTemporaryOfflineException(message);
				case FAILED_FILE_NOT_EXIST:
					throw new HostFileNotExistException(message);
				case FAILED:
					throw new HostException(message);
				default:
					// Do nothing, other failure types are not supported
					break;
			}
		}
	}
}
