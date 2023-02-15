package commands;

import game.Question;
import game.Quizz;
import messages.Messages;

public class HelpHandler implements CommandHandler {

    @Override
    public void execute(Quizz quizz, Quizz.ClientConnectionHandler clientConnectionHandler) {
        clientConnectionHandler.send(Messages.COMMANDS_LIST);
    }
}
