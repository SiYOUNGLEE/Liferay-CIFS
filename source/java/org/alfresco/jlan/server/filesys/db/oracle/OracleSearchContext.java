/*
 * Copyright (C) 2006-2008 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */

package org.alfresco.jlan.server.filesys.db.oracle;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import org.alfresco.jlan.debug.Debug;
import org.alfresco.jlan.server.filesys.FileAttribute;
import org.alfresco.jlan.server.filesys.FileInfo;
import org.alfresco.jlan.server.filesys.db.DBSearchContext;
import org.alfresco.jlan.util.WildCard;

/**
 * Oracle Database Search Context Class
 *
 * @author gkspencer
 */
public class OracleSearchContext extends DBSearchContext {

  /**
	 * Class constructor
	 * 
	 * @param rs ResultSet
	 * @param stmt Statement
	 * @param filter WildCard
   */
  protected OracleSearchContext(ResultSet rs, Statement stmt, WildCard filter) {
    super(rs, stmt, filter);
  }
  
  /**
   * Return the next file from the search, or return false if there are no more files
   * 
   * @param info FileInfo
   * @return boolean
   */
  public boolean nextFileInfo(FileInfo info) {
    
    //	Get the next file from the search
    
    try {
    	
    	//	Return the next file details or loop until a match is found if a complex wildcard filter
    	//	has been specified
    	
      while ( m_rs.next()) {
        
        //	Get the file name for the next file
        
        info.setFileId(m_rs.getInt("FileId"));
        info.setFileName(m_rs.getString("FileName"));
        info.setSize(m_rs.getLong("FileSize"));

        Timestamp createDate = m_rs.getTimestamp("CreateDate");
        if ( createDate != null)
        	info.setCreationDateTime(createDate.getTime());
        else
        	info.setCreationDateTime(System.currentTimeMillis());
        	
        Timestamp modifyDate = m_rs.getTimestamp("ModifyDate");
        if ( modifyDate != null)
        	info.setModifyDateTime(modifyDate.getTime());
        else
        	info.setModifyDateTime(System.currentTimeMillis());
        	
				Timestamp accessDate = m_rs.getTimestamp("AccessDate");
				if ( accessDate != null)
					info.setAccessDateTime(accessDate.getTime());
        	
        //	Build the file attributes flags
        
        int attr = 0;
        
        if ( m_rs.getBoolean("ReadOnlyFile") == true)
        	attr += FileAttribute.ReadOnly;
        	
        if ( m_rs.getBoolean("SystemFile") == true)
        	attr += FileAttribute.System;
        	
        if ( m_rs.getBoolean("HiddenFile") == true)
        	attr += FileAttribute.Hidden;
        	
        if ( m_rs.getBoolean("DirectoryFile") == true)
        	attr += FileAttribute.Directory;

				if ( m_rs.getBoolean("ArchivedFile") == true)
					attr += FileAttribute.Archive;
        	
        info.setFileAttributes(attr);
        
				//	Get the group/owner id
	    
				info.setGid(m_rs.getInt("OwnerGid"));
				info.setUid(m_rs.getInt("OwnerUid"));
	    
				info.setMode(m_rs.getInt("FileMode"));

        //	Check if there is a complex wildcard filter
        
        if ( m_filter == null || m_filter.matchesPattern(info.getFileName()) == true)
        	return true;
      }
    }
    catch (SQLException ex) {
      Debug.println(ex);
    }
    
		//	No more files
		
    closeSearch();
    return false;
  }

  /**
   * Return the file name of the next file in the active search. Returns
   * null if the search is complete.
   *
   * @return String
   */
  public String nextFileName() {

    //	Get the next file from the search
    
    try {

			//	Return the next file details or loop until a match is found if a complex wildcard filter
			//	has been specified

			String fileName = null;
			    	
      while ( m_rs.next()) {
        
        //	Get the file name for the next file
        
        fileName = m_rs.getString("FileName");
        
				//	Check if there is a complex wildcard filter
		        
				if ( m_filter == null || m_filter.matchesPattern(fileName) == true)
					return fileName;
      }
    }
    catch (SQLException ex) {
      Debug.println(ex);
    }
    
    //	No more files

    return null;
  }
}
