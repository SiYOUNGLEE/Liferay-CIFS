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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.alfresco.jlan.server.SrvSession;
import org.alfresco.jlan.server.core.DeviceContext;
import org.alfresco.jlan.server.core.DeviceContextException;
import org.alfresco.jlan.server.filesys.AccessDeniedException;
import org.alfresco.jlan.server.filesys.DiskDeviceContext;
import org.alfresco.jlan.server.filesys.DiskInterface;
import org.alfresco.jlan.server.filesys.FileAttribute;
import org.alfresco.jlan.server.filesys.FileInfo;
import org.alfresco.jlan.server.filesys.FileOpenParams;
import org.alfresco.jlan.server.filesys.NetworkFile;
import org.alfresco.jlan.server.filesys.SearchContext;
import org.alfresco.jlan.server.filesys.TreeConnection;

import org.springframework.extensions.config.ConfigElement;

/**
 * The implementation of DiskInterface utilizing a Liferay document library
 * folder as the underlying share storage
 */
public class DocumentLibraryDiskDriver implements DiskInterface {

	/**
	 * Close the previously opened file
	 * 
	 * @param sess Session details
	 * @param tree Tree connection
	 * @param param The network file to close
	 * @throws IOException
	 */
	public void closeFile(
		SrvSession sess, TreeConnection tree, NetworkFile param)
		throws IOException {

		// File file = new
		// File("write"+LiferayStorage.GetLiferayFileName(param.getFullName()));

		if (param.hasDeleteOnClose()) {
			if (param.isDirectory()) {
				deleteDirectory(
					sess, tree, _getLiferayFullPath(param.getFullName()));
			}
			else {
				deleteFile(
					sess, tree, _getLiferayFullPath(param.getFullName()));
			}

		}

		param.closeFile();
		
		// file = new
		// File("read"+LiferayStorage.GetLiferayFileName(param.getFullName()));
		// if(file.exists())
		// {

		// try{
		// FileWriter fstream = new FileWriter("closeFile.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("fileName:"+param.getFullName());
		// out.newLine();
		// out.write("file.exists():"+file.exists());
		// out.close();
		// throw new
		// UnsupportedOperationException("read"+FileName.splitPath(
		//		param.getFullName())[1]);
		// sess.debugPrintln("close true " +
		// LiferayStorage.GetLiferayFileName(param.getFullName()));
		// System.gc();
		// boolean status = file.delete();
		//
		// sess.debugPrintln("deleting status " + status);
		// out.newLine();
		// out.write("file.exists() after delete:"+file.exists());
		// out.close();

		// } catch (Exception e) {
		// System.err.println("Error: " + e.getMessage());
		// }
		// else {
		// throw new
		// UnsupportedOperationException("read"+FileName.splitPath(
		// 		param.getFullName())[1]);
		// sess.debugPrintln("close false " +
		// "read"+FileName.splitPath(param.getFullName())[1]);
		// }
		
	}

	/**
	 * Initialize a disk share. This method is called by JLAN at startup once
	 * for each share.
	 * 
	 * @param name The name of the share
	 * @param args The configuration files section corresponding to a share
	 * @return The device context with root path in document library used as
	 *         device name
	 * @throws DeviceContextException
	 */
	public DeviceContext createContext(String name, ConfigElement args)
		throws DeviceContextException {
		
		// The LiferaySharedFolder share configuration parameter stores the name
		// of the
		// Liferay folder we wish to share together with all its content
		// It should start with the topmost folder name, no leading backslash or
		// point allowed
		// One tailing backslash is allowed
		// try{
		// FileWriter fstream = new FileWriter("createContext.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("name:"+name);
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e);
		// }

		ConfigElement path = args.getChild("LiferaySharedFolder");

		_m_liferaySharedFolder = path.getValue();
		
		// Trim tailing backslash if there is one
		
		if (_m_liferaySharedFolder.endsWith("\\")) {
			_m_liferaySharedFolder = _m_liferaySharedFolder.substring(
					0, _m_liferaySharedFolder.length() - 1);
		}
		
		// Use Liferay shared folder name as device name
		
		DiskDeviceContext context =
			new DiskDeviceContext(_m_liferaySharedFolder);
		
		// Detect the group ID to access Liferay and remember the URL, username
		// and password
		
