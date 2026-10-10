package fun.dungeonbot.screen;

import fun.dungeonbot.bot.DungeonBot;
import fun.dungeonbot.config.BotConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class BotScreen extends Screen {
    private static final int field_width = 240;
    private static final int row_height = 36;

    private TextFieldWidget rtpField;
    private TextFieldWidget homeField;
    private TextFieldWidget itemsField;
    private TextFieldWidget keywordsField;
    private TextFieldWidget radiusField;
    private ButtonWidget toggleButton;

    public BotScreen() {
        super(Text.literal("DungeonBot"));
    }

    @Override
    protected void init() {
        BotConfig config = BotConfig.get();
        int left = width / 2 - field_width / 2;
        int top = 44;

        rtpField = addField(left, top, config.rtpCommand);
        homeField = addField(left, top + row_height, config.homeCommand);
        itemsField = addField(left, top + row_height * 2, String.join(", ", config.itemIds));
        keywordsField = addField(left, top + row_height * 3, String.join(", ", config.nameKeywords));
        radiusField = addField(left, top + row_height * 4, String.valueOf(config.depositRadius));

        int buttonsTop = top + row_height * 5;
        int halfWidth = field_width / 2 - 2;

        toggleButton = addDrawableChild(ButtonWidget.builder(toggleLabel(), button -> {
            applyFields();
            DungeonBot.get().toggle();
            button.setMessage(toggleLabel());
        }).dimensions(left, buttonsTop, halfWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Сохранить"), button -> applyFields()).dimensions(left + halfWidth + 4, buttonsTop, halfWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Забыть пройденные сундуки"), button -> DungeonBot.get().forgetLooted()).dimensions(left, buttonsTop + 24, field_width, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int left = width / 2 - field_width / 2;
        int top = 44;

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 16, 0x55FFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Команда RTP"), left, top - 11, 0xA0A0A0);
        context.drawTextWithShadow(textRenderer, Text.literal("Команда дома"), left, top + row_height - 11, 0xA0A0A0);
        context.drawTextWithShadow(textRenderer, Text.literal("ID предметов (через запятую)"), left, top + row_height * 2 - 11, 0xA0A0A0);
        context.drawTextWithShadow(textRenderer, Text.literal("Слова в названии (через запятую)"), left, top + row_height * 3 - 11, 0xA0A0A0);
        context.drawTextWithShadow(textRenderer, Text.literal("Радиус сундуков дома"), left, top + row_height * 4 - 11, 0xA0A0A0);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(DungeonBot.get().status()), width / 2, height - 20, 0xFFFFFF);
    }

    @Override
    public void tick() {
        toggleButton.setMessage(toggleLabel());
    }

    @Override
    public void close() {
        applyFields();
        super.close();
    }

    private TextFieldWidget addField(int x, int y, String value) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, field_width, 18, Text.empty());
        field.setMaxLength(512);
        field.setText(value);
        return addDrawableChild(field);
    }

    private void applyFields() {
        BotConfig config = BotConfig.get();
        config.rtpCommand = rtpField.getText().trim();
        config.homeCommand = homeField.getText().trim();
        config.itemIds = splitList(itemsField.getText());
        config.nameKeywords = splitList(keywordsField.getText());

        try {
            config.depositRadius = Math.max(1, Integer.parseInt(radiusField.getText().trim()));
        } catch (NumberFormatException ignored) {
        }

        BotConfig.save();
    }

    private static List<String> splitList(String text) {
        return Arrays.stream(text.split(",")).map(String::trim).filter(entry -> !entry.isEmpty()).collect(Collectors.toCollection(ArrayList::new));
    }

    private static Text toggleLabel() {
        return Text.literal(DungeonBot.get().isRunning() ? "Стоп" : "Старт");
    }
}
