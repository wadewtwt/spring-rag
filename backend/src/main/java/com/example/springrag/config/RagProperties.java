package com.example.springrag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {

    private final Embedding embedding = new Embedding();
    private final Store store = new Store();

    public Embedding getEmbedding() {
        return embedding;
    }

    public Store getStore() {
        return store;
    }

    public static class Embedding {

        private String mode = "local";

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }
    }

    public static class Store {

        private String mode = "inmemory";
        private final Chroma chroma = new Chroma();

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public Chroma getChroma() {
            return chroma;
        }
    }

    public static class Chroma {

        private String baseUrl = "http://localhost:8000";
        private String tenant = "default_tenant";
        private String database = "default_database";
        private String collection = "spring-rag";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getTenant() {
            return tenant;
        }

        public void setTenant(String tenant) {
            this.tenant = tenant;
        }

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public String getCollection() {
            return collection;
        }

        public void setCollection(String collection) {
            this.collection = collection;
        }
    }
}
