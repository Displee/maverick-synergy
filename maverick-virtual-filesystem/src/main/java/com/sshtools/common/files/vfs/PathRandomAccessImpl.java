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
package com.sshtools.common.files.vfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.sshtools.common.files.AbstractFileRandomAccess;

public class PathRandomAccessImpl implements AbstractFileRandomAccess {
	
	protected FileChannel raf;
	protected Path f;
	
	public PathRandomAccessImpl(Path f, boolean writeAccess) throws IOException {
		this.f = f;
		if(writeAccess)
			raf = FileChannel.open(f, StandardOpenOption.READ, StandardOpenOption.WRITE);
		else
			raf = FileChannel.open(f, StandardOpenOption.READ);
	}
	
	public void write(int b) throws IOException {
		raf.write(ByteBuffer.wrap(new byte[] {(byte)b}));
	}
	
	public void write(byte[] buf, int off, int len) throws IOException {
		raf.write(ByteBuffer.wrap(buf, off, len));
	}

	public void close() throws IOException {
		raf.close();
	}
	
	public void seek(long position) throws IOException {
		raf.position(position);
	}
	
	public int read(byte[] buf, int off, int len) throws IOException {
		ByteBuffer b = ByteBuffer.allocate(len);
		int r = raf.read(b);
		if(r != -1) {
			byte[] arr = b.array(); 
			System.arraycopy(arr, 0, buf, off, arr.length);
		}
		return r;
	}
	
	public void setLength(long length) throws IOException {
		raf.truncate(length);
	}
	
	public long getFilePointer() throws IOException {
		return raf.position();
	}
}