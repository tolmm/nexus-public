/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.logging.task;

import java.io.File;

import javax.annotation.Nullable;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.sift.SiftingAppender;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

import static org.slf4j.Logger.ROOT_LOGGER_NAME;

/**
 * @since 3.5
 */
public class TaskLogHome
{
  private static final Logger log = LoggerFactory.getLogger(TaskLogHome.class);

  private TaskLogHome() { throw new IllegalAccessError("Utility class"); }

  /**
   * Get the home (absolute path) of the task logs on disk
   *
   * @return the task log home, null if it couldn't be found (usually due to missing appender in logback.xml)
   */
  @Nullable
  public static String getTaskLogHome() {
    LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();

    Appender appender = loggerContext.getLogger(ROOT_LOGGER_NAME).getAppender("tasklogfile");
    if (!(appender instanceof SiftingAppender)) {
      // We are forgiving if the task log appender does not exist. It could be that a user had a customized logback.xml
      // as of 3.4.1 when task logging was introduced. We don't want to block application start in this scenario.
      log.error("Could not find a Logback SiftingAppender named 'tasklogfile' in the logback configuration. " +
         "Please check that the 'tasklogfile' appender exists in logback.xml");
      return null;
    }
    SiftingAppender siftingAppender = (SiftingAppender) appender;

    // this will create a new appender which ultimately creates a temp.log within the tasks log folder
    FileAppender tempFileAppender = (FileAppender) siftingAppender.getAppenderTracker().getOrCreate("temp", 0L);

    // Note that at full execution speed the temp.log may not actually exist yet, but we don't actually need it to
    File file = new File(tempFileAppender.getFile());

    String taskLogsFolder = file.getParent();

    // no need to keep the temp.log file around
    tempFileAppender.stop(); // stop the appender to ensure the file lock is released
    FileUtils.deleteQuietly(file);

    return taskLogsFolder;
  }
}
