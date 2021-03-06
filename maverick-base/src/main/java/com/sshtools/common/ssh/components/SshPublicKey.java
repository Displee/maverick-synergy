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
/* HEADER */
package com.sshtools.common.ssh.components;

import java.security.Key;

import com.sshtools.common.ssh.SecureComponent;
import com.sshtools.common.ssh.SshException;


/**
 * <p>Interface for SSH supported public keys.</p>
 * @author Lee David Painter
 */
public interface SshPublicKey extends SecureComponent {

  /**
   * Initialize the public key from a blob of binary data.
   * @param blob
   * @param start
   * @param len
   * @throws SshException
   */
  public void init(byte[] blob, int start, int len) throws SshException;

  /**
   * Get the algorithm name for the public key.
   * @return the algorithm name, for example "ssh-dss"
   */
  public String getAlgorithm();

  
  /**
   * The algorithm name expected to be encoded in SSH signatures
   */
  public String getSigningAlgorithm();
  
  
  /**
   * The algorithm name used in the encoding of the public key
   */
  public String getEncodingAlgorithm();
  
  /**
   * Get the bit length of the public key
   * @return the bit length of the public key
   */
  public int getBitLength();

  /**
       * Encode the public key into a blob of binary data, the encoded result will be
   * passed into init to recreate the key.
   *
   * @return an encoded byte array
   * @throws SshException
   */
  public byte[] getEncoded() throws SshException;

  /**
   * Return an SSH fingerprint of the public key
   * @return String
   * @throws SshException
   */
  public String getFingerprint() throws SshException;

  /**
   * Verify the signature.
   * @param signature
   * @param data
   * @return <code>true</code> if the signature was produced by the corresponding
   * private key that owns this public key, otherwise <code>false</code>.
   * @throws SshException
   */
  public boolean verifySignature(byte[] signature,
                                 byte[] data)
     throws SshException;
  
  /**
   * Test the JCE for algorithm availability.
 * @return 
   */
  public String test();

  /**
   * Return the JCE component for this key.
   * @return
   */
  public Key getJCEPublicKey();

  /**
   * Indicates if this key is part of a certificate.
   * @return
   */
  public default boolean isCertificate() {
	return false;
}
}
