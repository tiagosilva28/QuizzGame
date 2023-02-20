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

public class Quiz {

    /**
     * This class represents a Quiz game that can be played by multiple players simultaneously.
     */

    private final List<PlayerController> players;
    private final Object lock = new Object();
    int gameRounds = 5;
    private ServerSocket serverSocket;
    private ExecutorService service;
    private Queue<Question> questions = new LinkedList<>();
    private int numberOfOnlinePlayers = 0;
    private Map<Integer, Integer> numberOfResponsesByRound = new HashMap<>();

    public Quiz() {
        players = new CopyOnWriteArrayList<>();
    }

    public void start(int port) throws IOException {
        // Initialize server socket and thread pool
        serverSocket = new ServerSocket(port);
        service = Executors.newCachedThreadPool();
        // Create queue of questions from a file
        splitAndCreateQuestionsQueue();
        int numberOfConnections = 0;
        System.out.printf(Messages.SERVER_STARTED, port);
        //Start a new thread to handle each connection
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
    }

    /**
     * Add player to the quiz list of players and send initial message.
     */
    private void addPlayerAndStartGame(PlayerController playerController) {
        players.add(playerController);
        playerController.send(Messages.WELCOME.formatted(playerController.getUserName()));
        playerController.send(Messages.COMMANDS_LIST);
    }

    /**
     * Splits the questions file into individual questions and adds them to the questions queue.
     */
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

