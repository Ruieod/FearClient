package fun.Fear.utils.client.managers.api.command.datatypes;

import fun.Fear.utils.client.managers.api.command.exception.CommandException;

public interface IDatatypePost<T, O> extends IDatatype {
    T apply(IDatatypeContext datatypeContext, O original) throws CommandException;
}
