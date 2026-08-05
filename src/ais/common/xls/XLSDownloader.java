/*
 * XLSDownloader.java
 *
 * Created on September 17, 2007, 11:08 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package ais.common.xls;


import java.io.File;

import org.zkoss.zul.Filedownload;

/**
 *
 * @author M. Fauzi Murtadlo
 */
public class XLSDownloader {
    
    /** Creates a new instance of XLSDownloader */
    public XLSDownloader() {
    }
    
    
    
    
    
    public synchronized static void download(File file) throws Exception { 
        Filedownload.save(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }
    
   
    
    
}
