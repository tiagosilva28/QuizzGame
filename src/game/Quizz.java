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
    private int questionIndex;
    private boolean hasResponded = true;

    public Quizz() {
        players = new CopyOnWriteArrayList<>();
    }

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        service = Executors.newCachedThreadPool();
        int numberOfConnections = 0;
        System.out.printf(Messages.SERVER_STARTED, port);


        while (true) {
            acceptConnection(numberOfConnections);
            ++numberOfConnections;
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

    protected void sendQuestion(ClientConnectionHandler clientConnectionHandler) {
        //splitAndCreateQuestionsQueue();
        //clientConnectionHandler.send(questions.element().toString());
        broadcast("Quiz question: ", questions.element().toString());
        //System.out.println(questions.element().toString());
        //questions.remove();
    }
    protected void nextQuestion(ClientConnectionHandler clientConnectionHandler){
        sendQuestion(clientConnectionHandler);
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
        clientConnectionHandler.send(Messages.WELCOME.formatted(clientConnectionHandler.getName()));
        clientConnectionHandler.send(Messages.COMMANDS_LIST);
        broadcast(clientConnectionHandler.getName(), Messages.CLIENT_ENTERED_CHAT);
    }

    //private boolean hasAWinner(ClientConnectionHandler clientConnectionHandler){
    //  if(clientConnectionHandler)
    //}
    private synchronized void playerResponse(ClientConnectionHandler clientConnectionHandler) {
        while (clientConnectionHandler.playerAnswer == "") {
            try {
                clientConnectionHandler.playerAnswer.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }

        /*if(isAnswerRight(clientConnectionHandler.playerAnswer)){
            players.stream()
                    .forEach(handler -> handler.send(clientConnectionHandler.getName() + ": " + Messages.CORRECT_ANSWER ));
            broadcast(clientConnectionHandler.getName(), clientConnectionHandler.playerAnswer);
        }else {
        players.stream()
                .forEach(handler -> handler.send(clientConnectionHandler.getName() + ": " + Messages.WRONG_ANSWER));
        broadcast(clientConnectionHandler.getName(), clientConnectionHandler.playerAnswer);
        }
        hasResponded=true;
        notify();

         */
    }

    public void broadcast(String name, String message) {
        players.stream()
                .filter(handler -> !handler.getName().equals(name))
                .forEach(handler -> handler.send(name + ": " + message));
    }

    public String listClients() {
        StringBuffer buffer = new StringBuffer();
        players.forEach(client -> buffer.append(client.getName()).append("\n"));
        return buffer.toString();
    }

    public void removeClient(ClientConnectionHandler clientConnectionHandler) {
        players.remove(clientConnectionHandler);

    }

    public Optional<ClientConnectionHandler> getClientByName(String name) {
        return players.stream()
                .filter(clientConnectionHandler -> clientConnectionHandler.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public class ClientConnectionHandler implements Runnable {

        private String name;
        private Socket clientSocket;
        private BufferedWriter out;
        private String message;
        private String playerAnswer;

        public ClientConnectionHandler(Socket clientSocket, String name) throws IOException {
            this.clientSocket = clientSocket;
            this.name = name;
            this.out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        }

        @Override
        public void run() {

            addPlayer(this);
            splitAndCreateQuestionsQueue();
            sendQuestion(this);

            try {
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
                        continue;
                    }
                    if (message.equals("")) {
                        continue;
                    }

                    broadcast(name, message);
                }
            } catch (IOException e) {
                System.err.println(Messages.CLIENT_ERROR + e.getMessage());
            }
        }
            /*finally {
                removeClient(this);
            }*/


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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

        public String getName() {
        return name;
    }

        public void setName(String name) {
        this.name = name;
    }

        public String getMessage() {
        return message;
    }

        public void checkAnswer(String playerAnswer, ClientConnectionHandler clientConnectionHandler) {
        if (playerAnswer.toLowerCase().equals(questions.element().rightAnswer)) {
            players.stream()
                    .forEach(handler -> handler.send(Messages.CORRECT_ANSWER));
            nextQuestion(clientConnectionHandler);
        } else {
            players.stream()
                    .forEach(handler -> handler.send(Messages.WRONG_ANSWER));
            nextQuestion(clientConnectionHandler);
            //broadcast(clientConnectionHandler.getName(), clientConnectionHandler.playerAnswer);
        }
    }

    }
}




