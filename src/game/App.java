package game;

import java.io.IOException;

public class App {
    public static void main(String[] args) {
        Quiz quiz = new Quiz();

        try {
            quiz.start(8095);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}