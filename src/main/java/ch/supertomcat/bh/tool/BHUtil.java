package ch.supertomcat.bh.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.supertomcat.bh.settings.SettingsManager;
import ch.supertomcat.supertomcattools.fileiotools.FileTool;

/**
 * This class provides methods, which are often used.
 */
public class BHUtil {
	/**
	 * Logger for this class
	 */
	private static Logger logger = LoggerFactory.getLogger(BHUtil.class);

	/**
	 * This method filters not allowed chars in filenames
	 * 
	 * @param filename Filename
	 * @return Filtered Filename
	 */
	public static String filterFilename(String filename) {
		return FileTool.filterFilename(filename, SettingsManager.instance().getAllowedFilenameChars());
	}

	/**
	 * This method filters not allowed chars in paths (including filename if available)
	 * 
	 * @param path Path
	 * @return Filtered path
	 */
	public static String filterPath(String path) {
		return FileTool.filterPath(path, SettingsManager.instance().getAllowedFilenameChars());
	}

	/**
	 * Read patterns defined by the user from a textfile
	 * 
	 * @param file Text File
	 * @param encoding Encoding
	 * @param caseInsensitive True if patterns should be compiled with CASE_INSENSITIVE flag, false otherwise
	 * @return Patterns
	 */
	public static List<Pattern> readPatternsFromTextFile(File file, Charset encoding, boolean caseInsensitive) {
		List<Pattern> patterns = new ArrayList<>();
		if (!file.exists()) {
			return patterns;
		}

		try (FileInputStream in = new FileInputStream(file); BufferedReader br = new BufferedReader(new InputStreamReader(in, encoding))) {
			String row = null;
			while ((row = br.readLine()) != null) {
				if (row.isEmpty()) {
					continue;
				}

				try {
					Pattern compiledPattern;
					if (caseInsensitive) {
						compiledPattern = Pattern.compile(row, Pattern.CASE_INSENSITIVE);
					} else {
						compiledPattern = Pattern.compile(row);
					}
					patterns.add(compiledPattern);
				} catch (PatternSyntaxException pse) {
					logger.error("Could not compile pattern: {}", row, pse);
				}
			}
		} catch (IOException e) {
			logger.error("Could not load patterns from: {}", file.getAbsolutePath(), e);
		}
		return patterns;
	}

	/**
	 * Changes the root logger level
	 * 
	 * @param level Level
	 */
	public static void changeLog4JRootLoggerLevel(Level level) {
		@SuppressWarnings("resource")
		LoggerContext loggerContext = (LoggerContext)LogManager.getContext(false);
		Configuration config = loggerContext.getConfiguration();
		LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
		loggerConfig.setLevel(level);
		loggerContext.updateLoggers(config);
	}

	/**
	 * Deletes old backup files
	 * 
	 * @param folder Folder
	 * @param filename Filename
	 * @param daysToKeepBackup Days to keep backups
	 */
	public static void deleteOldBackupFiles(File folder, final String filename, int daysToKeepBackup) {
		final long now = System.currentTimeMillis();
		final Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -daysToKeepBackup);
		final Date backupDeleteDate = cal.getTime();
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd--HH-mm-ss-SSS");
		final long backupDeleteTime = daysToKeepBackup * 24 * 60 * 60 * 1000;

		// Delete old backup-Files
		File backupFiles[] = folder.listFiles(new FileFilter() {
			private final Pattern oldBackupPattern = Pattern.compile("^" + filename + ".bak-([0-9]+)$");
			private final Pattern backupPattern = Pattern.compile("^" + filename + "-([0-9]{4}-[0-9]{2}-[0-9]{2}--[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{3})$");

			@Override
			public boolean accept(File pathname) {
				Matcher matcher = backupPattern.matcher(pathname.getName());
				if (matcher.matches()) {
					try {
						Date backupDate = dateFormat.parse(matcher.group(1));
						if (backupDate.before(backupDeleteDate)) {
							return true;
						}
					} catch (ParseException e) {
						logger.error("Could not parse datetime of backup file: {}", pathname.getAbsolutePath(), e);
					}
					return false;
				}

				matcher = oldBackupPattern.matcher(pathname.getName());
				if (matcher.matches()) {
					try {
						long backupTime = Long.parseLong(matcher.group(1));
						if ((now - backupTime) > backupDeleteTime) {
							return true;
						}
					} catch (NumberFormatException nfe) {
						logger.error("Could not parse datetime of backup file: {}", pathname.getAbsolutePath(), nfe);
					}
					return false;
				}
				return false;
			}
		});
		if (backupFiles != null) {
			for (File oldBackupFile : backupFiles) {
				if (!oldBackupFile.delete()) {
					logger.error("Could not delete old backup file: {}", oldBackupFile.getAbsolutePath());
				}
			}
		}
	}
}
