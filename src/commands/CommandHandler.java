package commands;

import game.Quiz;

public interface CommandHandler {
    void execute(Quiz quiz, Quiz.PlayerController playerController);
}
