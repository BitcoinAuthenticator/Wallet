package wallettemplate.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileUtils {
	public static class ZipHelper  
	{
	    public static  boolean zipDir(String dirName, String nameZipFile) {
	    	try {
	    		ZipOutputStream zip = null;
		        FileOutputStream fW = null;
		        fW = new FileOutputStream(nameZipFile);
		        zip = new ZipOutputStream(fW);
		        addFolderToZip("", dirName, zip);
		        zip.close();
		        fW.close();
		        return true;
	    	}
	    	catch(IOException e) {
	    		e.printStackTrace();
	    		return false;
	    	}
	    }
	    
	    static public boolean unZip(String zipFile, String outputFolder) {
	        byte[] buffer = new byte[1024];
	        try{
	    
		       	//create output directory is not exists
		       	File folder = new File(outputFolder);
		       	if(!folder.exists()){
		       		folder.mkdir();
		       	}
		    
		       	//get the zip file content
		       	ZipInputStream zis = 
		       		new ZipInputStream(new FileInputStream(zipFile));
		       	//get the zipped file list entry
		       	ZipEntry ze = zis.getNextEntry();
		    
		       	while(ze!=null){
		    
		       	   String fileName = ze.getName();
		              File newFile = new File(outputFolder + File.separator + fileName);
		    		    
		               //create all non exists folders
		               //else you will hit FileNotFoundException for compressed folder
		               new File(newFile.getParent()).mkdirs();
		               
		               if(newFile.isDirectory()) {
		            	   newFile.mkdirs();
		            	   ze = zis.getNextEntry();
		            	   continue;
		               }
		    
		               FileOutputStream fos = new FileOutputStream(newFile);             
		    
		               int len;
		               while ((len = zis.read(buffer)) > 0) {
		          		fos.write(buffer, 0, len);
		               }
		    
		               fos.close();   
		               ze = zis.getNextEntry();
		       	}
		    
		        zis.closeEntry();
		       	zis.close();
		       	return true;
		       }catch(IOException ex){
		          ex.printStackTrace(); 
		          return false;
		       }
	    }  

	    private static void addFolderToZip(String path, String srcFolder, ZipOutputStream zip) throws IOException {
	        File folder = new File(srcFolder);
	        if (folder.list().length == 0) {
	            addFileToZip(path , srcFolder, zip, true);
	        }
	        else {
	            for (String fileName : folder.list()) {
	                if (path.equals("")) {
	                    addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip, false);
	                } 
	                else {
	                     addFileToZip(path + "/" + folder.getName(), srcFolder + "/" + fileName, zip, false);
	                }
	            }
	        }
	    }

	    private static void addFileToZip(String path, String srcFile, ZipOutputStream zip, boolean flag) throws IOException {
	        File folder = new File(srcFile);
	        if (flag) {
	            zip.putNextEntry(new ZipEntry(path + "/" +folder.getName() + "/"));
	        }
	        else {
	            if (folder.isDirectory()) {
	                addFolderToZip(path, srcFile, zip);
	            }
	            else {
	                byte[] buf = new byte[1024];
	                int len;
	                FileInputStream in = new FileInputStream(srcFile);
	                zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
	                while ((len = in.read(buf)) > 0) {
	                    zip.write(buf, 0, len);
	                }
	            }
	        }
	    }
	}
	
	/**
	 * Returns true if directory was deleted/ does not exist
	 * @param directory
	 * @return
	 */
	public static boolean deleteDirectory(File directory) {
	    if(directory.exists()){
	        File[] files = directory.listFiles();
	        if(null!=files){
	            for(int i=0; i<files.length; i++) {
	                if(files[i].isDirectory()) {
	                    deleteDirectory(files[i]);
	                }
	                else {
	                    files[i].delete();
	                }
	            }
	        }
	        return(directory.delete());
	    }
	   return true;
	}

}
