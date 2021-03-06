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
package com.sshtools.common.tests;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;

import com.sshtools.common.knownhosts.HostKeyVerification;
import com.sshtools.common.knownhosts.KnownHostsKeyVerification;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.SshException;

import junit.framework.TestCase;

@Ignore
public abstract class AbstractKnownHostsTests extends TestCase {

	public abstract KnownHostsKeyVerification loadKnownHosts(InputStream in) throws SshException, IOException;

	
	public void testLoading() throws SshException, IOException {
		loadKnownHosts(getClass().getResourceAsStream("/known_hosts"));
	}
	
	public void testOutputMatches() throws SshException, IOException {
		
		
		HostKeyVerification k = loadKnownHosts(getClass().getResourceAsStream("/known_hosts"));
		
		String output = k.toString();
		
		String originalContent = IOUtils.toString(getClass().getResourceAsStream("/known_hosts"), "UTF-8");
		
		/**
		 * Assert the original and output are the same, disregard newline convention which may differ
		 * between operating systems.
		 */
		assertEquals("The known_hosts file should be written exactly as it was read", 
				output.trim().replace("\r", "").replace("\n", ""), 
				originalContent.trim().replace("\r", "").replace("\n", ""));
	}
	
	public void testValidEntries() throws SshException, IOException {
		
		HostKeyVerification k = loadKnownHosts(getClass().getResourceAsStream("/known_hosts"));
		
		assertTrue(k.verifyHost("localhost", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		assertTrue(k.verifyHost("127.0.0.1", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		
	}
	
	public void testNonStandardPortEntries() throws SshException, IOException {
		
		HostKeyVerification k = loadKnownHosts(getClass().getResourceAsStream("/known_hosts"));
		
		assertTrue(k.verifyHost("[localhost]:4022", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa2048.pub"))));
		assertTrue(k.verifyHost("[127.0.0.1]:4022", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa2048.pub"))));
		
	}
	
	public void testHashedEntries() throws SshException, IOException {
		
		HostKeyVerification k = loadKnownHosts(getClass().getResourceAsStream("/known_hosts"));
		
		assertTrue(k.verifyHost("[localhost]:4022", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		assertTrue(k.verifyHost("[127.0.0.1]:4022", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		
	}

	public void testWildcardEntries() throws SshException, IOException {
		
		HostKeyVerification k = loadKnownHosts(getClass().getResourceAsStream("/known_hosts"));
		
		assertTrue(k.verifyHost("node1.sshtools.com", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		assertTrue(k.verifyHost("node2.sshtools.com", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		assertFalse(k.verifyHost("node2xsshtoolsxcom", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		
		assertTrue(k.verifyHost("server1.sshtools.net", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		assertTrue(k.verifyHost("server2.sshtools.net", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		assertFalse(k.verifyHost("server22.sshtools.net", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		assertFalse(k.verifyHost("server1xsshtoolsxnet", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		
	}
	
	public void testNegatedEntries() throws SshException, IOException {
		
		HostKeyVerification k = loadKnownHosts(getClass().getResourceAsStream("/known_hosts"));
		
		assertFalse(k.verifyHost("localhost", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/ecdsa256.pub"))));
		assertTrue(k.verifyHost("127.0.0.2", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/ecdsa256.pub"))));
	}
	
	public void testRevokedEntries() throws SshException, IOException {
		
		HostKeyVerification k = loadKnownHosts(getClass().getResourceAsStream("/known_hosts"));
		
		assertFalse(k.verifyHost("localhost", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/ecdsa384.pub"))));
		assertFalse(k.verifyHost("127.0.0.1", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/ecdsa384.pub"))));
		assertFalse(k.verifyHost("sshtools.com", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/ecdsa384.pub"))));
		
	}
	
	public void testNonCanonicalEntries() throws SshException, IOException {
		
		KnownHostsKeyVerification k = loadKnownHosts(getClass().getResourceAsStream("/known_hosts"));
		k.setUseCanonicalHostnames(false);
		
		assertFalse(k.verifyHost("127.0.0.1", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/dsa1024.pub"))));
		assertTrue(k.verifyHost("localhost", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/dsa1024.pub"))));
		
		// These are hashed entries, only the first is valid
		assertFalse(k.verifyHost("[127.0.0.1]:4022", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
	}
	
	public void testNonReverseDNSEntries() throws SshException, IOException {
		
		KnownHostsKeyVerification k = loadKnownHosts(getClass().getResourceAsStream("/known_hosts"));
		k.setUseReverseDNS(false);
		k.setUseCanonicalHostnames(false);
		
		assertTrue(k.verifyHost("127.0.0.1", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/ecdsa521.pub"))));
		assertFalse(k.verifyHost("localhost", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/ecdsa521.pub"))));
		
		// These are hashed entries, only the first is valid
		assertTrue(k.verifyHost("[localhost]:4022", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		assertFalse(k.verifyHost("[127.0.0.1]:4022", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		
	}
}
