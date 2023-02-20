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

public class Quiz {
    private final List<PlayerController> players;
    private final Object lock = new Object();
    private ServerSocket serverSocket;
    private ExecutorService service;
    private Queue<Question> questions = new LinkedList<>();
    private boolean hasResponded = true;
    private int numberOfOnlinePlayers = 0;


    public Quiz() {
        players = new CopyOnWriteArrayList<>();
    } //Hello aaaaa

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        service = Executors.newCachedThreadPool();
        splitAndCreateQuestionsQueue();
        int numberOfConnections = 0;
        System.out.printf(Messages.SERVER_STARTED, port);


        while (true) {
            acceptConnection(numberOfConnections);
            ++numberOfConnections;

            synchronized (lock) {
                ++numberOfOnlinePlayers;
                lock.notifyAll();
            }

        }
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
        players.add(playerController);
        playerController.send(Messages.WELCOME.formatted(playerController.getUserName()));
        playerController.send(Messages.COMMANDS_LIST);
        //playerController.send("Choose your user name:");
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

        synchronized (lock) {
            while (numberOfOnlinePlayers < 2) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        sendToMySelf(playerController.getUserName(), playerController.playerQuestions.element().toString());

        Timer timer = new Timer();
        playerController.questionTimer = timer;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!playerController.playerQuestions.isEmpty()) {
                    sendToMySelf(playerController.getUserName(), "Time's up!");
                    playerController.playerQuestions.remove();
                    sendToMySelf(playerController.getUserName(), Messages.NEXT_QUESTION);
                    playerController.round++;
                }
            }
        }, 5000);
    }

    private void checkAnswer(String playerAnswer, PlayerController playerController) {
        if (playerAnswer.startsWith("*")) {
            return;
        }
        if (playerAnswer.toLowerCase().equals(playerController.playerQuestions.element().rightAnswer)) {
            calculateAndSetScore(playerController);
            sendToMySelf(playerController.getUserName(), Messages.CORRECT_ANSWER);
            sendToMySelf(playerController.getUserName(), "Your score is: " + String.valueOf(playerController.getScore()));
            playerController.questionTimer.cancel();

        } else {
            sendToMySelf(playerController.getUserName(), Messages.WRONG_ANSWER);
        }
        playerController.playerQuestions.remove();
        sendToMySelf(playerController.getUserName(), Messages.NEXT_QUESTION);
        playerController.round++;
        playerController.questionTimer.cancel();

    }

    private void calculateAndSetScore(PlayerController playerController) {
        switch (playerController.playerQuestions.element().difficulty) {
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

    private void showPlayerScore(PlayerController playerController) {
        System.out.println(playerController.getUserName() + " score: " + playerController.getScore());

    }

    private void checkWinner(PlayerController playerController) {
        String theWinner = null;
        int nrPlayerThatFinished = players.stream()
                .filter(player -> player.round == 2).toList().size();

        if (nrPlayerThatFinished == numberOfOnlinePlayers) {
            theWinner = players.stream()
                    .max(Comparator.comparing(player -> player.getScore()))
                    .map(player -> "The Winner is: " + player.getUserName() + " with the score: " + player.getScore()).orElse("TIEEEEEE");
            sendToAll(playerController.getUserName(), theWinner);
        }
    }

    protected void nextQuestion(PlayerController playerController, String playerInput) {
        switch (playerInput) {
            case "*next":
                sendQuestion(playerController);
                break;
            case "*easy":
                String easyQuestion = playerController.playerQuestions.stream()
                        .filter(question -> question.difficulty
                                .equals("easy")).map(question -> question.toString()).findFirst().orElse(" ");
                sendToMySelf(playerController.getUserName(), easyQuestion);
                break;
            case "*medium":
                String mediumQuestion = playerController.playerQuestions.stream()
                        .filter(question -> question.difficulty
                                .equals("medium")).map(question -> question.toString()).findFirst().orElse(" ");
                sendToMySelf(playerController.getUserName(), mediumQuestion);
                break;
            case "*hard":
                String hardQuestion = playerController.playerQuestions.stream()
                        .filter(question -> question.difficulty
                                .equals("hard")).map(question -> question.toString()).findFirst().orElse(" ");
                sendToMySelf(playerController.getUserName(), hardQuestion);
                break;
        }
        //sendQuestion(playerController);
        showPlayerScore(playerController);
    }

    public void sendToAll(String whoIsSending, String message) {
        players.stream()
                // .filter(handler -> !handler.getName().equals(name))
                .forEach(player -> player.send(message));
    }

    public void sendToMySelf(String name, String message) {
        players.stream()
                .filter(handler -> handler.getUserName().equals(name))
                .forEach(handler -> handler.send(message));
    }

    public String listClients() {
        StringBuffer buffer = new StringBuffer();
        players.forEach(client -> buffer.append(client.getUserName()).append("\n"));
        return buffer.toString();
    }

    public void removePlayer(PlayerController playerController) {
        players.remove(playerController);

    } //here it is

    public Optional<PlayerController> getPlayerByName(String name) {
        return players.stream()
                .filter(clientConnectionHandler -> clientConnectionHandler.getUserName().equalsIgnoreCase(name))
                .findFirst();
    }

    public class PlayerController implements Runnable {

        public Timer questionTimer;
        private String userName;
        private Socket playerSocket;
        private BufferedWriter out;
        private String message;
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
                    if (isAnAnswer(message) || isACommandForNextQuestion(message)) {
                        checkAnswer(message, this);
                        nextQuestion(this, message);
                        showPlayerScore(this);
                        checkWinner(this);
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

        private boolean isACommandForNextQuestion(String message) {
            return message.startsWith("*");
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

            command.getHandler().execute(Quiz.this, this);
        }

        public void send(String message) {
            try {
                out.write(message);
                out.newLine();
                out.flush();
            } catch (IOException e) {
                removePlayer(this);
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




