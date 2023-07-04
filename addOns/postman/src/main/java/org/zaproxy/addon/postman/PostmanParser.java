/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2023 The ZAP Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.addon.postman;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.parosproxy.paros.network.HttpMessage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.httpclient.URI;
import org.parosproxy.paros.network.HttpSender;
import org.parosproxy.paros.Constant;
import java.util.HashSet;

public class PostmanParser {
    private static final HashSet<String> VALID_METHODS = new HashSet<>();
    
    static {
        VALID_METHODS.add("get");
        VALID_METHODS.add("post");
        VALID_METHODS.add("put");
        VALID_METHODS.add("delete");
        VALID_METHODS.add("patch");
        VALID_METHODS.add("head");
        VALID_METHODS.add("options");
    }

    private List<String> errors;

    public PostmanParser() {
        errors = new ArrayList<String>();
    }


    public static boolean isValidMethod(final String method) {
        String lowercaseMethod = method.toLowerCase();
        return VALID_METHODS.contains(lowercaseMethod);
    }

    public void importFromFile(final String filePath) throws IOException, Exception {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException(
                    Constant.messages.getString("postman.error.filenotfound", filePath));
        }
        if (!file.canRead() || !file.isFile()) {
            throw new IOException(Constant.messages.getString("postman.error.importfile"));
        }

        String defn = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        importDefinition(defn);
    }

    public void importDefinition(String defn) throws Exception {
        List<HttpMessage> list = parse(defn);

        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i).getNote());
        }
    }

    public List<HttpMessage> parse(String defn) throws IOException {
        List<HttpMessage> httpMessages = new ArrayList<HttpMessage>();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = null;
        
        try {
            jsonNode = objectMapper.readTree(defn);
        }
        catch (Exception e) {
            throw new IOException("Json is invalid");
        }
        

        extractHttpMessages(jsonNode, httpMessages);
        
        return httpMessages;
    }

    public void extractHttpMessages(JsonNode node, List<HttpMessage> list) {
        if (node.has("item")) {
            JsonNode item = node.get("item");
            if (item.getNodeType().equals(JsonNodeType.ARRAY)) {
                ArrayNode itemArray = (ArrayNode) item;
                for (JsonNode element : itemArray) {
                    extractHttpMessages(element, list);
                }
            } else if (item.getNodeType().equals(JsonNodeType.OBJECT)) {
                extractHttpMessages(node.get("item"), list);
            }
        } 
        if (node.has("request")) {
            JsonNode request = node.get("request");
            try {
                HttpMessage httpMessage = createHttpMessage(request);
                String note = node.get("name").textValue();
                httpMessage.setNote(note);
                list.add(httpMessage);
            } catch (Exception e) {
                System.err.println("Exception: " + e);
            }
        } else{
            System.out.println("An item Element of a Postman definition should either be an item or an item-group.");
        }
    }
    
    private HttpMessage createHttpMessage(JsonNode request) throws Exception{
        HttpMessage httpMessage = new HttpMessage();
    
        if (request.has("url")) {
            httpMessage.getRequestHeader().setURI(new URI(request.get("url").get("raw").textValue(), false));
        }

        if (request.has("method")) {
            final String method = request.get("method").textValue();
            if (isValidMethod(method)) {
                httpMessage.getRequestHeader().setMethod(request.get("method").textValue());
            } else {
                this.errors.add(Constant.messages.getString("postman.error.invalidmethod"));
            }
        } else {
            httpMessage.getRequestHeader().setMethod("GET");
        }

        if (request.has("header")) {
            ArrayNode headers = (ArrayNode) request.get("header");
            for (JsonNode header : headers) {
                if (header.has("key") && header.has("value")) {
                    String key = header.get("key").textValue();
                    String value = header.get("value").textValue();
                    httpMessage.getRequestHeader().setHeader(key, value);
                } else {
                    System.out.println("Header of the request must have key and value");
                }
                
            }
        }

        if (request.has("body")) {
            String body = request.get("body").toString();
            httpMessage.getRequestBody().setBody(body);
            httpMessage.getRequestHeader().setContentLength(body.length());
        }

        return httpMessage;
    }
    
}

