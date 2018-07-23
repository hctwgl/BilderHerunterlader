package ch.supertomcat.bh.clipboard;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.supertomcat.bh.importexport.Import;
import ch.supertomcat.bh.settings.SettingsManager;

/**
 * Class for observing the clipboard
 */
public class ClipboardObserver implements ClipboardOwner {
	private static final Pattern NEW_LINE_PATTERN = Pattern.compile("\r?\n|\r");

	private static final Pattern URL_PATTERN = Pattern.compile("^https?://.+");

	/**
	 * Singleton
	 */
	private static ClipboardObserver instance = null;

	/**
	 * Logger for this class
	 */
	private Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * The toolkit to work with the clipboard
	 */
	private Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

	private Thread clipboardObserverThread = null;

	private ClipboardObserverRunnable clipboardObserverRunnable = null;

	/**
	 * Constructor
	 */
	private ClipboardObserver() {
		if (SettingsManager.instance().isCheckClipboard()) {
			init();
		}
	}

	/**
	 * Returns the singleton
	 * 
	 * @return Singleton
	 */
	public static synchronized ClipboardObserver instance() {
		if (instance == null) {
			instance = new ClipboardObserver();
		}
		return instance;
	}

	/**
	 * Activate clipboard monitoring
	 */
	public synchronized void init() {
		if (clipboardObserverThread != null && clipboardObserverThread.isAlive()) {
			return;
		}
		clipboardObserverRunnable = new ClipboardObserverRunnable();
		clipboardObserverThread = new Thread(clipboardObserverRunnable);
		clipboardObserverThread.setName("ClipboardObserverThread-" + clipboardObserverThread.getId());
		clipboardObserverThread.setDaemon(true);
		clipboardObserverThread.start();
	}

	/**
	 * Stop clipboard monitoring
	 */
	public synchronized void stop() {
		if (clipboardObserverRunnable != null) {
			clipboardObserverRunnable.stop();
			clipboardObserverRunnable = null;
		}
		clipboardObserverThread = null;
	}

	/**
	 * Set content to clipboard
	 * 
	 * @param content Content
	 */
	public synchronized void setClipboardContent(String content) {
		boolean observer = clipboardObserverRunnable != null;
		try {
			if (observer) {
				clipboardObserverRunnable.setOwnContent(true);
			}
			clipboard.setContents(new StringSelection(content), this);
		} catch (Exception e) {
			logger.error("Could not set clipboard content", e);
			if (observer) {
				clipboardObserverRunnable.setOwnContent(false);
			}
		}
	}

	@Override
	public synchronized void lostOwnership(Clipboard clipboard, Transferable content) {
		/*
		 * Note: Content is the content this class placed into the clipboard
		 */
		if (clipboardObserverRunnable != null) {
			clipboardObserverRunnable.setOwnContent(false);
		}
	}

	private class ClipboardObserverRunnable implements Runnable {
		private String previousContent = null;

		private volatile boolean stop = false;

		private volatile boolean ownContent = false;

		@Override
		public void run() {
			stop = false;
			while (!stop) {
				synchronized (this) {
					try {
						this.wait(750);
					} catch (InterruptedException e) {
						// Nothing to do
					}
				}
				if (stop) {
					return;
				}
				if (ownContent) {
					continue;
				}
				if (SettingsManager.instance().isCheckClipboard()) {
					checkClipboard();
				}
			}
		}

		private synchronized void checkClipboard() {
			try {
				// Get the data from the clipboard
				Transferable content = clipboard.getContents(this);
				if (content == null) {
					return;
				}

				// Check if data is text
				if (content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
					try {
						String data = (String)content.getTransferData(DataFlavor.stringFlavor);
						if (hasContentChanged(previousContent, data)) {
							try {
								List<String> links = getLinksFromContent(data);
								for (String link : links) {
									// let the import class download the container page and get the links from it
									Import.importURL(link, link, false);
								}
							} finally {
								previousContent = data;
							}
						}
					} catch (UnsupportedFlavorException | IOException e) {
						logger.error("Could not get clipboard content", e);
					}
				}
			} catch (Exception e) {
				logger.error("Could not get clipboard content", e);
			}
		}

		private boolean hasContentChanged(String previousContent, String currentContent) {
			if (previousContent != null && currentContent != null) {
				return !previousContent.equals(currentContent);
			} else if ((previousContent == null && currentContent != null) || (previousContent != null && currentContent == null)) {
				return true;
			} else {
				return false;
			}
		}

		private List<String> getLinksFromContent(String data) {
			List<String> links = new ArrayList<>();
			if (data == null) {
				return links;
			}

			String[] lines = NEW_LINE_PATTERN.split(data);
			for (String line : lines) {
				if (URL_PATTERN.matcher(line).matches()) {
					links.add(line);
				}
			}
			return links;
		}

		/**
		 * Sets the ownContent
		 * 
		 * @param ownContent ownContent
		 */
		public void setOwnContent(boolean ownContent) {
			this.ownContent = ownContent;
		}

		/**
		 * Stop
		 */
		public void stop() {
			stop = true;
			synchronized (this) {
				this.notifyAll();
			}
		}
	}
}