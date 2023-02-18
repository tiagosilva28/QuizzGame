package commands;

import game.Quiz;

public class ShoutHandler implements CommandHandler {
    @Override
    public void execute(Quiz quiz, Quiz.PlayerController playerController) {
        String message = playerController.getMessage();
        String messageToSend = message.substring(6);
        quiz.sendToAll(playerController.getUserName(), messageToSend.toUpperCase());
    }
}
