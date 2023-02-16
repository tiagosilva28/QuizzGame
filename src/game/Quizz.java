package game;

import commands.Command;
import messages.Messages;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Quizz {
    private ServerSocket serverSocket;
    private ExecutorService service;
    private final List<ClientConnectionHandler> clients;
    private Path fileQuestions;
    private Question question;

    public Quizz(){
        clients = new CopyOnWriteArrayList<>();
    }

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        service = Executors.newCachedThreadPool();
        int numberOfConnections = -1;
        System.out.printf(Messages.SERVER_STARTED, port);
        sendQuestion();



        while (true) {
            ++numberOfConnections;
            acceptConnection(numberOfConnections);

        }
    }

    protected void sendQuestion(){
        Path fileQuestions = Paths.get("resources/questions.txt");
        question = new Question();
        try (BufferedReader br = new BufferedReader(new FileReader(fileQuestions.toFile()))) {
            String line;
            line = br.readLine();
                String[] parts = line.split("::");
                System.out.println(parts[0]);
                question.sentence = parts[0];
                question.rightAnswer = parts[1];
                question.multipleChoices = parts[2].split(";");
                question.difficulty = parts[3];
                question.category = parts[4];



        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void acceptConnection(int numberOfConnections) throws IOException {
        Socket clientSocket = serverSocket.accept();
        ClientConnectionHandler clientConnectionHandler =
                new ClientConnectionHandler(clientSocket,
                        Messages.DEFAULT_NAME + numberOfConnections);
        service.submit(clientConnectionHandler);
        //addClient(clientConnectionHandler);
    }

    private void addClient(ClientConnectionHandler clientConnectionHandler) {
        /*synchronized (clients) {
            clients.add(clientConnectionHandler);
        }*/

        clients.add(clientConnectionHandler);
        clientConnectionHandler.send(Messages.WELCOME.formatted(clientConnectionHandler.getName()));
        clientConnectionHandler.send(Messages.COMMANDS_LIST);
        broadcast(clientConnectionHandler.getName(), Messages.CLIENT_ENTERED_CHAT);
    }
    private void playerResponse(ClientConnectionHandler clientConnectionHandler){
        while(clientConnectionHandler.playerAnswer == "") {
            try {
                clientConnectionHandler.playerAnswer.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if(isAnswerRight(clientConnectionHandler.playerAnswer)){
            clients.stream()
                    .forEach(handler -> handler.send(clientConnectionHandler.getName() + ": " + Messages.CORRECT_ANSWER ));
            broadcast(clientConnectionHandler.getName(), clientConnectionHandler.playerAnswer);
        }else {
        clients.stream()
                .forEach(handler -> handler.send(clientConnectionHandler.getName() + ": " + Messages.WRONG_ANSWER));
        broadcast(clientConnectionHandler.getName(), clientConnectionHandler.playerAnswer);
        }
    }

    public void broadcast(String name, String playerAnswer) {
        clients.stream()
                .filter(handler -> !handler.getName().equals(name))
                .forEach(handler -> handler.send(name + ": " + question));



    }
    public void broadcast2(String name, String message) {
        clients.stream()
                .filter(handler -> !handler.getName().equals(name))
                .forEach(handler -> handler.send(name + ": " + question));
    }


    public String listClients() {
        StringBuffer buffer = new StringBuffer();
        clients.forEach(client -> buffer.append(client.getName()).append("\n"));
        return buffer.toString();
    }

    public void removeClient(ClientConnectionHandler clientConnectionHandler) {
        clients.remove(clientConnectionHandler);

    }

    public Optional<ClientConnectionHandler> getClientByName(String name) {
        return clients.stream()
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
            addClient(this);
            try {
                // BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                Scanner in = new Scanner(clientSocket.getInputStream());
                while (in.hasNext()) {
                    message = in.nextLine();
                    if (isCommand(message)) {
                        dealWithCommand(message);
                        continue;
                    }
                    if(message.matches("(?i)[abcd]")){
                        playerAnswer = message;
                        playerResponse(this);
                        return;
                    }
                    if (message.equals("")) {
                        continue;
                    }

                    broadcast(name, message);
                }
            } catch (IOException e) {
                System.err.println(Messages.CLIENT_ERROR + e.getMessage());
            } /*finally {
                removeClient(this);
            }*/
        }

        private boolean isCommand(String message) {
            return message.startsWith("/");
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
    }
    public boolean isAnswerRight(String playerAnswer){
        if(playerAnswer.toLowerCase().equals(question.rightAnswer)){
            //playerAnswer.notifyAll();
            return true;
        }
        return false;
    }


}
