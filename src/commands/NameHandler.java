package commands;

import game.Quizz;
import messages.Messages;


public class NameHandler implements CommandHandler{
    @Override
    public void execute(Quizz quizz, Quizz.ClientConnectionHandler clientConnectionHandler) {
        String message = clientConnectionHandler.getMessage();
        String name = message.substring(6);
        String oldName = clientConnectionHandler.getUserName();
        quizz.getClientByName(name).ifPresentOrElse(
                client -> clientConnectionHandler.send(Messages.CLIENT_ALREADY_EXISTS),
                () -> {
                    clientConnectionHandler.setUserName(name);
                    clientConnectionHandler.send(Messages.SELF_NAME_CHANGED.formatted(name));
                    quizz.sendToAll(name, Messages.NAME_CHANGED.formatted(oldName, name));
                }
        );
    }
}
