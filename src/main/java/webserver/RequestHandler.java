package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.User;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
        	
        	BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        	String line = br.readLine();
        	log.debug("request line : {}", line);
        	
        	if(line == null) {
        		return;
        	}
        	
        	String[] tokens = line.split(" ");
        	log.debug("tokens : {} / {} / {}",tokens[0], tokens[1], tokens[2]);
        	//String url = tokens[1];
        	int index = tokens[1].indexOf("?");
        	if(index > -1) {
	        	String requestPath = tokens[1].substring(0, index);
	        	String queryString = tokens[1].substring(index+1);
	        	Map<String, String> params = HttpRequestUtils.parseQueryString(queryString);
	        	log.debug("params = {}", params);
	        	User user = new User(params.get("userId"), params.get("password"), params.get("name"), params.get("email"));
	        	log.debug("user = {}",user);
        	}
        	
        	int contentLength = 0;
        	while (!"".equals(line)) {
        		line = br.readLine();
        		
        		if(line.startsWith("Content-Length")) {
        			String[] values =  line.split(":");
        			contentLength = Integer.parseInt(values[1].trim());
        		}
        		log.debug("request line : {}",line);
        	}
        	
        	String path = tokens[1];
        	if (tokens[0].equals("POST")) {
        		String queryString = IOUtils.readData(br, contentLength);
        		log.debug("POST_QueryString = {}", queryString);
	        	
        		Map<String, String> params = HttpRequestUtils.parseQueryString(queryString);
	        	log.debug("params = {}", params);
	        	User user = new User(params.get("userId"), params.get("password"), params.get("name"), params.get("email"));
	        	log.debug("user = {}",user);
	        	path = "/index.html";
        	}
        	
            DataOutputStream dos = new DataOutputStream(out);
            byte[] body = Files.readAllBytes(new File("./webapp"+path).toPath());
            //byte[] body = "Hello World".getBytes();
            response200Header(dos, body.length);
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
