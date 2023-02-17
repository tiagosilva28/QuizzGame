package commands;

import game.Quizz;
import messages.Messages;

public class QuitHandler implements CommandHandler {

    @Override
    public void execute(Quizz quizz, Quizz.PlayerController playerController) {
        quizz.removeClient(playerController);
        quizz.sendToAll(playerController.getUserName(), playerController.getUserName() + Messages.CLIENT_DISCONNECTED);
        playerController.close();
    }
}
