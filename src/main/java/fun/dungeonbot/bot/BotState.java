package fun.dungeonbot.bot;

public enum BotState {
    idle("выключен"),
    cooldown("ожидание"),
    teleporting("телепорт"),
    settling("прогрузка"),
    scanning("поиск данжей"),
    exploring("брожу и ищу"),
    next_container("выбор сундука"),
    walking("иду к сундуку"),
    opening("открываю"),
    transferring("перекладываю");

    public final String label;

    BotState(String label) {
        this.label = label;
    }
}
