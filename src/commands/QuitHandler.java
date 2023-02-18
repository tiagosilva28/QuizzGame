package commands;

import game.Quiz;
import messages.Messages;

public class QuitHandler implements CommandHandler {

    @Override
    public void execute(Quiz quiz, Quiz.PlayerController playerController) {
        quiz.removeClient(playerController);
        quiz.sendToAll(playerController.getUserName(), playerController.getUserName() + Messages.CLIENT_DISCONNECTED);
        playerController.close();
    }
}
