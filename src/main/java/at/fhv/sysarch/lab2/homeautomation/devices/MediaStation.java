package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Optional;

public class MediaStation extends AbstractBehavior<MediaStation.MediaStationCommand> {

    public interface MediaStationCommand {
    }

    public static final class PowerMediaStation implements MediaStationCommand {
        final Optional<Boolean> value;

        public PowerMediaStation(Optional<Boolean> value) {
            this.value = value;
        }
    }

    public static final class StartMovie implements MediaStationCommand {
        final Optional<Boolean> value;

        public StartMovie(Optional<Boolean> value) {
            this.value = value;
        }
    }

    private ActorRef<Blinds.BlindsCommand> blinds;
    private boolean poweredOn = false;
    private boolean moviePlaying = false;

    private MediaStation(ActorContext<MediaStationCommand> context, ActorRef<Blinds.BlindsCommand> blinds) {
        super(context);
        this.blinds = blinds;
    }

    public static Behavior<MediaStationCommand> create(ActorRef<Blinds.BlindsCommand> blinds) {
        return Behaviors.setup(context -> new MediaStation(context, blinds));
    }

    @Override
    public Receive<MediaStationCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(PowerMediaStation.class, this::onPowerMediaStationOn)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<MediaStationCommand> onPowerMediaStationOn(PowerMediaStation r) {
        getContext().getLog().info("Turning Power of MediaStation to {}", r.value.get());
        if (r.value.get() == true) {
            this.poweredOn = true;
            return Behaviors.receive(MediaStationCommand.class)
                    .onMessage(PowerMediaStation.class, this::onPowerMediaStationOff)
                    .onMessage(StartMovie.class, this::onStartMovie)
                    .onSignal(PostStop.class, signal -> onPostStop())
                    .build();
        }
        return this;
    }

    private Behavior<MediaStationCommand> onPowerMediaStationOff(PowerMediaStation r) {
        getContext().getLog().info("Turning Power of MediaStation to {}", r.value.get());
        if (r.value.get() == false) {
            this.poweredOn = false;
            this.blinds.tell(new Blinds.MovieChanged(Optional.of(false)));
            return Behaviors.receive(MediaStationCommand.class)
                    .onMessage(PowerMediaStation.class, this::onPowerMediaStationOn)
                    .onSignal(PostStop.class, signal -> onPostStop())
                    .build();
        }
        return this;
    }

    private Behavior<MediaStationCommand> onStartMovie(StartMovie r) {
        getContext().getLog().info("MediaStation starting Movie");
        if (r.value.get() == true) {
            this.moviePlaying = true;
            this.blinds.tell(new Blinds.MovieChanged(Optional.of(true)));
            return Behaviors.receive(MediaStationCommand.class)
                    .onMessage(PowerMediaStation.class, this::onPowerMediaStationOff)
                    .onSignal(PostStop.class, signal -> onPostStop())
                    .build();
        }
        return this;
    }

    private MediaStation onPostStop() {
        getContext().getLog().info("MediaStation actor stopped");
        return this;
    }
}
