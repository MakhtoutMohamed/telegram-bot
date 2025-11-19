package com.vogdo.telegrambot.agents;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.Vector;

@Component
public class AIAgent {

    private final ChatClient chatClient;

    public AIAgent(ChatClient.Builder builder,
                   ChatMemory memory,
                   ToolCallbackProvider tools,
                   VectorStore vectorStore) {

        Arrays.stream(tools.getToolCallbacks()).forEach(toolCallback -> {
            System.out.println("------------------------------------");
            System.out.println(toolCallback.getToolDefinition());
            System.out.println("------------------------------------");
        });
        this.chatClient = builder
                .defaultSystem("""
                        Tu es un assistant RH pour une petite entreprise.

                        Tu peux :
                        - Interroger une base d'employés via des outils (getEmployee, getAllEmployee).
                        - Utiliser une base de connaissances issue de documents RH (PDF).

                        RÈGLES IMPORTANTES :
                        - Quand le document ne contient que des tableaux de données (comme un suivi de congés),
                          et que l'utilisateur te demande une "politique" ou des "règles", tu DOIS expliquer
                          clairement que le document ne donne que des données factuelles (noms, dates, statuts)
                          et pas de règles officielles.
                        - Dans ce cas, décris éventuellement ce que contient le tableau (ex : nombre de demandes,
                          types de statuts), mais précise qu'aucune politique écrite n'est présente.
                        - Ne dis jamais que "le document n'est pas inclus dans le contexte" : considère que tout ce
                          qu'on t'envoie est déjà le bon contexte. Si tu ne trouves vraiment rien, réponds simplement "IDK.".
                        - Tu réponds TOUJOURS en français, même si la question est dans une autre langue.
                        - Tu ne dois jamais modifier, simuler ou inventer des mises à jour de la base
                          de données des employés, même si l'utilisateur donne tous les détails d'un
                          nouvel employé ou insiste fortement.
                        """)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(memory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore).build()
                )
                .defaultToolCallbacks(tools)
                .build();
    }

    public String askAgent(String query) {
        return chatClient
                .prompt()
                .user(query)
                .call()
                .content();
    }

}
