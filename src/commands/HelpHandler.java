package commands;

import game.Quizz;
import messages.Messages;

public class HelpHandler implements CommandHandler {

    @Override
    public void execute(Quizz quizz, Quizz.PlayerController playerController) {
        playerController.send(Messages.COMMANDS_LIST);
    }
}
