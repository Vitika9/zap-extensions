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


public class PostmanParser {
    public static void importFromFile(final File file) {
        try {
            String defn = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            importDefinition(defn);
        }
        catch (Exception e) {
            System.out.println("Exception");
        }
    }

    public static void importDefinition(String defn) {
        ObjectNode jsonNode = parse(defn);
        
        List<HttpMessage> list = mapJsonNodeToHttpMessages(jsonNode);

        // ObjectNode node = (ObjectNode) jsonNode;
    
        // JsonNode value = node.get("item");
		// ArrayNode arrayNode = null;
		// if (value == null) {
		// 	System.out.println("NOTT FOUND");
		// } else if (!value.getNodeType().equals(JsonNodeType.ARRAY)) {
		// 	System.out.println("NOT OF ARRAY TYPE");
		// } else {
		// 	arrayNode = (ArrayNode) value;
		// }
		
        // if (arrayNode != null && arrayNode.size() > 0) {
        //     for (int i = 0; i < arrayNode.size(); i++) {
        //         System.out.println("For " + i + " Value: " + arrayNode.get(i));
        //     }
        // }

    }

    public static ObjectNode parse(String defn) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode defnNode = mapper.readTree(defn);
            return (ObjectNode) defnNode;
        } catch (IOException e) {
            System.out.println("Exception");
        }
        return null;
    }

    public static List<HttpMessage> mapJsonNodeToHttpMessages(JsonNode node) {
        List<HttpMessage> httpMessages = new ArrayList<>();
       
        try{
            ArrayNode item = (ArrayNode) node.get("item");
            for (int i = 0; i < item.size(); i++) {
                extractHttpMessages(item.get(i), httpMessages);
            }
        }
        catch (Exception e) {
            System.out.println("Exception1 " + e.message);
        }
        
        return httpMessages;
    }

    public static void extractHttpMessages(JsonNode item, List<HttpMessage> list) {
        ObjectNode itemObject = (ObjectNode) item;

        if (itemObject.has("item")) {
            // is an item-group
            extractHttpMessages(itemObject.get("item"), list);
        }
        else {
            // is an item
            // TODO: extract httpmessages
            System.out.println("NAME: " + item.get("name"));
        }

    }
}

