package game;

public enum DifficultyLevels {

    EASY("easy",10),
    MEDIUM("medium", 30),
    HARD("hard",50);

    private String level;
    private int score;

    DifficultyLevels(String level, int score) {
        this.level = level;
        this.score = score;
    }

    public String getLevel() {
        return level;
    }

    public int getScore() {
        return score;
    }
}
