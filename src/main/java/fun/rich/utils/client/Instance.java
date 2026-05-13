package fun.Fear.utils.client;

import lombok.experimental.UtilityClass;
import fun.Fear.utils.client.managers.api.draggable.AbstractDraggable;
import fun.Fear.features.module.Module;
import fun.Fear.Fear;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@UtilityClass
public class Instance {
    private final ConcurrentMap<Class<? extends Module>, Module> instanceModules = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<? extends AbstractDraggable>, AbstractDraggable> instanceDraggables = new ConcurrentHashMap<>();

    public <T extends Module> T get(Class<T> clazz) {
        return clazz.cast(instanceModules.computeIfAbsent(clazz, instance -> Fear.getInstance().getModuleProvider().get(instance)));
    }

    public <T extends Module> T get(String module) {
        return Fear.getInstance().getModuleProvider().get(module);
    }

    public <T extends AbstractDraggable> T getDraggable(Class<T> clazz) {
        return clazz.cast(instanceDraggables.computeIfAbsent(clazz, instance -> Fear.getInstance().getDraggableRepository().get(instance)));
    }

    public <T extends AbstractDraggable> T getDraggable(String draggable) {
        return Fear.getInstance().getDraggableRepository().get(draggable);
    }
}
