package com.example.halstead;

public class TableRow {
    private int j;
    private String operator;
    private int f1j;
    private int i;
    private String operand;
    private int f2i;

    public TableRow(int j, String operator, int f1j, int i, String operand, int f2i) {
        this.j = j;
        this.operator = operator;
        this.f1j = f1j;
        this.i = i;
        this.operand = operand;
        this.f2i = f2i;
    }

    public int getJ() { return j; }
    public String getOperator() { return operator; }
    public int getF1j() { return f1j; }
    public int getI() { return i; }
    public String getOperand() { return operand; }
    public int getF2i() { return f2i; }
}