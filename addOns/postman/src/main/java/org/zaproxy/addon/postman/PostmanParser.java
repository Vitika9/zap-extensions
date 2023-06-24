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


public class PostmanParser {
    public static void importFromFile(final File file) throws Exception {
        
        String defn = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        importDefinition(defn);
        
        
    }

    public static void importDefinition(String defn) throws Exception {
        ObjectNode jsonNode = parse(defn);
        
        List<HttpMessage> list = mapJsonNodeToHttpMessages(jsonNode);

        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i).getNote());
        }

        Requestor requestor = new Requestor(HttpSender.MANUAL_REQUEST_INITIATOR);
        requestor.addListener(new HistoryPersister());

        requestor.run(list);
    }

    public static ObjectNode parse(String defn) throws Exception{
        ObjectMapper mapper = new ObjectMapper();

        JsonNode defnNode = mapper.readTree(defn);
        return (ObjectNode) defnNode;
        
    }

    public static List<HttpMessage> mapJsonNodeToHttpMessages(JsonNode node) {
        List<HttpMessage> httpMessages = new ArrayList<>();
        
        if (node.has("item")) {
            ArrayNode item = (ArrayNode) node.get("item");

            try {
                for (int i = 0; i < item.size(); i++) {
                    extractHttpMessages(item.get(i), httpMessages);
                }
            }
            catch (Exception e) {
                System.out.println("Exception "+ e);
            }
        }
        
        
        return httpMessages;
    }

    public static void extractHttpMessages(JsonNode item, List<HttpMessage> list) {
        ObjectNode itemObject = (ObjectNode) item;

        if (itemObject.has("item")) {
            // is an item-group
            ArrayNode array = (ArrayNode) itemObject.get("item");
            
            for (int i = 0; i < array.size(); i++) {
                extractHttpMessages(array.get(i), list);
            }
        }
        else if (itemObject.has("request")){
            // is an item
            // TODO: extract httpmessages
            ObjectNode request = (ObjectNode) itemObject.get("request");

            try {
                
                System.out.println("URLLLLLLL IS: " + request.get("url").get("raw").textValue());
                HttpMessage httpMessage = new HttpMessage();
                httpMessage.getRequestHeader().setURI(new URI(request.get("url").get("raw").textValue(), false));

                System.out.println(httpMessage.getRequestHeader().getURI());
                System.out.println("METHOD IS: " + request.get("method").textValue());
                httpMessage.getRequestHeader().setMethod(request.get("method").textValue());

                for (JsonNode header : (ArrayNode) request.get("header")) {
                    System.out.println("KEY IS: " + header.get("key").textValue());
                    System.out.println("VALUE IS: " + header.get("value").textValue());
                    httpMessage.getRequestHeader().setHeader(header.get("key").textValue(), header.get("value").textValue());
                }

                if (request.has("body")) {
                    httpMessage.getRequestBody().setBody(request.get("body").toString());
                    httpMessage.getRequestHeader().setContentLength(request.get("body").toString().length());
                }
            
                System.out.println("NAME IS: " + itemObject.get("name").textValue());
                httpMessage.setNote(itemObject.get("name").textValue());
                

                list.add(httpMessage);
            }
            catch (Exception e) {
                System.out.println("Exception " + e);
            }
            
        }

    }
}

