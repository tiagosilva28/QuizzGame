package commands;

import game.Quizz;

public class ListHandler implements CommandHandler {

    @Override
    public void execute(Quizz quizz, Quizz.PlayerController playerController) {
       playerController.send(quizz.listClients());
    }
}
