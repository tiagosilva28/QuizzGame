package commands;

import game.Quizz;
import messages.Messages;

public class QuitHandler implements CommandHandler {

    @Override
    public void execute(Quizz quizz, Quizz.ClientConnectionHandler clientConnectionHandler) {
        quizz.removeClient(clientConnectionHandler);
        quizz.broadcast(clientConnectionHandler.getName(), clientConnectionHandler.getName() + Messages.CLIENT_DISCONNECTED);
        clientConnectionHandler.close();
    }
}
