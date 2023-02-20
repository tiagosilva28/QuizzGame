package game;

import java.io.*;
import java.net.Socket;

public class Player {

    /**
     * The Player class represents a client that connects to a server using sockets and allows the user to send messages to the server
     * through the console.
     */

    /**
     * The main() method instantiates the Player
     * class and starts the player by calling the start() method.
     */

    public static void main(String[] args) {
        Player player = new Player();
        try {
            player.start("localhost", 8095);
        } catch (IOException e) {
            System.out.println("Connection closed");
        }
    }

    /**
     * The start() method establishes a socket connection with the server and creates a
     * BufferedReader and BufferedWriter to read and write messages to the server respectively.
     */
    private void start(String host, int port) throws IOException {
        Socket socket = new Socket(host, port);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        new Thread(new KeyboardHandler(out, socket)).start();
        String line;
        while ((line = in.readLine()) != null) {
            System.out.println(line);
        }
        socket.close();
    }

    private class KeyboardHandler implements Runnable {
        private BufferedWriter out;
        private Socket socket;
        private BufferedReader in;

        public KeyboardHandler(BufferedWriter out, Socket socket) {
            this.out = out;
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(System.in));
        }

        @Override
        public void run() {

            while (!socket.isClosed()) {
                try {
                    String line = in.readLine();

                    out.write(line);
                    out.newLine();
                    out.flush();

                    if (line.equals("/quit")) {
                        socket.close();
                        System.exit(0);
                    }

                } catch (IOException e) {
                    System.out.println("Something went wrong with the server. Connection closing...");
                    try {
                        socket.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }


}
