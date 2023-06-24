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

import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.network.HttpSender;

import java.util.ArrayList;
import java.util.List;
import org.zaproxy.zap.network.HttpRedirectionValidator;
import org.zaproxy.zap.network.HttpRequestConfig;
import org.apache.commons.httpclient.URI;

public class Requestor {
    private int initiator;
    private List<HistoryPersister> listeners = new ArrayList<>();
    private HttpSender sender;
    private final HttpRequestConfig requestConfig;

    public Requestor(int initiator) {
        this.initiator = initiator;
        this.sender = new HttpSender(initiator);
        this.requestConfig =
                HttpRequestConfig.builder().setRedirectionValidator(new MessageHandler()).build();
    }

    public void run(List<HttpMessage> httpMessages) {
        for (HttpMessage httpMessage: httpMessages) {
            try {
                if (httpMessage.getRequestHeader().getURI() == null) {
                    System.out.println("URII ISS NOOT PRESENT here in req");
                }
                else {
                    System.out.println("URII ISS HAII PRESENT here in req");
                }
                sender.sendAndReceive(httpMessage, requestConfig);
            }
            catch (Exception e) {
                System.out.println("Exception " + e);
            }
        }
    }

    public void addListener(HistoryPersister listener) {
        this.listeners.add(listener);
    }

    public void removeListener(HistoryPersister listener) {
        this.listeners.remove(listener);
    }

    private class MessageHandler implements HttpRedirectionValidator {

        @Override
        public void notifyMessageReceived(HttpMessage message) {
            if (message.getRequestHeader().getURI() == null) {
                System.out.println("URII ISS NOOT PRESENT here in handler");
            }
            else {
                System.out.println("URII ISS HAII PRESENT here in handler");
            }
            for (HistoryPersister listener : listeners) {
                try {
                    listener.handleMessage(message, initiator);
                } catch (Exception e) {
                    System.out.println("Exception " + e);
                }
            }
        }

        @Override
        public boolean isValid(URI redirection) {
            return true;
        }
    }
}
