package com.developersunit.spotit_glasses_hackhub25;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DictionaryAPIHelper {
    private static final String API_KEY = "0bc54eebea9002065db999261ba3721d580cd36c9083d93dbc84a82f35e8bf16";

    public interface Callback {
        void onResult(String result);
    }

    public static void fetchMeaning(String word, Callback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String result = fetchDataFromAPI(word);
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(result));
        });
    }

    private static String fetchDataFromAPI(String word) {
        try {
            // Build the query string: "What's the definition of <word>"
            String query = "What's the definition of " + word;
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            String apiUrl = "https://serpapi.com/search.json?q=" + encodedQuery + "&hl=en&gl=us&api_key=" + API_KEY;

            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return "Error: Unable to fetch data (HTTP " + responseCode + ")";
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Debug: Print the API response in Logcat
            System.out.println("API Response: " + response.toString());

            // Parse JSON response
            JSONObject jsonResponse = new JSONObject(response.toString());

            // Try to extract definition from the answer_box if available.
            if (jsonResponse.has("answer_box")) {
                JSONObject answerBox = jsonResponse.getJSONObject("answer_box");
                if (answerBox.has("answer")) {
                    return answerBox.getString("answer");
                } else if (answerBox.has("snippet")) {
                    return answerBox.getString("snippet");
                }
            }

            // Fallback: Check in organic_results if available.
            if (jsonResponse.has("organic_results")) {
                JSONArray organicResults = jsonResponse.getJSONArray("organic_results");
                if (organicResults.length() > 0) {
                    JSONObject firstResult = organicResults.getJSONObject(0);
                    if (firstResult.has("snippet")) {
                        return firstResult.getString("snippet");
                    }
                }
            }

            return "Meaning not found";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error fetching meaning";
        }
    }
}