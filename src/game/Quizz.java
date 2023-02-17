package game;

import commands.Command;
import messages.Messages;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Quizz {
    private ServerSocket serverSocket;
    private ExecutorService service;
    private final List<PlayerController> players;
    private Queue<Question> questions = new LinkedList<>();
    private boolean hasResponded = true;
    private int numberOfOnlinePlayers = 0;


    //paracommit teste

    public Quizz() {
        players = new CopyOnWriteArrayList<>();
    }

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        service = Executors.newCachedThreadPool();
        splitAndCreateQuestionsQueue();
        int numberOfConnections = 0;
        System.out.printf(Messages.SERVER_STARTED, port);


        while (true) {
            acceptConnection(numberOfConnections);
            ++numberOfConnections;
            ++numberOfOnlinePlayers;
        }
    }

    protected void splitAndCreateQuestionsQueue() {
        Path fileQuestionsPath = Paths.get("resources/questions.txt");
        try (BufferedReader br = new BufferedReader(new FileReader(fileQuestionsPath.toFile()))) {

            String line = br.readLine();
            while (line != null) {
                String[] parts = line.split("::");
                Question question = new Question();
                question.sentence = parts[0];
                question.rightAnswer = parts[1];
                question.multipleChoices = parts[2].split(";");
                question.difficulty = parts[3];
                question.category = parts[4];
                questions.add(question);
                line = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void sendQuestion(PlayerController playerController) {
        /*try {
            this.wait();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }*/
        sendToMySelf(playerController.getUserName(), playerController.playerQuestions.element().toString());
    }
    protected void nextQuestion(PlayerController playerController){
        sendQuestion(playerController);
        showPlayerScore(playerController);
    }

    public void acceptConnection(int numberOfConnections) throws IOException {
        Socket playerSocket = serverSocket.accept();
        PlayerController playerController =
                new PlayerController(playerSocket,
                        Messages.DEFAULT_NAME + numberOfConnections);
        service.submit(playerController);
        //addClient(clientConnectionHandler);
    }

    private void addPlayer(PlayerController playerController) {
        /*synchronized (clients) {
            clients.add(clientConnectionHandler);
        }*/

        players.add(playerController);
        playerController.send(Messages.WELCOME.formatted(playerController.getUserName()));
        playerController.send(Messages.COMMANDS_LIST);
        sendToAll(playerController.getUserName(), Messages.CLIENT_ENTERED_CHAT);
    }

    private void checkAnswer(String playerAnswer, PlayerController playerController) {
        if(playerAnswer.toLowerCase().equals(playerController.playerQuestions.element().rightAnswer)) {
            calculateAndSetScore(playerController);
            sendToMySelf(playerController.getUserName(), Messages.CORRECT_ANSWER);
            sendToMySelf(playerController.getUserName(), String.valueOf(playerController.getScore()));
        } else {
            sendToMySelf(playerController.getUserName(), Messages.WRONG_ANSWER);
        }
        playerController.playerQuestions.remove();
        playerController.round++;
    }

    private void calculateAndSetScore(PlayerController playerController){
        switch (playerController.playerQuestions.element().difficulty){
            case "easy":
                playerController.setScore(playerController.getScore() + DifficultyLevels.EASY.getScore());
                break;
            case "medium":
                playerController.setScore(playerController.getScore() + DifficultyLevels.MEDIUM.getScore());
                break;
            case "hard":
                playerController.setScore(playerController.getScore() + DifficultyLevels.HARD.getScore());
                break;
        }
    }

    private void showPlayerScore(PlayerController playerController){
        System.out.println(playerController.getUserName() + " score: " + playerController.getScore());

    }
    private void checkWinner(PlayerController playerController){
        String theWinner = null;


        int nrPlayerThatFinished = players.stream()
                .filter(player -> player.round == 2).toList().size();

        System.out.println(playerController.playerQuestions.size());

        System.out.println("ROUND " + playerController.round);
        System.out.println("PLAYERS FINISH " + nrPlayerThatFinished);
        System.out.println("ONLINE PLAYERS " + numberOfOnlinePlayers);


        if(nrPlayerThatFinished == numberOfOnlinePlayers){
         theWinner = players.stream()
                  .max(Comparator.comparing(player -> player.getScore()))
                  .map(player -> "The Winner is: " + player.getUserName() + " with the score: " + player.getScore()).orElse("TIEEEEEE");
            sendToAll(playerController.getUserName(), theWinner);
        }
        System.out.println("ENTER IN METHOD WINNER");

    }

    public void sendToAll(String name, String message) {
        players.stream()
               // .filter(handler -> !handler.getName().equals(name))
                .forEach(player -> player.send(name + ": " + message));
    }

    public void sendToMySelf(String name, String message) {
        players.stream()
                .filter(handler -> handler.getUserName().equals(name))
                .forEach(handler -> handler.send(name + ": " + message));
    }

    public String listClients() {
        StringBuffer buffer = new StringBuffer();
        players.forEach(client -> buffer.append(client.getUserName()).append("\n"));
        return buffer.toString();
    }

    public void removeClient(PlayerController playerController) {
        players.remove(playerController);

    }

    public Optional<PlayerController> getClientByName(String name) {
        return players.stream()
                .filter(clientConnectionHandler -> clientConnectionHandler.getUserName().equalsIgnoreCase(name))
                .findFirst();
    }

    public class PlayerController implements Runnable {

        private String userName;
        private Socket playerSocket;
        private BufferedWriter out;
        private String message;
        private String playerAnswer;
        private int score = 0;
        private int round;
        private Queue<Question> playerQuestions = new LinkedList<>(questions);
        private PlayerController(Socket playerSocket, String userName) throws IOException {
            this.playerSocket = playerSocket;
            this.userName = userName;
            this.out = new BufferedWriter(new OutputStreamWriter(playerSocket.getOutputStream()));
        }


        @Override
        public void run() {


            addPlayer(this);

                try {
                        sendQuestion(this);
                        // BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        Scanner in = new Scanner(playerSocket.getInputStream());
                        while (in.hasNext()) {

                            message = in.nextLine();
                            if (isCommand(message)) {
                                dealWithCommand(message);
                                continue;
                            }
                            if (isAnAnswer(message)) {
                                checkAnswer(message, this);
                                nextQuestion(this);
                                showPlayerScore(this);
                                checkWinner(this);
                                continue;
                            }
                            if (message.equals("")) {
                                continue;
                            }
                            sendToAll(userName, message);
                        }

                    } catch(IOException e){
                        System.err.println(Messages.CLIENT_ERROR + e.getMessage());
                    }

        }


        private boolean isCommand(String message) {
        return message.startsWith("/");
    }
        private boolean isAnAnswer(String message) {
        return message.matches("(?i)[abcd]");
    }
        private void dealWithCommand(String message) throws IOException {
        String description = message.split(" ")[0];
        Command command = Command.getCommandFromDescription(description);

        if (command == null) {
            out.write(Messages.NO_SUCH_COMMAND);
            out.newLine();
            out.flush();
            return;
        }

        command.getHandler().execute(Quizz.this, this);
    }
        public void send(String message) {
        try {
            out.write(message);
            out.newLine();
            out.flush();
        } catch (IOException e) {
            removeClient(this);
            e.printStackTrace();
        }
    }
        public void close() {
        try {
            playerSocket.close();
            --numberOfOnlinePlayers;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
        public String getUserName() {
        return userName;
    }
        public void setUserName(String userName) {
        this.userName = userName;
    }
        public int getScore() {
            return score;
        }
        public void setScore(int score) {
            this.score = score;
        }
        public String getMessage() {
        return message;
    }

        public void setPlayerQuestions(Queue<Question> playerQuestions) {
            this.playerQuestions = playerQuestions;
        }
    }
}