		LiferayStorage.initStorage(
			args.getChild("LiferayURL").getValue(),
			args.getChild("LiferayUser").getValue(), 
			args.getChild("LiferayUserId").getValue(),
			args.getChild("LiferayPassword").getValue());
		return context;
	}

	/**
	 * The operation for creating directories is not implemented yet
	 * 
	 * @param sess
	 * @param tree
	 * @param params
	 * @throws IOException
	 */
	public void createDirectory(
		SrvSession sess, TreeConnection tree, FileOpenParams params)
		throws IOException {
		
		// try{
		// FileWriter fstream = new FileWriter("createDirectory.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("name:"+params.getFullPath());
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e);
		// }
		// sess.debugPrintln("createDirectoryFullPath():"+params.getFullPath());
		// sess.debugPrintln("createDirectoryPath():"+params.getPath());
		// sess.debugPrintln("createDirectoryShareName():"+
		// tree.getContext().getShareName());
		// sess.debugPrintln("createDirectoryDeviceName():"+
		// tree.getContext().getDeviceName());
		
		LiferayStorage.createFolder(_getLiferayFullPath(params.getFullPath()));
		
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * The operation for creating files is not implemented yet
	 * 
	 * @param sess
	 * @param tree
	 * @param params
	 * @return
	 * @throws IOException
	 */
	public NetworkFile createFile(
		SrvSession sess, TreeConnection tree, FileOpenParams params)
		throws IOException {
		
		sess.debugPrintln("createFileFullPath():" + params.getFullPath());
		
		// try{
		// FileWriter fstream = new FileWriter("createFile.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("name:"+params.getPath());
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e);
		// }
		
		FileInfo info =
			LiferayStorage.createFile(
				_getLiferayFullPath(params.getFullPath()));
		String liferayFullPath = _getLiferayFullPath(params.getPath());
		
		// FileInfo info = getFileInformation(sess, tree, params.getPath());
		
		if (info == null) {
			
			// return null;
			
			throw new UnsupportedOperationException("CreateFileError");
		}
		NetworkFile networkFile = new DocumentLibraryNetworkFile(info);
		networkFile.setFullName(liferayFullPath);
		return networkFile;
		
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * The operation for deleting folders is not implemented yet
	 * 
	 * @param sess
	 * @param tree
	 * @param dir
	 * @throws IOException
	 */
	public void deleteDirectory(
		SrvSession sess, TreeConnection tree, String dir)
		throws IOException {
		
		// try{
		// FileWriter fstream = new FileWriter("deleteDirectory.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("name:"+dir);
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e);
		// }
		
		LiferayStorage.deleteFolder(_getLiferayFullPath(dir));
		
		// throw new UnsupportedOperationException("Not supported yet.");
		
	}

	/**
	 * The operation for deleting files is not implemented yet
	 * 
	 * @param sess
	 * @param tree
	 * @param name
	 * @throws IOException
	 */
	public void deleteFile(SrvSession sess, TreeConnection tree, String name)
		throws IOException {
		
		// try{
		// FileWriter fstream = new FileWriter("deleteFile.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("name:"+name);
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e);
		// }
		
		LiferayStorage.deleteFile(name);
		
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Check if a file or directory exists in the Liferay-based share. This
	 * method is called by JLAN when the SBM client checks file existance.
	 * 
	 * @param sess Session details
	 * @param tree Tree connection
	 * @param name Full file name starting with a backslash relative to share
	 *            root
	 * @return FileStatus.DirectoryExists if a directory with such a name
	 *         exists, FileStatus.FileExists if a file with such a name exists,
	 *         or FileStatus.NotExist otherwise
	 */
	public int fileExists(SrvSession sess, TreeConnection tree, String name) {
		
		// try{
		// FileWriter fstream = new FileWriter("fileExists.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("FileName:"+name+"Status:"+
		// LiferayStorage.FileExists(getLiferayFullPath(name)));
		// out.newLine();
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e);
		// }
		// sess.debugPrintln("fileExists:"+getLiferayFullPath(name)+" 
		// "+LiferayStorage.FileExists(getLiferayFullPath(name)));
		// Check file or folder existence if Liferay
		
		return LiferayStorage.fileExists(_getLiferayFullPath(name));
	}

	/**
	 * The operation for flushing the buffered output to the file is not
	 * implemented yet.
	 * 
	 * @param sess Session details
	 * @param tree ree connection
	 * @param file
	 * @throws IOException
	 */
	public void flushFile(SrvSession sess, TreeConnection tree, 
		NetworkFile file)
		throws IOException {
		
		// try{
		// FileWriter fstream = new FileWriter("flushFile.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("fileName:"+file.getFullName());
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e);
		// }
		// sess.debugPrintln(m_liferaySharedFolder);
		// throw new UnsupportedOperationException("Not supported yet.");
		
	}

	/**
	 * Get file or directory information by name
	 * 
	 * @param sess Session details
	 * @param tree Tree connection
	 * @param name Full path to file relative to share root, starts with a
	 *            backslash
	 * @return FileInfo object with information regarding file or null if file
	 *         was not found
	 * @throws IOException
	 */
	public FileInfo getFileInformation(
		SrvSession sess, TreeConnection tree, String name)
		throws IOException {

		// try{
		// FileWriter fstream = new FileWriter("getFileInformation.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		//
		// out.write("fileName:"+getLiferayFullPath(name));
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e.getMessage());
		// }
		// sess.debugPrintln("getFileInformation:"+name);
		
		FileInfo fileInfo = null;
		try {
			if (name.equals("\\") || name.equals("")) {
				fileInfo = new FileInfo();
				fileInfo.setFileName(_m_liferaySharedFolder);
				fileInfo.setFileAttributes(FileAttribute.Directory);
			}
			else {
				
				// fileInfo = LiferayStorage.GetFileInfo(name);
				
				fileInfo = LiferayStorage.getFileInfo(
					_getLiferayFullPath(name));
			}
			
			// sess.debugPrintln("GetFileInfo:"+getLiferayFullPath(name));
			// sess.debugPrint("FileName:");
			// sess.debugPrintln(fileInfo.getFileName());
			// FileWriter fstream = new
			// FileWriter("getFileInformation.txt",true);
			// BufferedWriter out = new BufferedWriter(fstream);
			// out.newLine();
			// out.write("________");
			// out.newLine();
			// out.write("fileName:"+getLiferayFullPath(name));
			// out.newLine();
			// out.write("isDirectory:"+fileInfo.isDirectory());
			// out.close();

			return fileInfo;
		}
		catch (Exception ex) {
			return null;
		}
	}

	/**
	 * The operation for setting file properties is not implemented yet.
	 * 
	 * @param sess
	 * @param tree
	 * @param name
	 * @param info
	 * @throws IOException
	 */
	public void setFileInformation(
		SrvSession sess, TreeConnection tree, String name, FileInfo info)
		throws IOException {
		
		// try{
		// FileWriter fstream = new FileWriter("setFileInformation.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("name:"+name);
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e);
		// }
		// sess.debugPrintln(name);
		// throw new UnsupportedOperationException("Not supported yet.");

	}

	/**
	 * The operation for checking if file is read-only is not implemented yet.
	 * 
	 * @param sess Session details
	 * @param ctx Device context
	 * @return True if file is readonly, false otherwise
	 * @throws IOException
	 */
	public boolean isReadOnly(SrvSession sess, DeviceContext ctx)
		throws IOException {
		
		// try{
		// FileWriter fstream = new FileWriter("isReadOnly.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("context:"+ctx.getDeviceName());
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e);
		// }
		
		return false;
		
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Open a file (get the NetworkFile instance for a file)
	 * 
	 * @param sess Session details
	 * @param tree Tree connection
	 * @param params File open parameters
	 * @return The NetworkFile class descibing the file being opened
	 * @throws IOException
	 */
	public NetworkFile openFile(
		SrvSession sess, TreeConnection tree, FileOpenParams params)
		throws IOException {
		
		// sess.debugPrintln("openFile:"+params.getPath());
		
		String liferayFullPath = _getLiferayFullPath(params.getPath());
		
		// Check if a file exists
		
		FileInfo info = getFileInformation(sess, tree, params.getPath());
		if (info != null && info.getFileName() != null) {

			// try{
			// FileWriter fstream = new FileWriter("openFile.txt",true);
			// BufferedWriter out = new BufferedWriter(fstream);
			// out.newLine();
			// out.write("________");
			// out.newLine();
			// out.write("getLiferayFullPath:"+getLiferayFullPath(
			// params.getPath()));
			// out.newLine();
			// out.write("getPath:"+params.getPath());
			// out.newLine();
			// out.write("info:"+info.isDirectory());
			// out.close();
			// } catch (Exception e) {
			// System.err.println("Error: " + e);
			// }

			NetworkFile networkFile = new DocumentLibraryNetworkFile(info);
			networkFile.setFullName(liferayFullPath);
			
			// networkFile.setFullName(params.getPath());
			
			return networkFile;
			
			// throw new FileNotFoundException(liferayFullPath);
			
		}
		else {
			throw new UnsupportedOperationException(
				"Error: info is null-" +
				(info == null) + "params.GetPath:" + params.getPath());
		}
		// return null;
	}

	/**
	 * Read file data - currently stubbed to return zero data for files. Reading
	 * directories leads to an AccessDeniedException, as it should
	 * 
	 * @param sess Session details
	 * @param tree Tree connection
	 * @param file
	 * @param buf
	 * @param bufPos
	 * @param siz
	 * @param filePos
	 * @return Number of bytes actually read (currently 0 for files)
	 * @throws IOException
	 */
	public int readFile(
		SrvSession sess, TreeConnection tree, NetworkFile file, byte[] buf,
		int bufPos, int siz, long filePos)
		throws IOException {
		
		sess.debugPrintln("readFile:" + file.getName());
		
		// //throw new UnsupportedOperationException("Not supported yet.");
		// try{
		// FileWriter fstream = new FileWriter("readFile.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("fileName:"+file.getFullName());
		// out.newLine();
		// out.write("buf:"+buf.length);
		// out.newLine();
		// // out.write("isDirectory():"+file.isDirectory());
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e);
		// }

		if (file.isDirectory()) {
			throw new AccessDeniedException();
		}
		// file.setFullName(m_liferaySharedFolder);
		
		int rdlen = file.readFile(buf, siz, bufPos, filePos);

		// If we have reached end of file return a zero length read

		if (rdlen == -1)
			rdlen = 0;

		// Return the actual read length

		return rdlen;

		// File f = new File("read"+FileName.splitPath(file.getFullName())[1]);
		// if (!f.exists())
		// {
		// f = LiferayStorage.ReadFile(getLiferayFullPath(file.getFullName()));
		// }
		// if (f!=null)
		// {
		// FileInputStream str = new FileInputStream(f);//"read"+
		// FileName.splitPath(file.getFullName())[1]);
		// str.read(buf, bufPos, siz);
		// str.close();

		// return siz;
		// }
		// else
		// {return 0;}
		// return 0;
		// return
		// LiferayStorage.ReadFile(getLiferayFullPath(file.getFullName()),buf,
		// siz, bufPos, filePos);
		// throw new AccessDeniedException();

	}

	/**
	 * The operation for renaming files is not implemented yet.
	 * 
	 * @param sess
	 * @param tree
	 * @param oldName
	 * @param newName
	 * @throws IOException
	 */
	public void renameFile(
		SrvSession sess, TreeConnection tree, String oldName, String newName)
		throws IOException {
		
		// if (2>1){
		// sess.debugPrint("HUITA");
		// throw new AuthenticationException("file name:");}
		
		try {
			FileWriter fstream = new FileWriter("renameFile.txt", true);
			BufferedWriter out = new BufferedWriter(fstream);
			out.newLine();
			out.write("________");
			out.newLine();
			out.write("oldName:" + oldName);
			out.newLine();
			out.write("newName:" + newName);
			out.close();
		}
		catch (Exception e) {
			System.err.println("Error: " + e);
		}
		sess.debugPrintln("renameFile oldName:" + oldName);
		sess.debugPrintln("renameFile newName:" + newName);
		if (LiferayStorage.getFileInfo(
			_getLiferayFullPath(oldName)).isDirectory()) {
			
			// sess.debugPrintln("============RENAMING FOLDER===");
			
			LiferayStorage.renameFolder(
				_getLiferayFullPath(oldName), _getLiferayFullPath(newName));
		}
		else {
			
			// sess.debugPrintln("============RENAMING FILE===");
			
			LiferayStorage.renameFile(
				_getLiferayFullPath(oldName), _getLiferayFullPath(newName));
		}

		// throw new UnsupportedOperationException("Not supported yet.");
		
	}

	/**
	 * The operation for positioning in a file is not implemented yet.
	 * 
	 * @param sess
	 * @param tree
	 * @param file
	 * @param pos
	 * @param typ
	 * @return
	 * @throws IOException
	 */
	public long seekFile(
		SrvSession sess, TreeConnection tree, NetworkFile file, long pos,
		int typ)
		throws IOException {
		
		// try{
		// FileWriter fstream = new FileWriter("seekFile.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("fileName:"+file.getFullName());
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e);
		// }
		
		return 0;
		
		// throw new UnsupportedOperationException("Not supported yet.");
		
	}

	/**
	 * Initiate the search (prepare the file list) This method is called by JLAN
	 * when the SBM client initiates a file or directoty search or when it gets
	 * directory content. The SearchContext class methods will later be invoked
	 * by JLAN to retrieve each file record in the result set
	 * 
	 * @param sess Session details
	 * @param tree Tree connection
	 * @param searchPath Search path starts with a backslash and may contain a
	 *            single file/folder full name (with path) or wildcards (also
	 *            with path)
	 * @param attrib
	 * @return The search context for the initiated search
	 * @throws FileNotFoundException
	 */
	public SearchContext startSearch(
		SrvSession sess, TreeConnection tree, String searchPath, int attrib)
		throws FileNotFoundException {
		
		// sess.debugPrintln("startSearch:"+searchPath);
		// try{
		// FileWriter fstream = new FileWriter("startSearch.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("searhPath:"+getLiferayFullPath(searchPath));
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e);
		// }
		
		DocumentLibrarySearchContext searchContext =
			new DocumentLibrarySearchContext();
		searchContext.initSearch(_getLiferayFullPath(searchPath), attrib);

		return searchContext;
	}

	/**
	 * Nothing special is done when connection closed to the device. This method
	 * is called by JLAN.
	 * 
	 * @param sess Session details
	 * @param tree Tree connection
	 */
	public void treeClosed(SrvSession sess, TreeConnection tree) {
		
		// try{
		// FileWriter fstream = new FileWriter("treeClosed.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("tree:"+tree.getContext().getDeviceName());
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e);
		// }

		// sess.debugPrintln(m_liferaySharedFolder);
		
	}

	/**
	 * Nothing special is done when connection opened to the device. This method
	 * is called by JLAN.
	 * 
	 * @param sess Session details
	 * @param tree Tree connection
	 */
	public void treeOpened(SrvSession sess, TreeConnection tree) {
		
		// try{
		// FileWriter fstream = new FileWriter("treeOpened.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("tree:"+tree.getContext().getDeviceName());
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e);
		// }
		// sess.debugPrintln(m_liferaySharedFolder);
		
	}
	
	/**
	 * The operation for truncating the file to a given size is not implemented
	 * yet.
	 * 
	 * @param sess
	 * @param tree
	 * @param file
	 * @param siz
	 * @throws IOException
	 */
	public void truncateFile(
		SrvSession sess, TreeConnection tree, NetworkFile file, long siz)
		throws IOException {
		
		// try{
		// FileWriter fstream = new FileWriter("truncateFile.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("fileName:"+file.getFullName());
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e);
		// }
		// sess.debugPrintln(m_liferaySharedFolder);
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * The operation for writing data to a file is not implemented yet.
	 * 
	 * @param sess
	 * @param tree
	 * @param file
	 * @param buf
	 * @param bufoff
	 * @param siz
	 * @param fileoff
	 * @return
	 * @throws IOException
	 */
	public int writeFile(
		SrvSession sess, TreeConnection tree, NetworkFile file, byte[] buf,
		int bufoff, int siz, long fileoff)
		throws IOException {
		
		// sess.debugPrintln("writeFile" + file.getFullName());
		// try{
		// FileWriter fstream = new FileWriter("writeFile.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("fileName:"+file.getFullName());
		// out.newLine();
		// out.write("bufOff:"+bufoff);
		// out.newLine();
		// out.write("siz:"+siz);
		// out.newLine();
		// out.write("fileoff:"+fileoff);
		// out.newLine();
		// out.write("buf:"+buf.length);
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e);
		// }
		// FileOutputStream str = new FileOutputStream("write"+
		// FileName.splitPath(file.getFullName())[1],true);
		// str.write(buf,bufoff, siz);
		// str.close();
		// LiferayStorage.UpdateFile(getLiferayFullPath(file.getFullName()),
		// buf,bufoff,siz,
		// fileoff);
		
		file.writeFile(buf, siz, bufoff, fileoff);
		return siz;
		
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	private String _getLiferayFullPath(String shareRelativePath) {
		String liferayFullPath = "";
		if (shareRelativePath.startsWith("\\") || 
			shareRelativePath.equals("")) {
			
			liferayFullPath = _m_liferaySharedFolder + shareRelativePath;
		}
		else {
			liferayFullPath = shareRelativePath;
		}
		if (liferayFullPath.startsWith("\\")) {
			liferayFullPath = liferayFullPath.substring(1);
		}
		return liferayFullPath;
	}

	/**
	 * Liferay shared folder name
	 */
	private String _m_liferaySharedFolder;
	
}
