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

public class Quizz {
    private ServerSocket serverSocket;
    private ExecutorService service;
    private final List<ClientConnectionHandler> players;
    private Queue<Question> questions = new LinkedList<>();
    private boolean hasResponded = true;
    private int numberOfOnlinePlayers = 0;

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

    protected synchronized void splitAndCreateQuestionsQueue() {
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

    protected void sendQuestion(ClientConnectionHandler clientConnectionHandler) {
        sendToMySelf(clientConnectionHandler.getUserName(), questions.element().toString());
    }
    protected void nextQuestion(ClientConnectionHandler clientConnectionHandler){
        sendQuestion(clientConnectionHandler);
        showPlayerScore(clientConnectionHandler);
    }

    public void acceptConnection(int numberOfConnections) throws IOException {
        Socket clientSocket = serverSocket.accept();
        ClientConnectionHandler clientConnectionHandler =
                new ClientConnectionHandler(clientSocket,
                        Messages.DEFAULT_NAME + numberOfConnections);
        service.submit(clientConnectionHandler);
        //addClient(clientConnectionHandler);
    }

    private void addPlayer(ClientConnectionHandler clientConnectionHandler) {
        /*synchronized (clients) {
            clients.add(clientConnectionHandler);
        }*/

        players.add(clientConnectionHandler);
        clientConnectionHandler.send(Messages.WELCOME.formatted(clientConnectionHandler.getUserName()));
        clientConnectionHandler.send(Messages.COMMANDS_LIST);
        sendToAll(clientConnectionHandler.getUserName(), Messages.CLIENT_ENTERED_CHAT);
    }

    private void checkAnswer(String playerAnswer, ClientConnectionHandler clientConnectionHandler) {
        if(playerAnswer.toLowerCase().equals(questions.element().rightAnswer)) {
            calculateAndSetScore(clientConnectionHandler);
            sendToMySelf(clientConnectionHandler.getUserName(), Messages.CORRECT_ANSWER);
            sendToMySelf(clientConnectionHandler.getUserName(), String.valueOf(clientConnectionHandler.getScore()));
        } else {
            sendToMySelf(clientConnectionHandler.getUserName(), Messages.WRONG_ANSWER);
        }
        questions.remove();
        clientConnectionHandler.round++;
    }

    private void calculateAndSetScore(ClientConnectionHandler clientConnectionHandler){
        switch (questions.element().difficulty){
            case "easy":
                clientConnectionHandler.setScore(clientConnectionHandler.getScore() + DifficultyLevels.EASY.getScore());
            case "medium":
                clientConnectionHandler.setScore(clientConnectionHandler.getScore() + DifficultyLevels.MEDIUM.getScore());
            case "hard":
                clientConnectionHandler.setScore(clientConnectionHandler.getScore() + DifficultyLevels.HARD.getScore());
        }




    }

    private String showPlayerScore(ClientConnectionHandler clientConnectionHandler){
        System.out.println(clientConnectionHandler.getUserName() + " score: " + clientConnectionHandler.getScore());
        return clientConnectionHandler.getUserName() + " score: " + clientConnectionHandler.getScore();
    }
    private String checkWinner(ClientConnectionHandler clientConnectionHandler){
        String theWinner = null;

        long nrPlayerThatFinished = players.stream()
                .filter(player -> player.round == 2).count();
        if(nrPlayerThatFinished == numberOfOnlinePlayers){
          theWinner = String.valueOf(players.stream()
                            .sorted(Comparator.comparing(player -> player.getScore())).findFirst()
                            .map(player -> player.getUserName()));
        }
            sendToAll(showPlayerScore(clientConnectionHandler), theWinner);
            return theWinner;

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

    public void removeClient(ClientConnectionHandler clientConnectionHandler) {
        players.remove(clientConnectionHandler);

    }

    public Optional<ClientConnectionHandler> getClientByName(String name) {
        return players.stream()
                .filter(clientConnectionHandler -> clientConnectionHandler.getUserName().equalsIgnoreCase(name))
                .findFirst();
    }

    public class ClientConnectionHandler implements Runnable {

        private String userName;
        private Socket clientSocket;
        private BufferedWriter out;
        private String message;
        private String playerAnswer;
        private int score;
        private int round;

        public ClientConnectionHandler(Socket clientSocket, String userName) throws IOException {
            this.clientSocket = clientSocket;
            this.userName = userName;
            this.out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        }

        @Override
        public void run() {

            addPlayer(this);

            try {
                sendQuestion(this);
                // BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                Scanner in = new Scanner(clientSocket.getInputStream());
                while (in.hasNext()) {

                    message = in.nextLine();
                    if (isCommand(message)) {
                        dealWithCommand(message);
                        continue;
                    }
                    if (isAnAnswer(message)) {
                        checkAnswer(message,this);
                        nextQuestion(this);
                        showPlayerScore(this);
                       // System.out.println(checkWinner(this));
                        continue;
                    }
                    if (message.equals("")) {
                        continue;
                    }
                    sendToAll(userName, message);
                }
            } catch (IOException e) {
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
            clientSocket.close();
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

    }
}




