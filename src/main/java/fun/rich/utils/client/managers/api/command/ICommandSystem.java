package fun.Fear.utils.client.managers.api.command;

import fun.Fear.utils.client.managers.api.command.argparser.IArgParserManager;

public interface ICommandSystem {
    IArgParserManager getParserManager();
}
