package commands;

import game.Quiz;

public class ListHandler implements CommandHandler {

    @Override
    public void execute(Quiz quiz, Quiz.PlayerController playerController) {
        playerController.send(quiz.listPlayersScore());
    }
}
