package com.vogdo.telegrambot.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

@Configuration
public class RagConfig {

    // Vector store en mémoire (lié au modèle d'embedding OpenAI)
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    // Chargement des documents RH au démarrage de l'appli
    @Bean
    public CommandLineRunner loadHrDocuments(VectorStore vectorStore) {
        return args -> {
            var resource = new ClassPathResource("docs/rh.pdf");

            // 1. Lire le PDF
            DocumentReader pdfReader = new PagePdfDocumentReader(resource);
            List<Document> documents = pdfReader.read();

            // 2. Découper en petits chunks (pour de meilleurs embeddings)
            TextSplitter splitter = new TokenTextSplitter();
            List<Document> splitDocs = splitter.split(documents);

            // 3. Stocker dans le vector store
            vectorStore.add(splitDocs);

            System.out.println("✅ RAG: " + splitDocs.size() + " chunks RH chargés dans le VectorStore.");
        };
    }
}