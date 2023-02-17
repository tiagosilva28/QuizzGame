package commands;

import game.Quizz;
import messages.Messages;


public class NameHandler implements CommandHandler{
    @Override
    public void execute(Quizz quizz, Quizz.PlayerController playerController) {
        String message = playerController.getMessage();
        String name = message.substring(6);
        String oldName = playerController.getUserName();
        quizz.getClientByName(name).ifPresentOrElse(
                client -> playerController.send(Messages.CLIENT_ALREADY_EXISTS),
                () -> {
                    playerController.setUserName(name);
                    playerController.send(Messages.SELF_NAME_CHANGED.formatted(name));
                    quizz.sendToAll(name, Messages.NAME_CHANGED.formatted(oldName, name));
                }
        );
    }
}
