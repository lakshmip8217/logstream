package com.example.demo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FetchLogFileController {

	@Value("${logs.folder.path}")
	private String logsFolderPath;


	@Value("${log.filename}")
	private String logFilename;

	@Value("${remote.server.name}")
	private String serverName;
		
	@Value("${remote.server.port}")
	private String serverPort;
	
	@GetMapping("/")
	public String index() {
		return "Greetings from Spring Boot!";
	}
	
	@GetMapping("/log")
    public ResponseEntity<?> fetchCurrentLogFile(@RequestParam(required = false) String fileName) {
        String logFolderPath = "logs/";
        String currentLogFilePath = logFolderPath + fileName;
        
        if(isEmptyOrNull(fileName)){
            return fetchFolderContents(logFolderPath);
        }else{
            return fetchFile(currentLogFilePath);
        }
    }

	/**
	 * This method is used to read contents from log file name given as input parameter to rest api and 
	 * write the contents to the same file name in local disk path
	 * @param filePath
	 * @return
	 */
	private ResponseEntity<?> fetchFile(String filePath) {
       
        byte[] buffer = new byte[8 * 1024];
        try {
        	String command =
        			  "curl -X GET "+filePath;
        	ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
        	processBuilder.directory(new File("/var/log/"));
        	Process process = processBuilder.start();
        	InputStream inputStream = process.getInputStream();
			
        	File targetFile = new File("/logs/"+logFilename);
        	OutputStream outStream = new FileOutputStream(targetFile);
        	
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(outStream);
        } catch (Exception e) {
            return ResponseEntity.ok().body(e.getMessage());
        }

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(buffer);
    }

	/**
	 * This method is used to read contents from log files folder configured in the property file and 
	 * considered if file name is empty in rest api call 
	 * write the contents reading from log files folder to local disk path
	 * @param filePath
	 * @return
	 */
    private ResponseEntity<?> fetchFolderContents(String folderPath) {
        List<String> folderContents;

        try {
            Path path = Paths.get(folderPath);
            File folder = path.toFile();
            folderContents = Arrays.stream(Objects.requireNonNull(folder.list()))
                    .filter(contents -> contents.endsWith(".log"))
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            return ResponseEntity.ok().body(e.getMessage());
        }

        return ResponseEntity.ok().body(folderContents);
    }

    private boolean isEmptyOrNull(String str) {
        return str == null || "".equals(str.trim());
    }

}
