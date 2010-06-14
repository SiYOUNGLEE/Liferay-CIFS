/* 
 * Copyright © 2010 Arcusys Ltd. - http://www.arcusys.fi/
 * 
 * This file is part of "LiferayCIFS".
 *
 * "LiferayCIFS" is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * "LiferayCIFS" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with "LiferayCIFS".  If not, see <http://www.gnu.org/licenses/>.
 */

package com.arcusys.liferay.smb;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.alfresco.jlan.server.filesys.FileInfo;
import org.alfresco.jlan.server.filesys.NetworkFile;
import org.alfresco.jlan.smb.SeekType;

/**
 * Implementation of the NetworkFile abstract class used with the
 * DocumentLibraryDiskDriver
 */
public class DocumentLibraryNetworkFile extends NetworkFile {

	/**
	 * Create a DocumentLibraryNetworkFile instance based on a FileInfo object
	 * 
	 * @param info FileInfo object to get infomation from
	 */
	public DocumentLibraryNetworkFile(FileInfo info) {
		super(info.getFileName());
		this.setFileSize(info.getSize());
		this.setAttributes(info.getFileAttributes());
		liferayName = info.getShortName();

		// try{
		// FileWriter fstream = new
		// FileWriter("DocumentLibraryNetworkFile.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("fileName:"+info.getFileName());
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e);
		// }
		// Current prototype takes only name & size

	}

	/**
	 * Nothing special happens yet when file is closed. This is a stub. Invoked
	 * both by JLAN and the implementation of DiskDriver
	 */
	@Override
	public void closeFile() throws IOException {

		if (m_io != null) {
			m_io.close();
			m_io = null;
		}
		if (fileName != null) {

			// th

			if (fileName.startsWith("write")) {

				// File file = new File("write"+FileName.splitPath(""));

				// if (m_io!=null)
				// {
				// try{
				// FileWriter fstream = new
				// FileWriter("DocumentLibraryCloseFile.txt",true);
				// BufferedWriter out = new BufferedWriter(fstream);
				// out.newLine();
				// out.write("________");
				// out.newLine();
				// out.write("fileName:"+fileName);
				// out.newLine();
				// out.write("this.getFullName():"+this.getFullName());
				// // for(int i =0;i<m_io.length();i++)
				// // {out.write(m_io.readChar());}
				// out.write("write buf");
				// out.close();
				// } catch (Exception e) {
				// System.err.println("Error: " + e);
				// }
				
				File currentFile = new File(fileName);
				
				// InputStream istream = new FileInputStream(currentFile);
				//
				// long length = currentFile.length();
				// byte[] bytes = new byte[(int)length];
				// int offset = 0;
				// int numRead = 0;
				//
				// while (offset < bytes.length && (numRead=istream.read(bytes,
				// offset, bytes.length-offset)) >= 0) {
				// offset += numRead;
				// }
				// istream.close();
				
				LiferayStorage.updateFile(this.getFullName(), currentFile);
				
				// m_io.close();
				// for(int i =0;i<bytes.length;i++)
				// {out.write((char)bytes[i]);}

				// }
			}

		}

		// deleteFile

		System.gc();
		if (fileName != null) {

			File file = new File(fileName);
			if (file.exists()) {

				// file.

				boolean status = file.delete();

				// throw new UnsupportedOperationException(fileName +
				// " deleting status:"+status);

			}
		}
		setClosed(true);

		// boolean b = false;
	}

	public long currentPosition() {

		// Check if the file is open

		try {
			if (m_io != null)
				return m_io.getFilePointer();
		}
		catch (Exception ex) {
		}
		return 0;
	}

	/**
	 * The operation to flush the buffered output to the file is not implemented
	 * yet. Invoked by the implementation of DiskDriver
	 * 
	 * @throws IOException
	 */
	@Override
	public void flushFile() throws IOException {

		// try{
		// FileWriter fstream = new
		// FileWriter("DocumentLibraryFlushFile.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e);
		// }

		if (m_io != null)
			m_io.getFD().sync();

		// boolean b = false;
		// throw new UnsupportedOperationException("Not supported yet.");

	}

	public String getLiferayName() {
		return liferayName;
	}

	/**
	 * The operation to open file is not implemented yet
	 * 
	 * @param createFlag
	 * @throws IOException
	 */
	@Override
	public void openFile(boolean createFlag) throws IOException {

		// try{
		// FileWriter fstream = new
		// FileWriter("DocumentLibraryOpenFile.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("createFlag:"+createFlag);
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e);
		// }

		setClosed(false);
		boolean h = createFlag;

		// throw new UnsupportedOperationException("Not supported yet.");

	}

	/**
	 * The operation to read file content is not implemented yet (stubbed to
	 * return 0). Invoked by the implementation of DiskDriver
	 * 
	 * @param buf
	 * @param len
	 * @param pos
	 * @param fileOff
	 * @return Number of bytes actually read. Currently returns 0
	 * @throws IOException
	 */
	@Override
	public int readFile(byte[] buf, int len, int pos, long fileOff)
		throws IOException {

		// try{
		// FileWriter fstream = new
		// FileWriter("DocumentLibraryReadFile.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("buf"+buf.length);
		// out.newLine();
		// out.write("fileName:"+this.getFullName());
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e);
		// }

		if (m_io == null) {

			// liferayName = LiferayStorage.ReadFile( this.getFullName());

			File fileForRead = LiferayStorage.readFile(this.getFullName());
			if (fileForRead == null) {
				return 0;
			}
			m_io = new RandomAccessFile(fileForRead, "r");
			fileName = fileForRead.getName();
		}

		if (currentPosition() != fileOff)
			seekFile(fileOff, SeekType.StartOfFile);

		// Read from the file

		if (m_io != null) {
			return m_io.read(buf, pos, len);
		}
		else {
			return 0;
		}

	}

	/**
	 * The operation to seek to a position in a file is not implemented yet.
	 * Invoked by the implementation of DiskDriver and by PseudoNetworkFile
	 * class (if it is used)
	 * 
	 * @param pos
	 * @param typ
	 * @return
	 * @throws IOException
	 */
	@Override
	public long seekFile(long pos, int typ) throws IOException {

		// try{
		// FileWriter fstream = new
		// FileWriter("DocumentLibrarySeekFile.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("fileName:"+fileName);
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e);
		// }

		if (m_io == null)

			// openFile(false);

			// Check if the current file position is the required file position

			switch (typ) {

			// From start of file

			case SeekType.StartOfFile:
				if (currentPosition() != pos)
					m_io.seek(pos);
				break;

			// From current position

			case SeekType.CurrentPos:
				m_io.seek(currentPosition() + pos);
				break;

			// From end of file

			case SeekType.EndOfFile: {
				long newPos = m_io.length() + pos;
				m_io.seek(newPos);
			}
				break;
			}
		return currentPosition();

		// throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * The operation to trunctate the file to a given size is not implemented
	 * yet. Invoked by the implementation of DiskDriver
	 * 
	 * @param siz
	 * @throws IOException
	 */
	@Override
	public void truncateFile(long siz) throws IOException {

		// try{
		// FileWriter fstream = new
		// FileWriter("DocumentLibraryTruncateFile.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e);
		// }

		boolean b = false;

		// throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * The operation to write file content is not implemented yet. Invoked by
	 * the implementation of DiskDriver
	 * 
	 * @param buf
	 * @param len
	 * @param pos
	 * @param fileOff
	 * @throws IOException
	 */
	@Override
	public void writeFile(byte[] buf, int len, int pos, long offset)
		throws IOException {

		// try{
		// FileWriter fstream = new
		// FileWriter("DocumentLibraryWriteFile.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");

		// boolean b = false;

		if (m_io == null) {
			fileName = LiferayStorage.getFileNameForWrite(this.getFullName());

			// if (fileName==null)
			// {throw FileNotFoundException(this.getFullName());}

			m_io = new RandomAccessFile(fileName, "rw");
			if (m_io == null) {
				return;
			}
		}

		// out.newLine();
		// out.write("fileName:"+fileName);
		// out.newLine();
		// out.write("this.getFullName():"+this.getFullName());

		// FileOutputStream str = new FileOutputStream("write"+
		// FileName.splitPath(file.getFullName())[1],true);
		// m_io.write(buf, pos, len);
		// m_io.close();
		// Write to the file

		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e);
		// }
		// Update the write count for the file

		long fileLen = m_io.length();

		if (offset > fileLen) {

			// Extend the file

			m_io.setLength(offset + len);
		}

		// Check for a zero length write

		if (len == 0)
			return;

		// Seek to the write position

		m_io.seek(offset);

		// Write to the file

		m_io.write(buf, pos, len);

		// Update the write count for the file

		incrementWriteCount();
	}

	protected RandomAccessFile m_io;
	protected String fileName;
	protected String liferayName;

}
