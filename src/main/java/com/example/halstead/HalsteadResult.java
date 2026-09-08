package com.example.halstead;

import java.util.List;

public class HalsteadResult {
    private List<TokenCount> operators;
    private List<TokenCount> operands;
    private int eta1, eta2, N1, N2;
    private int vocabulary;
    private int length;
    private double volume;
    private List<TableRow> tableRows;

    public List<TableRow> getTableRows() { return tableRows; }
    public void setTableRows(List<TableRow> tableRows) { this.tableRows = tableRows; }

    // геттеры/сеттеры
    public List<TokenCount> getOperators() { return operators; }
    public void setOperators(List<TokenCount> operators) { this.operators = operators; }
    public List<TokenCount> getOperands() { return operands; }
    public void setOperands(List<TokenCount> operands) { this.operands = operands; }
    public int getEta1() { return eta1; }
    public void setEta1(int eta1) { this.eta1 = eta1; }
    public int getEta2() { return eta2; }
    public void setEta2(int eta2) { this.eta2 = eta2; }
    public int getN1() { return N1; }
    public void setN1(int n1) { N1 = n1; }
    public int getN2() { return N2; }
    public void setN2(int n2) { N2 = n2; }
    public int getVocabulary() { return vocabulary; }
    public void setVocabulary(int vocabulary) { this.vocabulary = vocabulary; }
    public int getLength() { return length; }
    public void setLength(int length) { this.length = length; }
    public double getVolume() { return volume; }
    public void setVolume(double volume) { this.volume = volume; }
}