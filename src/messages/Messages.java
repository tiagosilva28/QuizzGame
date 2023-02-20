package messages;

public abstract class Messages {
    public static final String SERVER_STARTED = "Server started on port: %s";
    public static final String DEFAULT_NAME = "CLIENT-";
    public static final String NO_SUCH_COMMAND = "⚠️ Invalid command!";
    public static final String COMMANDS_LIST = """
            List of the game commands:
            /list -> gets you the list of connected players
            *next -> picks you a random question
            *easy -> picks you a random easy question
            *medium -> picks you a random medium question
            *hard -> picks you a random hard question
            /name <new name> -> lets you change your userName
            /quit -> exits the server\n""";
    public static final String CLIENT_DISCONNECTED = " left the chat.";
    public static final String WHISPER_INSTRUCTIONS = "Invalid whisper use. Correct use: '/whisper <username> <message>";
    public static final String NO_SUCH_CLIENT = "The client you want to whisper to doesn't exists.";
    public static final String WHISPER = "(whisper)";
    public static final String WELCOME = " " +
            "\n" +
            "#######  ##     ## #### ########     ######      ###    ##     ## ######## \n" +
            "##     ## ##     ##  ##       ##     ##    ##    ## ##   ###   ### ##       \n" +
            "##     ## ##     ##  ##      ##      ##         ##   ##  #### #### ##       \n" +
            "##     ## ##     ##  ##     ##       ##   #### ##     ## ## ### ## ######   \n" +
            "##  ## ## ##     ##  ##    ##        ##    ##  ######### ##     ## ##       \n" +
            "##    ##  ##     ##  ##   ##         ##    ##  ##     ## ##     ## ##       \n" +
            " ##### ##  #######  #### ########     ######   ##     ## ##     ## ######## \n \n";
    public static final String CLIENT_ERROR = "Something went wrong with this client's connection. Error: ";
    public static final String CLIENT_ALREADY_EXISTS = "A client with this name already exists. Please choose another one.";
    public static final String SELF_NAME_CHANGED = "You changed your name to: %s";
    public static final String NAME_CHANGED = "%s changed name to: %s";
    public static final String CORRECT_ANSWER = "Congrats! Your answer is correct!";
    public static final String WRONG_ANSWER = "Wrong answer.";
    public static final String NEXT_QUESTION = "*next - random question ||" +
            " *easy - easy question || " +
            " *medium - medium question || " +
            " *hard - hard question";
    public static final String GAME_OVER =
            "\n" +
                    "..######......###....##.....##.########.....#######..##.....##.########.########.\n" +
                    ".##....##....##.##...###...###.##..........##.....##.##.....##.##.......##.....##\n" +
                    ".##.........##...##..####.####.##..........##.....##.##.....##.##.......##.....##\n" +
                    ".##...####.##.....##.##.###.##.######......##.....##.##.....##.######...########.\n" +
                    ".##....##..#########.##.....##.##..........##.....##..##...##..##.......##...##..\n" +
                    ".##....##..##.....##.##.....##.##..........##.....##...##.##...##.......##....##.\n" +
                    "..######...##.....##.##.....##.########.....#######.....###....########.##.....##";

}