    /**
     * Send a question to the player from his queue of questions
     */
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
        sendToMySelf(playerController.getUserName(), "Round: " + playerController.round);
        sendToMySelf(playerController.getUserName(), "\n" + playerController.playerQuestions.element().toString());
    }

    /**
     * Check the answer (input) of the player, send a message to the player informing if the answer is correct or wrong
     * and then remove the question from player's queue of questions and send the message to the player to choose
     * the next question by difficulty or random
     */
    private void checkAnswer(String playerAnswer, PlayerController playerController) {
        if (playerAnswer.startsWith("*")) {
            return;
        }
        if (playerAnswer.toLowerCase().equals(playerController.playerQuestions.element().rightAnswer)) {
            calculateAndSetScore(playerController);
            sendToMySelf(playerController.getUserName(), Messages.CORRECT_ANSWER);
            sendToMySelf(playerController.getUserName(), "Your score is: " + String.valueOf(playerController.getScore()));
        } else {
            sendToMySelf(playerController.getUserName(), Messages.WRONG_ANSWER);
        }
        playerController.playerQuestions.remove();
        if (playerController.round != gameRounds) {
            sendToMySelf(playerController.getUserName(), Messages.NEXT_QUESTION);
        }
        synchronized (lock) {
            int numberOfResponsesThisRound = numberOfResponsesByRound.getOrDefault(playerController.round, 0);
            numberOfResponsesByRound.put(playerController.round, (numberOfResponsesThisRound + 1));
            lock.notifyAll();
        }
    }

    /**
     * Considering the question difficulty, get the question value on DifficultyLevel (enum) and set the player score
     */
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

    /**
     * Print the score of the player
     */
    private void showPlayerScore(PlayerController playerController) {
        System.out.println(playerController.getUserName() + " score: " + playerController.getScore());
    }

    /**
     * If the game finished (number of rounds equals to attribute rounds) check who is the winner.
     * Create a list of winners in case more than one player has the maximum score.
     * Send a message of congratulation to the winners
     * Send game over to all players
     */
    private void checkWinner(PlayerController playerController) throws IOException {

        if (numberOfResponsesByRound.containsKey(gameRounds) && numberOfResponsesByRound.get(gameRounds).equals(numberOfOnlinePlayers)) {
            int highestScore = players.stream()
                    .max(Comparator.comparing(player -> player.getScore()))
                    .map(player -> player.getScore())
                    .get();
            List<PlayerController> winners = players.stream()
                    .filter(player -> player.getScore() == highestScore)
                    .collect(Collectors.toList());
            String winnersName = winners.stream()
                    .map(player -> player.getUserName())
                    .collect(Collectors.joining(", "));
            String winnersMessage = "Congratulation! " + winnersName + " you win the quiz.";
            winners.stream().forEach(winnerPlayer -> winnerPlayer.send(winnersMessage));
            sendToAll(playerController.getUserName(), Messages.GAME_OVER);
            listPlayersScore();
            sendToAll(playerController.getUserName(), listPlayersScore());

        }
    }

    /**
     * Considering the input of the player, enter in switch and select the next question from the player queue question.
     * The question is printed to the player after the lock is notified.
     */
    protected void nextQuestion(PlayerController playerController, String playerInput) {
        synchronized (lock) {
            while (numberOfResponsesByRound.getOrDefault(playerController.round, 0) < numberOfOnlinePlayers) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        switch (playerInput) {
            case "*next":
                playerController.round++;
                sendQuestion(playerController);
                break;
            case "*easy":
                playerController.round++;
                String easyQuestion = playerController.playerQuestions.stream()
                        .filter(question -> question.difficulty
                                .equals("easy")).map(question -> question.toString()).findFirst().orElse(" ");
                sendToMySelf(playerController.getUserName(), "Round: " + playerController.round);
                sendToMySelf(playerController.getUserName(), "\n" + easyQuestion);
                break;
            case "*medium":
                playerController.round++;
                String mediumQuestion = playerController.playerQuestions.stream()
                        .filter(question -> question.difficulty
                                .equals("medium")).map(question -> question.toString()).findFirst().orElse(" ");
                sendToMySelf(playerController.getUserName(), "Round: " + playerController.round);
                sendToMySelf(playerController.getUserName(), "\n" + mediumQuestion);
                break;
            case "*hard":
                playerController.round++;
                String hardQuestion = playerController.playerQuestions.stream()
                        .filter(question -> question.difficulty
                                .equals("hard")).map(question -> question.toString()).findFirst().orElse(" ");
                sendToMySelf(playerController.getUserName(), "Round: " + playerController.round);
                sendToMySelf(playerController.getUserName(), "\n" + hardQuestion);
                break;
        }
        showPlayerScore(playerController);
    }

    /**
     * Send a message to all players.
     */
    public void sendToAll(String whoIsSending, String message) {
        players.stream()
                // .filter(handler -> !handler.getName().equals(name))
                .forEach(player -> player.send(message));
    }

    /**
     * Send a message only to a specific player
     */
    public void sendToMySelf(String name, String message) {
        players.stream()
                .filter(handler -> handler.getUserName().equals(name))
                .forEach(handler -> handler.send(message));
    }

    /**
     * Shows a list of all players connected.
     */
    public String listPlayersScore() {
        StringBuffer buffer = new StringBuffer();
        players.forEach(client -> buffer.append(client.getUserName()).append(" score: " + client.score + "\n"));
        return buffer.toString();
    }

    public void removePlayer(PlayerController playerController) {
        players.remove(playerController);
    }

    /*private void leaderboard(PlayerController playerController) {
        // read in scores
        List<Score> scores = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("resources/leaderboard.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" with score: ");
                String name = parts[0];
                int score = Integer.parseInt(parts[1]);
                scores.add(new Score(name, score));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // sort by score
        Collections.sort(scores, Comparator.comparingInt(Score::getScore).reversed());

        // write out sorted scores
        try (PrintWriter pw = new PrintWriter(new FileWriter("resources/leaderboard.txt"))) {
            for (Score score : scores) {
                pw.printf("%s with score: %d \n", score.getName(), score.getScore());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

     */

    public Optional<PlayerController> getPlayerByName(String name) {
        return players.stream()
                .filter(clientConnectionHandler -> clientConnectionHandler.getUserName().equalsIgnoreCase(name))
                .findFirst();
    }

    // Score class to store player name and score
    private static class Score {
        private final String name;
        private final int score;

        public Score(String name, int score) {
            this.name = name;
            this.score = score;
        }

        public String getName() {
            return name;
        }

        public int getScore() {
            return score;
        }
    }

    public class PlayerController implements Runnable {
        private String userName;
        private Socket playerSocket;
        private BufferedWriter out;
        private String message;
        private int score = 0;
        private int round = 1;
        private Queue<Question> playerQuestions = new LinkedList<>(questions);

        private PlayerController(Socket playerSocket, String userName) throws IOException {
            this.playerSocket = playerSocket;
            this.userName = userName;
            this.out = new BufferedWriter(new OutputStreamWriter(playerSocket.getOutputStream()));
            Collections.shuffle((List<Question>) playerQuestions);
        }

        @Override
        public void run() {
            addPlayerAndStartGame(this);
            try {
                Scanner in = new Scanner(playerSocket.getInputStream());
                sendToMySelf(this.userName, "Write your username: ");
                String userName = in.nextLine();
                sendToMySelf(this.userName, "\n");
                setUserName(userName);
                sendQuestion(this);
                while (in.hasNext()) {
                    message = in.nextLine();
                    if (isCommand(message)) {
                        dealWithCommand(message);
                        continue;
                    }
                    if (isAnAnswer(message)) {
                        checkAnswer(message, this);
                        showPlayerScore(this);
                        checkWinner(this);
                        continue;
                    }
                    if (isACommandForNextQuestion(message)) {
                        nextQuestion(this, message);
                        continue;
                    }
                    sendToMySelf(this.userName, "Invalid command, please write again.");
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
    }
}