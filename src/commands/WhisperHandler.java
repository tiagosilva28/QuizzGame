package commands;

import game.Quizz;
import messages.Messages;

import java.util.Optional;

public class WhisperHandler implements CommandHandler {

    @Override
    public void execute(Quizz quizz, Quizz.ClientConnectionHandler clientConnectionHandler) {
        String message = clientConnectionHandler.getMessage();

        if (message.split(" ").length < 3) {
            clientConnectionHandler.send(Messages.WHISPER_INSTRUCTIONS);
            return;
        }

        Optional<Quizz.ClientConnectionHandler> receiverClient = quizz.getClientByName(message.split(" ")[1]);

        if (receiverClient.isEmpty()) {
            clientConnectionHandler.send(Messages.NO_SUCH_CLIENT);
            return;
        }

        String messageToSend = message.substring(message.indexOf(" ") + 1).substring(message.indexOf(" ") + 1);
        receiverClient.get().send(clientConnectionHandler.getUserName() + Messages.WHISPER + ": " + messageToSend);
    }
}
