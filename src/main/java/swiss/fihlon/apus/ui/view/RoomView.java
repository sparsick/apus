/*
 * Apus - A social wall for conferences with additional features.
 * Copyright (C) Marcus Fihlon and the individual contributors to Apus.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package swiss.fihlon.apus.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Svg;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import swiss.fihlon.apus.event.Language;
import swiss.fihlon.apus.event.Room;
import swiss.fihlon.apus.event.RoomStyle;
import swiss.fihlon.apus.event.Session;
import swiss.fihlon.apus.event.Speaker;
import swiss.fihlon.apus.event.Track;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@CssImport(value = "./themes/apus/views/room-view.css")
public final class RoomView extends Div {

    private final @NotNull ZoneId timezone;
    private final transient @NotNull Room room;
    private final @Nullable String title;
    private final transient @NotNull List<Speaker> speakers;
    private final @Nullable ZonedDateTime startTime;
    private final @Nullable ZonedDateTime endTime;
    private final @Nullable Language language;
    private final transient @Nullable Track track;

    private @NotNull RoomStyle roomStyle = RoomStyle.NONE;

    public RoomView(final @NotNull ZoneId timezone,
                    final @NotNull Room room) {
        this(timezone, room, null, List.of(), null, null, null, null);
    }

    public RoomView(final @NotNull ZoneId timezone,
                    final @NotNull Session session) {
        this(
                timezone,
                session.room(),
                session.title(),
                session.speakers(),
                session.startDate(),
                session.endDate(),
                session.language(),
                session.track()
        );
    }

    @SuppressWarnings({ "java:S107", "ParameterNumber" })
    public RoomView(final @NotNull ZoneId timezone,
                    final @NotNull Room room,
                    final @Nullable String title,
                    final @NotNull List<Speaker> speakers,
                    final @Nullable ZonedDateTime startTime,
                    final @Nullable ZonedDateTime endTime,
                    final @Nullable Language language,
                    final @Nullable Track track) {
        this.timezone = timezone;
        this.room = room;
        this.title = title;
        this.speakers = speakers;
        this.startTime = startTime;
        this.endTime = endTime;
        this.language = language;
        this.track = track;

        addClassName("room-view");
        add(createTitleComponent());
        add(createSpeakersComponent());
        add(createRoomComponent());
        add(createTimeComponent());
        add(createImageComponent());
        addClassName(roomStyle.getCssStyle());
    }

    private @NotNull Component createTitleComponent() {
        final var titleComponent = new Div();
        titleComponent.addClassName("title");
        titleComponent.add(new H3(new Text(title == null ? getTranslation("event.room.empty") : title)));
        if (language != null && language != Language.UNKNOWN) {
            final var flagComponent = new Image(language.getFlagFileName(), language.getLanguageCode());
            flagComponent.addClassName("language");
            titleComponent.add(flagComponent);
        }
        return titleComponent;
    }

    private @NotNull Component createSpeakersComponent() {
        final var speakersComponent = new Div();
        speakersComponent.addClassName("speakers");
        if (speakers.isEmpty()) {
            speakersComponent.add(nbsp());
        } else {
            final var joinedSpeakers = speakers.stream()
                    .map(Speaker::fullName)
                    .collect(Collectors.joining(", "));
            speakersComponent.add(
                    new Icon(VaadinIcon.USER),
                    new Span(joinedSpeakers)
            );
        }
        return speakersComponent;
    }

    private @NotNull Component createRoomComponent() {
        final var roomComponent = new Div(
                new Icon(VaadinIcon.LOCATION_ARROW_CIRCLE),
                new Text(room.name())
        );
        roomComponent.addClassName("room");
        return roomComponent;
    }

    private @NotNull Component createTimeComponent() {
        final var timeComponent = new Div();
        timeComponent.addClassName("time");
        final var now = ZonedDateTime.now(timezone).withSecond(59).withNano(999);
        if (startTime == null || endTime == null) { // empty session
            timeComponent.add(nbsp());
            roomStyle = RoomStyle.EMPTY;
        } else if (startTime.isAfter(now)) { // next session
            timeComponent.add(
                    new Icon(VaadinIcon.ALARM),
                    new Text(String.format("%s - %s",
                            startTime.withZoneSameInstant(timezone).toLocalTime(),
                            endTime.withZoneSameInstant(timezone).toLocalTime()))
            );
            roomStyle = RoomStyle.NEXT;
        } else { // running session
            final Duration duration = Duration.between(now, endTime);
            final int minutesLeft = Math.round(duration.toSeconds() / 60f);
            timeComponent.add(new Icon(VaadinIcon.HOURGLASS));
            if (minutesLeft <= 0) {
                timeComponent.add(new Text(getTranslation("event.session.countdown.now")));
            } else if (minutesLeft == 1) {
                timeComponent.add(new Text(getTranslation("event.session.countdown.one-minute")));
            } else {
                timeComponent.add(new Text(getTranslation("event.session.countdown.minutes", minutesLeft)));
            }
            roomStyle = RoomStyle.RUNNING;
        }
        return timeComponent;
    }

    private @NotNull Component createImageComponent() {
        final var speakerAvatars = speakers.stream()
                .filter(speaker -> speaker.imageUrl() != null && !speaker.imageUrl().isBlank())
                .map(speaker -> new Avatar(speaker.fullName(), speaker.imageUrl()))
                .toArray(Avatar[]::new);
        if (speakerAvatars.length == 0) {
            return createTrackComponent();
        }

        final var avatarGroup = new Div();
        avatarGroup.addClassName("avatar-group");
        avatarGroup.add(speakerAvatars);

        final var avatarComponent = new Div();
        avatarComponent.addClassName("avatar");
        avatarComponent.add(avatarGroup);
        return avatarComponent;
    }

    private @NotNull Component createTrackComponent() {
        final var trackComponent = new Div();
        trackComponent.addClassName("track");
        if (track != null && !track.equals(Track.NONE)) {
            final var trackImage = new Svg(track.svgCode());
            trackComponent.add(trackImage);
        }
        return trackComponent;
    }

    private static @NotNull Component nbsp() {
        return new Html("<span>&nbsp;</span>");
    }

    public @NotNull RoomStyle getRoomStyle() {
        return roomStyle;
    }
}
