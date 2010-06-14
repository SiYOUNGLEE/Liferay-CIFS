/*
 * Copyright (C) 2006-2008 Alfresco Software Limited.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 * 
 * As a special exception to the terms and conditions of version 2.0 of the GPL,
 * you may redistribute this Program in connection with Free/Libre and Open
 * Source Software ("FLOSS") applications as described in Alfresco's FLOSS
 * exception. You should have recieved a copy of the text describing the FLOSS
 * exception, and it is also available here:
 * http://www.alfresco.com/legal/licensing"
 */

package org.alfresco.jlan.server.filesys.db;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.alfresco.jlan.server.filesys.cache.FileState;
import org.alfresco.jlan.smb.SeekType;

/**
 * Local Data Network File Class <p>Maps a file in a virtual filesystem to a
 * file in the local filesystem.
 * 
 * @author gkspencer
 */
public class LocalDataNetworkFile extends DBNetworkFile {

	// File details

	protected File m_file;

	// Random access file used to read/write the actual file

	protected RandomAccessFile m_io;

	// End of file flag

	protected boolean m_eof;

	/**
	 * Class constructor
	 * 
	 * @param name String
	 * @param fid int
	 * @param did int
	 * @param file File
	 */
	public LocalDataNetworkFile(String name, int fid, int did, File file) {
		super(name, fid, 0, did);

		// Set the file details

		m_file = file;

		// Set the file size

		setFileSize(m_file.length());
		m_eof = false;

		// Set the modification date/time, if available

		setModifyDate(m_file.lastModified());
	}

	/**
	 * Open the file
	 * 
	 * @param createFlag boolean
	 * @exception IOException
	 */
	public void openFile(boolean createFlag)
		throws IOException {

		// Open the local file

		m_io = new RandomAccessFile(m_file, "rw");
	}

	/**
	 * Read from the file.
	 * 
	 * @param buf byte[]
	 * @param len int
	 * @param pos int
	 * @param fileOff int
	 * @return Length of data read.
	 * @exception IOException
	 */
	public int readFile(byte[] buf, int len, int pos, long fileOff)
		throws IOException {

		// Open the file, if not already open

		if (m_io == null)
			openFile(false);

		// Seek to the read position

		m_io.seek(fileOff);

		// Read from the file

		int rdlen = m_io.read(buf, pos, len);
		return rdlen;
	}

	/**
	 * Write a block of data to the file.
	 * 
	 * @param buf byte[]
	 * @param len int
	 * @param pos int
	 * @param offset int
	 * @exception IOException
	 */
	public void writeFile(byte[] buf, int len, int pos, long offset)
		throws IOException {

		// Open the file, if not already open

		if (m_io == null)
			openFile(false);

		// We need to seek to the write position. If the write position is off
		// the end of the file
		// we must null out the area between the current end of file and the
		// write position.

		long fileLen = m_io.length();
		long endpos = offset + (long) len;

		if (endpos > fileLen) {

			// Extend the file

			m_io.setLength(endpos);
		}

		// Check for a zero length write

		if (len == 0)
			return;

		// Seek to the write position

		m_io.seek(offset);

		// Write to the file

		m_io.write(buf, pos, len);
		incrementWriteCount();
		setStatus(FileState.FILE_UPDATED);
	}

	/**
	 * Flush any buffered output to the file
	 * 
	 * @throws IOException
	 */
	public final void flushFile()
		throws IOException {

		// Flush any buffered data

		if (m_io != null)
			m_io.getFD().sync();
	}

	/**
	 * Seek to the specified file position.
	 * 
	 * @param pos long
	 * @param typ int
	 * @return long
	 * @exception IOException
	 */
	public long seekFile(long pos, int typ)
		throws IOException {

		// Open the file, if not already open

		if (m_io == null)
			openFile(false);

		// Check if the current file position is the required file position

		long curPos = m_io.getFilePointer();

		switch (typ) {

		// From start of file

		case SeekType.StartOfFile:
			if (curPos != pos)
				m_io.seek(pos);
			break;

		// From current position

		case SeekType.CurrentPos:
			m_io.seek(curPos + pos);
			break;

		// From end of file

		case SeekType.EndOfFile: {
			long newPos = m_io.length() + pos;
			m_io.seek(newPos);
		}
			break;
		}

		// Return the new file position

		return (int) (m_io.getFilePointer() & 0xFFFFFFFF);
	}

	/**
	 * Truncate the file to the specified file size
	 * 
	 * @param siz long
	 * @exception IOException
	 */
	public void truncateFile(long siz)
		throws IOException {

		// Open the file, if not already open

		if (m_io == null)
			openFile(false);

		// Set the file length

		m_io.setLength(siz);

		// Indicate that the file data has changed

		incrementWriteCount();
	}

	/**
	 * Close the file
	 */
	public void closeFile() {

		// Close the file, if used

		if (m_io != null) {

			// Close the file

			try {
				m_io.close();
			}
			catch (Exception ex) {
			}
			m_io = null;

			// Set the last modified date/time for the file

			if (this.getWriteCount() > 0)
				m_file.setLastModified(System.currentTimeMillis());

			// Set the new file size

			setFileSize(m_file.length());
		}
	}

}
