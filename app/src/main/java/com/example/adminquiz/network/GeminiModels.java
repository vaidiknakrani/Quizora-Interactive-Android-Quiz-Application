package com.example.adminquiz.network;

import java.util.ArrayList;
import java.util.List;

/** Request/response bodies for the Gemini generateContent endpoint. */
public final class GeminiModels {
    private GeminiModels() {
    }

    public static class Part {
        public String text;

        public Part() {
        }

        public Part(String text) {
            this.text = text;
        }
    }

    public static class Content {
        public String role;
        public List<Part> parts = new ArrayList<>();
    }

    public static class GenerationConfig {
        public String responseMimeType = "application/json";
        public double temperature = 0.7;
    }

    public static class GeminiRequest {
        public List<Content> contents = new ArrayList<>();
        public GenerationConfig generationConfig = new GenerationConfig();
    }

    public static class Candidate {
        public Content content;
        public String finishReason;
    }

    public static class GeminiResponse {
        public List<Candidate> candidates;
    }
}
