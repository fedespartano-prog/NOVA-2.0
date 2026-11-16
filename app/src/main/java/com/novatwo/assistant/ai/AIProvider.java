
package com.novatwo.assistant.ai;

public interface AIProvider {

    String getProviderName();

    void generateResponse(
            String prompt,
            ResponseCallback callback
    );

    interface ResponseCallback {

        void onSuccess(String response);

        void onError(String error);
    }
}
