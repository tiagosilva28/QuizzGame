package commands;

import game.Quizz;

public class ShoutHandler implements CommandHandler {
    @Override
    public void execute(Quizz quizz, Quizz.PlayerController playerController) {
        String message = playerController.getMessage();
        String messageToSend = message.substring(6);
        quizz.sendToAll(playerController.getUserName(), messageToSend.toUpperCase());
    }
}
