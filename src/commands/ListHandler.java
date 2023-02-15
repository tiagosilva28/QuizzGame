package commands;

import game.Quizz;

public class ListHandler implements CommandHandler {

    @Override
    public void execute(Quizz quizz, Quizz.ClientConnectionHandler clientConnectionHandler) {
       clientConnectionHandler.send(quizz.listClients());
    }
}
