package commands;

import game.Quizz;

public class ShoutHandler implements CommandHandler {
    @Override
    public void execute(Quizz quizz, Quizz.ClientConnectionHandler clientConnectionHandler) {
        String message = clientConnectionHandler.getMessage();
        String messageToSend = message.substring(6);
        quizz.broadcast(clientConnectionHandler.getName(), messageToSend.toUpperCase());
    }
}
