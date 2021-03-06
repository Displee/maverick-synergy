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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.callback.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.sshtools.common.logger.Log;
import com.sshtools.common.nio.ConnectRequestFuture;
import com.sshtools.common.nio.DisconnectRequestFuture;
import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.TransportProtocol;

/**
 * Implements a reverse SSH server. Making a client socket connection out to the CallbackServer which is listening
 * on the SSH port to act as a client to any incoming connections. The connection is authenticated by a public
 * key held by the CallbackClient.
 */
public class CallbackSession implements Runnable {

	public static final String CALLBACK_IDENTIFIER = "CallbackClient-";

	CallbackConfiguration config;
	CallbackClient app;
	ConnectRequestFuture future;
	Connection<?> currentConnection;
	boolean isStopped = false;
	String hostname;
	int port;
	boolean onDemand = false;
	Map<String,Object> attributes = new HashMap<String,Object>();
	int numberOfAuthenticationErrors = 0;
	
	public CallbackSession(CallbackConfiguration config, CallbackClient app, String hostname, int port, boolean onDemand) throws IOException {
		this.config = config;
		this.app = app;
		this.onDemand = onDemand;
		this.hostname = hostname;
		this.port = port;

	}
	
	public void run() {
		try {
			connect();
		} catch (IOException e) {
			Log.error("Failed to startup", e);
		}
	}

	public void connect() throws IOException {
		
		if(isStopped) {
			throw new IOException("Client has been stopped");
		}
		
		if(Log.isInfoEnabled()) {
			Log.info(String.format("Connecting to %s:%d", hostname, port));
		}
		
		synchronized(app) {
			if(!app.getSshEngine().isStarted() && !app.getSshEngine().isStarting()) {
				if(!app.getSshEngine().startup()) {
					throw new IOException("SSH Engine failed to start");
				}
			}
		}
		int count = 1;
		while(app.getSshEngine().isStarted()) {
			try {
				future = app.getSshEngine().connect(
						hostname, 
						port, 
						app.createContext(app.getSshEngine().getContext(), config));;
				future.waitFor(30000L);
				if(future.isDone() && future.isSuccess()) {
					currentConnection = future.getConnection();
					currentConnection.getAuthenticatedFuture().waitFor(30000L);
					if(currentConnection.getAuthenticatedFuture().isDone() && currentConnection.getAuthenticatedFuture().isSuccess()) {
						currentConnection.setProperty("callbackClient", this);
						app.onClientConnected(this);
						if(Log.isInfoEnabled()) {
							Log.info(String.format("Client is connected to %s:%d", hostname, port));
						}
						numberOfAuthenticationErrors = 0;
						break;
					} else {
						if(Log.isInfoEnabled()) {
							Log.info(String.format("Could not authenticate to %s:%d", hostname, port));
						}
						currentConnection.disconnect();
						numberOfAuthenticationErrors++;
					}
				}
				try {
					long interval = config.getReconnectIntervalMs();
					if(numberOfAuthenticationErrors >= 3) {
						interval = TimeUnit.MINUTES.toMillis(10);
					}
					if(numberOfAuthenticationErrors >= 9) {
						interval = TimeUnit.MINUTES.toMillis(60);
					}
					if(Log.isInfoEnabled()) {
						Log.info(String.format("Will reconnect to %s:%d in %d seconds", hostname, port, interval / 1000));
					}
					Thread.sleep(interval);
				} catch (InterruptedException e) {
				}
			} catch(Throwable e) {
				Log.error(String.format("%s on %s:%d", 
						e.getMessage(),
						config.getServerHost(), 
						config.getServerPort()), e);
				long interval = config.getReconnectIntervalMs() * Math.min(count, 12 * 60);
				if(Log.isInfoEnabled()) {
					Log.info(String.format("Reconnecting to %s:%d in %d seconds", hostname, port, interval / 1000));
				}
				try {
					Thread.sleep(interval);
				} catch (InterruptedException e1) {
				}
				if(count >= 12) {
					count += 12;
				} else {
					count++;
				}
			}
		}
	}
		
	public void disconnect() {
		if(future.isDone() && future.isSuccess()) {
			future.getTransport().disconnect(TransportProtocol.BY_APPLICATION, "The user disconnected.");
		}
		currentConnection = null;
	}
	
	public DisconnectRequestFuture stop() {
		isStopped = true;
		disconnect();
		return future.getTransport().getDisconnectFuture();
	}

	public String getName() {
		return config.getAgentName() + "@" + config.getServerHost();
	}

	public CallbackConfiguration getConfig() {
		return config;
	}

	public boolean isStopped() {
		return isStopped;
	}

	public void setConfig(CallbackConfiguration config) {
		this.config = config;
	}
	
//	private void brokerConnection(String hostname, int port) throws IOException {
//		app.start(app.createClient(config, hostname, port, true));
//	}
	
	public boolean hasAttribute(String key) {
		return attributes.containsKey(key);
	}
	
	public Object getAttribute(String key) {
		return attributes.get(key);
	}
	
	public void setAttribute(String key, Object val) {
		attributes.put(key, val);
	}
}
