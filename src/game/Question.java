package game;

import java.util.Arrays;

public class Question {
    String sentence;
    String[] multipleChoices;
    String rightAnswer;
    String category;
    String difficulty;


    @Override
    public String toString() {
        return
                sentence + "\n" +
                Arrays.toString(multipleChoices) + "\n" +
                "Category: " + category + "\n" +
                "Question Level: " + difficulty;
    }




}
