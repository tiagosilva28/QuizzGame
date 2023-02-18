package commands;

import game.Quiz;
import messages.Messages;

public class HelpHandler implements CommandHandler {

    @Override
    public void execute(Quiz quiz, Quiz.PlayerController playerController) {
        playerController.send(Messages.COMMANDS_LIST);
    }
}
