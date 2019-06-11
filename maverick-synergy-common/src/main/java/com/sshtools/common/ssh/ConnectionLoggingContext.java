/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.ssh;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import com.sshtools.common.logger.FileLoggingContext;
import com.sshtools.common.logger.Log;
import com.sshtools.common.logger.Log.Level;
import com.sshtools.common.logger.LoggerContext;

public class ConnectionLoggingContext implements LoggerContext {

	Level level;
	Map<SshConnection, FileLoggingContext> activeLoggers = new HashMap<>();
	ConnectionManager<?> cm;
	
	ConnectionLoggingContext(Level level, ConnectionManager<?> cm) {
		this.level = level;
		this.cm = cm;
	}
	
	@Override
	public boolean isLogging(Level level) {
		SshConnection currentConnection = cm.getCurrentConnection();
		if(activeLoggers.containsKey(currentConnection)) {
			return this.level != Level.NONE && this.level.ordinal() >= level.ordinal();
		} 
		return false;
	}

	@Override
	public void log(Level level, String msg, Throwable e, Object... args) {

		SshConnection currentConnection = cm.getCurrentConnection();
		if(!Objects.isNull(currentConnection)) {
			FileLoggingContext ctx = activeLoggers.get(currentConnection);
			if(!Objects.isNull(ctx)) {
				ctx.log(level, msg, e, args);
			}
		}
	}

	private boolean isLoggingRemoteAddress(SshConnection con) {
		return lookup(getPropertyKey(".remoteAddr"), con.getRemoteAddress().getHostAddress(), con);
	}
	
	private boolean isLoggingLocalAddress(SshConnection con) {
		return lookup(getPropertyKey(".localAddr"), con.getLocalAddress().getHostAddress(), con);
	}
	
	private boolean isLoggingRemotePort(SshConnection con) {
		return lookup(getPropertyKey(".remotePort"), String.valueOf(con.getRemotePort()), con);
	}
	
	private boolean isLoggingLocalPort(SshConnection con) {
		return lookup(getPropertyKey(".localPort"), String.valueOf(con.getLocalPort()), con);
	}
	
	private boolean lookup(String key, String value, SshConnection con) {
		Properties loggingProperties = Log.getDefaultContext().getLoggingProperties();
		String v = loggingProperties.getProperty(key, "");
		if("".equals(v)) {
			return true;
		}
		Set<String> addr = new HashSet<String>(Arrays.asList(v.split(",")));
		return addr.isEmpty() 
				|| addr.contains(con.getRemoteAddress().getHostAddress()) 
				|| addr.contains(con.getRemoteAddress().getHostAddress());
	}

	
	public void open(Connection<?> con) throws IOException {
		
		if(checkLogStatus(con)) {
			createLog(con);
		}
	}
	
	private void createLog(Connection<?> con) throws IOException {
		
		
		Properties loggingProperties = Log.getDefaultContext().getLoggingProperties();
		
		this.level = Level.valueOf(loggingProperties.getProperty("maverick.log.connection.defaultLevel", "NONE"));
		
		Level level = Level.valueOf(loggingProperties.getProperty(getPropertyKey(".level"), this.level.name()));
		String filenameFormat = loggingProperties.getProperty(getPropertyKey(".filenameFormat"), "${timestamp}__${uuid}.log");
		Integer maxFiles = Integer.parseInt(loggingProperties.getProperty(getPropertyKey(".maxFiles"), "10"));
		Long maxSize = Long.parseLong(loggingProperties.getProperty(getPropertyKey(".maxSize"), String.valueOf(1024 * 1024 * 20)));

		String filename = filenameFormat
				.replace("${timestamp}", LocalDateTime.now().format(
						DateTimeFormatter.ofPattern(
								loggingProperties.getProperty(
										getPropertyKey(".timestampPattern"), 
										"yyyy-MM-dd-HH-mm-ss-SSS"))))
				.replace("${uuid}", con.getUUID())
				.replace("${remotePort}", String.valueOf(con.getRemotePort()))
				.replace("${remoteAddr}", con.getRemoteAddress().getHostAddress())
				.replace("${localPort}", String.valueOf(con.getLocalPort()))
				.replace("${localAddr}", con.getLocalAddress().getHostAddress());
		
		activeLoggers.put(con, new FileLoggingContext(level, new File(filename), maxFiles, maxSize));
	}

	private boolean checkLogStatus(Connection<?> con) {
		
		Properties loggingProperties = Log.getDefaultContext().getLoggingProperties();
		
		/**
		 * Get maverick.log.connection.<name> property to determine if logging is enabled.
		 * Default to the default log level
		 */
		if(!"true".equalsIgnoreCase(loggingProperties.getProperty(getPropertyKey(""), 
				String.valueOf(!this.level.equals(Level.NONE))))) {
			return false;
		}
		
		return isLoggingRemoteAddress(con) && isLoggingRemotePort(con)
				&& isLoggingLocalAddress(con) && isLoggingLocalPort(con);
	}

	private String getPropertyKey(String key) {
		return String.format("maverick.log.connection.%s%s", cm.getName(), key);
	}
	
	public void close(Connection<?> con) {
		FileLoggingContext ctx = activeLoggers.remove(con);
		if(!Objects.isNull(ctx)) {
			ctx.close();
		}
	}

	@Override
	public void raw(Level level, String msg) {
		SshConnection currentConnection = cm.getCurrentConnection();
		if(!Objects.isNull(currentConnection)) {
			FileLoggingContext ctx = activeLoggers.get(currentConnection);
			if(!Objects.isNull(ctx)) {
				ctx.raw(level, msg);
			}
		}
	}

	@Override
	public void close() {
		/**
		 * We don't need to close anything because logs are connection specific. If this
		 * is the result of a change in the log file, the new settings will be applied
		 * to new connections after the changes are applied in the global context.
		 */
	}

	@Override
	public void newline() {
		SshConnection currentConnection = cm.getCurrentConnection();
		if(!Objects.isNull(currentConnection)) {
			FileLoggingContext ctx = activeLoggers.get(currentConnection);
			if(!Objects.isNull(ctx)) {
				ctx.newline();
			}
		}
	}
}