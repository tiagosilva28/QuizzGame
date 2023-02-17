package commands;

import game.Quizz;

public interface CommandHandler {
    void execute(Quizz quizz, Quizz.PlayerController playerController);
}
