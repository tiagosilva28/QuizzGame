# QuizzGame
QuizzGame is a game that allows 2 players to compete against each other in a general knowledge game.

The Quiz class maintains a List of PlayerController instances, which represent the players currently playing the game.

When the game is started via the start method, a ServerSocket is opened on the specified port, and the server enters an infinite loop that listens for incoming client connections via the acceptConnection method. For each incoming connection, a new PlayerController instance is created, and the instance is added to the players list. The PlayerController instance is then submitted to an ExecutorService to run on its own thread.

The PlayerController class is responsible for handling player input and output. When a new PlayerController instance is created, the player is welcomed to the game and is provided with a list of commands they can use during the game. The player is then prompted to enter their name. The PlayerController class then listens for input from the player and responds accordingly. If the player inputs an answer to a question, the checkAnswer method is called to determine whether the answer is correct, and the player's score is updated accordingly. If the player inputs the *next command, the nextQuestion method is called, which moves the game to the next question.

The Quiz class maintains a Queue of Question instances, which are read from a text file in the splitAndCreateQuestionsQueue method. The sendQuestion method is responsible for sending the current question to the player.

The checkWinner method is responsible for determining whether the game has ended and declaring a winner. If all players have answered 10 questions, the highest-scoring player is declared the winner.
