package fun.Fear.events.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import fun.Fear.utils.client.managers.event.events.Event;

@Getter
@AllArgsConstructor
public class RotationUpdateEvent implements Event {
    byte type;
}
