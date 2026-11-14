package com.applyrank.model;

import jakarta.persistence.*;

/** One multiple-choice question. The correct answer lives ONLY on the server. */
@Entity
@Table(name = "questions", indexes = @Index(name = "idx_question_domain", columnList = "domain"))
public class Question {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String domain;                         // frontend | backend | data
    private String type;                           // Concept | Predict the output | Spot the bug | Scenario
    @Column(nullable = false, length = 1000)
    private String prompt;
    @Column(length = 2000)
    private String snippet;                        // optional code shown with the question
    @Column(nullable = false, length = 2000)
    private String optionsJson;                    // ["A","B","C","D"]
    private int correctIndex;
    @Column(length = 2000)
    private String explanation;

    public Long getId() { return id; }
    public String getDomain() { return domain; }
    public void setDomain(String v) { this.domain = v; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String v) { this.prompt = v; }
    public String getSnippet() { return snippet; }
    public void setSnippet(String v) { this.snippet = v; }
    public String getOptionsJson() { return optionsJson; }
    public void setOptionsJson(String v) { this.optionsJson = v; }
    public int getCorrectIndex() { return correctIndex; }
    public void setCorrectIndex(int v) { this.correctIndex = v; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String v) { this.explanation = v; }
}
