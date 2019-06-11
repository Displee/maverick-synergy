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
package com.sshtools.common.files;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;

public class AbstractFileAdapter implements AbstractFile {

	protected AbstractFile file;
	
	public AbstractFileAdapter(AbstractFile file) {
		this.file = file;
	}
	
	public AbstractFileAdapter() {
	}
	
	protected void init(AbstractFile file) {
		this.file = file;
	}
	
	public boolean exists() throws IOException {
		return file.exists();
	}

	public boolean createFolder() throws IOException, PermissionDeniedException {
		return file.createFolder();
	}

	public long lastModified() throws IOException{
		return file.lastModified();
	}

	public String getName() {
		return file.getName();
	}

	public long length() throws IOException {
		return file.length();
	}

	public SftpFileAttributes getAttributes() throws IOException, PermissionDeniedException {
		return file.getAttributes();
	}

	public boolean isDirectory() throws IOException{
		return file.isDirectory();
	}

	public List<AbstractFile> getChildren() throws IOException, PermissionDeniedException {
		return file.getChildren();
	}

	public boolean isFile() throws IOException {
		return file.isFile();
	}

	public String getAbsolutePath() throws IOException, PermissionDeniedException {
		return file.getAbsolutePath();
	}

	public boolean isReadable() throws IOException {
		return file.isReadable();
	}

	public boolean createNewFile() throws PermissionDeniedException,
			IOException {
		return file.createNewFile();
	}

	public void truncate() throws PermissionDeniedException, IOException {
		file.truncate();
	}

	public InputStream getInputStream() throws IOException {
		return file.getInputStream();
	}

	public void setAttributes(SftpFileAttributes attrs) throws IOException {
		file.setAttributes(attrs);
	}

	public String getCanonicalPath() throws IOException, PermissionDeniedException {
		return file.getCanonicalPath();
	}

	public boolean supportsRandomAccess() {
		return file.supportsRandomAccess();
	}

	public AbstractFileRandomAccess openFile(boolean writeAccess) throws IOException {
		return file.openFile(writeAccess);
	}

	public boolean isHidden() throws IOException {
		return file.isHidden();
	}

	public OutputStream getOutputStream() throws IOException {
		return file.getOutputStream();
	}

	public boolean isWritable() throws IOException {
		return file.isWritable();
	}

	public void copyFrom(AbstractFile src) throws IOException,
			PermissionDeniedException {
		file.copyFrom(src);		
	}

	public void moveTo(AbstractFile target) throws IOException,
			PermissionDeniedException {
		file.moveTo(target);
	}

	public boolean delete(boolean recursive) throws IOException,
			PermissionDeniedException {
		return file.delete(recursive);
		
	}

	public void refresh() {
		file.refresh();
	}

	public OutputStream getOutputStream(boolean append) throws IOException {
		return file.getOutputStream(append);
	}

	public AbstractFile resolveFile(String child) throws IOException, PermissionDeniedException {
		return file.resolveFile(child);
	}

	public AbstractFileFactory<? extends AbstractFile> getFileFactory() {
		return file.getFileFactory();
	}
}