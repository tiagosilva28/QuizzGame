package game;

import java.io.IOException;

public class App {
    public static void main(String[] args) {
        Quizz quizz = new Quizz();

        try {
            quizz.start(8089);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}