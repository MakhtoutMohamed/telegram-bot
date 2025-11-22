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

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    // charger docs
    @Bean
    public CommandLineRunner loadHrDocuments(VectorStore vectorStore) {
        return args -> {
            var resource = new ClassPathResource("docs/rh.pdf");

            // pdf
            DocumentReader pdfReader = new PagePdfDocumentReader(resource);
            List<Document> documents = pdfReader.read();

            // mini chunks
            TextSplitter splitter = new TokenTextSplitter();
            List<Document> splitDocs = splitter.split(documents);

            // ajouter dans vector store
            vectorStore.add(splitDocs);

            System.out.println("RAG: " + splitDocs.size() + " chunks rh chargés dans le VectorStore");
        };
    }
}