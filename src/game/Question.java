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
        return "Question{" +
                "sentence='" + sentence + '\'' +
                ", multipleChoices=" + Arrays.toString(multipleChoices) +
                ", rightAnswer='" + rightAnswer + '\'' +
                ", category='" + category + '\'' +
                ", difficulty='" + difficulty + '\'' +
                '}';
    }
}
