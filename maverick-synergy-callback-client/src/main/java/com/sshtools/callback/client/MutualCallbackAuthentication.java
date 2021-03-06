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
import java.nio.ByteBuffer;
import java.util.Objects;

import com.sshtools.common.auth.AbstractAuthenticationProtocol;
import com.sshtools.common.auth.AuthenticationMechanism;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.Context;
import com.sshtools.common.ssh.ExecutorOperationSupport;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.sshd.AbstractServerTransport;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

public class MutualCallbackAuthentication<C extends Context> implements AuthenticationMechanism {

	public static final int SSH_MSG_USERAUTH_SIGNED_CHALLENGE = 60;
	
	AbstractServerTransport<C> transport;
	AbstractAuthenticationProtocol<C> authentication;
	SshConnection con;
	MutualCallbackAuthenticationProvider provider;
	
	public static final String AUTHENTICATION_METHOD = "publickey";
	
	public MutualCallbackAuthentication(AbstractServerTransport<C> transport,
			AbstractAuthenticationProtocol<C> authentication, SshConnection con,
			MutualCallbackAuthenticationProvider provider) {
		this.transport = transport;
		this.authentication = authentication;
		this.con = con;
		this.provider = provider;
	}

	@Override
	public boolean startRequest(String username, byte[] msg) throws IOException {
		
		transport.addTask(ExecutorOperationSupport.EVENTS, new ProcessRemoteChallenge(con, username, msg));
		return true;
	}

	@Override
	public boolean processMessage(byte[] msg) throws IOException {
		
		if(msg[0]!=SSH_MSG_USERAUTH_SIGNED_CHALLENGE) {
			return false;
		}
		
		transport.addTask(ExecutorOperationSupport.EVENTS, new ProcessLocalChallenge(con, msg));
		return true;
	}

	@Override
	public String getMethod() {
		return MutualCallbackAuthenticationProvider.MUTUAL_KEY_AUTHENTICATION;
	}
	
	
	class ProcessRemoteChallenge extends ConnectionAwareTask {

		byte[] msg;
		String username;
		public ProcessRemoteChallenge(SshConnection con, String username, byte[] msg) {
			super(con);
			this.username = username;
			this.msg = msg;
		}

		@Override
		protected void doTask() throws Throwable {
			
			try(ByteArrayReader bar = new ByteArrayReader(msg)) {
				
				byte[] theirChallenge = bar.readBinaryString();
				try(ByteArrayWriter writer = new ByteArrayWriter()) {
					writer.writeBinaryString(theirChallenge);
					writer.writeString(username);
					writer.writeBinaryString(transport.getSessionKey());
					
					SshKeyPair key = provider.getLocalPrivateKey(username);
					if(Objects.isNull(key)) {
						authentication.failedAuthentication();
						return;
					}
					byte[] signed = key.getPrivateKey().sign(writer.toByteArray());
					
					byte[] ourChallenge = new byte[512];
					JCEComponentManager.getSecureRandom().nextBytes(ourChallenge);
					con.setProperty("ourChallenge", ourChallenge);
					con.setProperty("username", username);
					transport.postMessage(new SshMessage() {
						public boolean writeMessageIntoBuffer(ByteBuffer buf) {
							buf.put((byte) SSH_MSG_USERAUTH_SIGNED_CHALLENGE);
							buf.putInt(signed.length);
							buf.put(signed);
							buf.putInt(ourChallenge.length);
							buf.put(ourChallenge);
							return true;
						}
	
						public void messageSent(Long sequenceNo) {
							if(Log.isDebugEnabled())
								Log.debug("Sent SSH_MSG_USERAUTH_SIGNED_CHALLENGE");
						}
					});
				}
				
			}
			
		}
		
	}
	
	class ProcessLocalChallenge extends ConnectionAwareTask {

		byte[] msg;
		public ProcessLocalChallenge(SshConnection con, byte[] msg) {
			super(con);
			this.msg = msg;
		}

		@Override
		protected void doTask() throws Throwable {
			
			try(ByteArrayReader bar = new ByteArrayReader(msg)) {
				
				bar.skip(1);
				byte[] signature = bar.readBinaryString();
				String username = con.getProperty("username").toString();
				
				try(ByteArrayWriter writer = new ByteArrayWriter()) {
					writer.writeBinaryString((byte[])con.getProperty("ourChallenge"));
					writer.writeString(username);
					writer.writeBinaryString(transport.getSessionKey());

					SshPublicKey key = provider.getRemotePublicKey(username);
					
					if(Objects.isNull(key)) {
						authentication.failedAuthentication();
					}
					if(key.verifySignature(signature, writer.toByteArray())) {
						authentication.completedAuthentication();
					} else {
						authentication.failedAuthentication();
					}
				}
				
			}
			
		}
		
	}

}
