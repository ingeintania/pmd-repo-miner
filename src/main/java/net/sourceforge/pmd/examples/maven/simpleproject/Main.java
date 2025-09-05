package net.sourceforge.pmd.examples.maven.simpleproject;

public class Main {

    public static void main(String[] args) {
        System.out.println("Hello World!");

        //AvoidReassigningCatchVariables
        try {
            System.out.println("Hello One");
        } catch (Exception e) {
            e = new NullPointerException(); // not recommended
        }
    }
}
