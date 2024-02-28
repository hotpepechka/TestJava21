package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.*;

public class CrptApi {

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Semaphore semaphore;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.semaphore = new Semaphore(requestLimit);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::releaseSemaphore, 0, timeUnit.toSeconds(1), TimeUnit.SECONDS);
    }

    public void createDocument(Document document, String signature) {
        try {
            semaphore.acquire();
            String apiUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.putPOJO("description", document);
            requestBody.put("signature", signature);

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .post(RequestBody.create(MediaType.parse("application/json"), requestBody.toString()))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("Failed to create document: " + response.code() + " " + response.message());
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            semaphore.release();
        }
    }

    private void releaseSemaphore() {
        semaphore.release(semaphore.availablePermits());
    }

    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 10);

        Document document = new Document();
        document.setParticipantInn("string");

        String signature = "sample_signature";

        crptApi.createDocument(document, signature);
    }

    static class Document {
        private String participantInn;

        public String getParticipantInn() {
            return participantInn;
        }

        public void setParticipantInn(String participantInn) {
            this.participantInn = participantInn;
        }
    }
}
