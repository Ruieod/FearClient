package fun.Fear.utils.client.managers.api.command.datatypes;

import fun.Fear.utils.client.managers.api.command.exception.CommandException;
import fun.Fear.utils.display.interfaces.QuickImports;

import java.util.stream.Stream;

public interface IDatatype extends QuickImports {
    Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException;
}
