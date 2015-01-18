package org.authenticator.utilsTest;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.authenticator.Utils.CryptoUtils;
import org.authenticator.Utils.FileUtils;
import org.authenticator.Utils.CryptoUtils.CannotDecryptMessageException;
import org.authenticator.Utils.CryptoUtils.CouldNotEncryptPayload;
import org.junit.Test;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Hex;

public class FileUtilsTest {
	
	String contentTxtFile1 = "Decentralized two-factor authentication built right into your wallet" + 
							 "Introducing the first Bitcoin wallet to offer a practical decentralized wallet security solution. Forget about third party services that require you to sacrifice privacy for security. Now you can use Bitcoin the way it was intended ― keep your own keys and enjoy the benefits of a private, secure, censorship-resistant digital currency.";
	String contentTxtFile2 = "The next generation of bitcoin wallet software is here." + 
						     "The Authenticator wallet is a feature-rich, privacy and security-centric wallet. The goal is to pack cutting edge features into a package that looks good and is intuitive and easy to use. You shouldn’t need an in-depth knowledge cryptography or computer security to use Bitcoin safely. We believe wallets should handle everything for you, automatically and without additional effort on your part.";
	String contentTxtFile3 = "The security of multisig without a centralized server.";
	String contentTxtFile4 = "It isn’t very difficult for a hacker to steal your bitcoins. A simple key logger is usually enough to compromise your wallet." + 
							 "Bitcoin Authenticator is a decentralized 2-factor authentication solution for Android mobile devices. When activated, it makes it substantially harder for an attacker to steal your bitcoins." +
							 "When paired with Bitcoin Authenticator, your wallet will generate multi-signature addresses which require two keys to sign a transaction. One set of keys is kept in the wallet, the other on your mobile device. When you make a transaction, your mobile device will prompt you for authorization. If one of your devices is compromised, your bitcoins are still safe.";

	File tmpDir 		= null;
	File unzipResult 	= null;
	File destZip 		= null;
	File txt1 			= null;
	File txt2 			= null;
	File txt3 			= null;
	File txt4 			= null;
	
	@Test
	public void zipAndUnzipTest() {
		
		try {
			prepareTempFolder();
			
			unzipResult = new File("unzipResult");
			
			destZip = new File("zipTest.zip");
			
			boolean result = FileUtils.ZipHelper.zipDir(tmpDir.getAbsolutePath(), destZip.getAbsolutePath());
			if(result == false)
				assertTrue(false);
			
			result = FileUtils.ZipHelper.unZip(destZip.getAbsolutePath(), unzipResult.getAbsolutePath());
			if(result == false)
				assertTrue(false);
			
			File[] unzippedFiles = new File(unzipResult.getAbsolutePath() + "/tmp").listFiles();
			boolean txt1Found = false;
			boolean txt2Found = false;
			boolean txt3Found = false;
			boolean txt4Found = false;
			for(int i=0; i<unzippedFiles.length; i++) {
				try {
					String txtResult = readAllTextFromFile(unzippedFiles[i].getCanonicalPath()).replace(System.getProperty("line.separator"), "");;
					if(unzippedFiles[i].getName().equals(txt1.getName())) {
						assertTrue(txtResult.equals(contentTxtFile1));
						txt1Found = true;
					}
					else if(unzippedFiles[i].getName().equals(txt2.getName())) {
						assertTrue(txtResult.equals(contentTxtFile2));
						txt2Found = true;
					}
					else if(unzippedFiles[i].getName().equals(txt3.getName())) {
						assertTrue(txtResult.equals(contentTxtFile3));
						txt3Found = true;
					}
					else if(unzippedFiles[i].getName().equals(txt4.getName())) {
						assertTrue(txtResult.equals(contentTxtFile4));
						txt4Found = true;
					}
				} catch (IOException e) {
					e.printStackTrace();
					assertTrue(false);
				}
			}
			
			if(txt1Found == false || txt2Found == false || txt3Found == false || txt4Found == false)
				assertTrue("Could not find all zipped files", false);
		}
		catch(Exception e) {
			
		}
		finally {
			// cleanup
			if(tmpDir != null)
				FileUtils.deleteDirectory(tmpDir);
			if(destZip != null)
				destZip.delete();
			if(unzipResult != null)
				FileUtils.deleteDirectory(unzipResult);
		}		
	}
	
	@Test
	public void deleteFolderTest() {
		prepareTempFolder();
		FileUtils.deleteDirectory(tmpDir);
		
		File tmp = new File("tmp");
		assertTrue(tmp.exists() == false);
	}
	
	private void prepareTempFolder() {
		tmpDir = new File("tmp");
		tmpDir.mkdir();
		
		txt1 = new File(tmpDir, "txt1.txt");
		writeTextToFile(txt1, contentTxtFile1);
		
		txt2 = new File(tmpDir, "txt2.txt");
		writeTextToFile(txt2, contentTxtFile2);
		
		txt3 = new File(tmpDir, "txt3.txt");
		writeTextToFile(txt3, contentTxtFile3);
		
		txt4 = new File(tmpDir, "txt4.txt");
		writeTextToFile(txt4, contentTxtFile4);
	}
	
	private void writeTextToFile(File file, String content) {
		try {
			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}
 
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content);
			bw.close();
 
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	private String readAllTextFromFile(String filePath) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filePath));
	    try {
	        StringBuilder sb = new StringBuilder();
	        String line = br.readLine();

	        while (line != null) {
	            sb.append(line);
	            sb.append(System.lineSeparator());
	            line = br.readLine();
	        }
	        String everything = sb.toString();
	        return everything;
	    } finally {
	        br.close();
	    }
	}
}
