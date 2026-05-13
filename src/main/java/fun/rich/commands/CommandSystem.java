package fun.Fear.commands;

import fun.Fear.utils.client.managers.api.command.ICommandSystem;
import fun.Fear.utils.client.managers.api.command.argparser.IArgParserManager;
import fun.Fear.commands.argparser.ArgParserManager;

public enum CommandSystem implements ICommandSystem {
    INSTANCE;

    @Override
    public IArgParserManager getParserManager() {
        return ArgParserManager.INSTANCE;
    }
}
