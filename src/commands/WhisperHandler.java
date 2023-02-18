package commands;

import game.Quiz;
import messages.Messages;

import java.util.Optional;

public class WhisperHandler implements CommandHandler {

    @Override
    public void execute(Quiz quiz, Quiz.PlayerController playerController) {
        String message = playerController.getMessage();

        if (message.split(" ").length < 3) {
            playerController.send(Messages.WHISPER_INSTRUCTIONS);
            return;
        }

        Optional<Quiz.PlayerController> receiverClient = quiz.getPlayerByName(message.split(" ")[1]);

        if (receiverClient.isEmpty()) {
            playerController.send(Messages.NO_SUCH_CLIENT);
            return;
        }

        String messageToSend = message.substring(message.indexOf(" ") + 1).substring(message.indexOf(" ") + 1);
        receiverClient.get().send(playerController.getUserName() + Messages.WHISPER + ": " + messageToSend);
    }
}
