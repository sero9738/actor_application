package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Optional;

public class Blinds extends AbstractBehavior<Blinds.BlindsCommand> {

    public interface BlindsCommand {
    }

    public static final class WeatherChanged implements BlindsCommand {
        final Optional<Boolean> value;

        public WeatherChanged(Optional<Boolean> value) {
            this.value = value;
        }
    }

    public static final class MovieChanged implements BlindsCommand {
        final Optional<Boolean> value;

        public MovieChanged(Optional<Boolean> value) {
            this.value = value;
        }
    }

    private boolean isClosed = false;
    private boolean isSunny = false;
    private boolean isMovieRunning = false;

    private Blinds(ActorContext<BlindsCommand> context) {
        super(context);
    }

    public static Behavior<BlindsCommand> create() {
        return Behaviors.setup(context -> new Blinds(context));
    }

    @Override
    public Receive<BlindsCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(WeatherChanged.class, this::onWeatherChanged)
                .onMessage(MovieChanged.class, this::onMovieChanged)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<BlindsCommand> onWeatherChanged(WeatherChanged command) {
        isSunny = command.value.get();
        DecideBehavior();
        return this;
    }

    private Behavior<BlindsCommand> onMovieChanged(MovieChanged command) {
        isMovieRunning = command.value.get();
        DecideBehavior();
        return this;
    }

    private void DecideBehavior(){
        if (!isSunny && !isMovieRunning && isClosed){
            OpeningBlinds();
        }else if (isSunny || isMovieRunning && !isClosed){
            ClosingBlinds();
        }
    }

    private void OpeningBlinds(){
        getContext().getLog().info("Blinds are opening");
        isClosed = false;
    }

    private void ClosingBlinds(){
        getContext().getLog().info("Blinds are closing");
        isClosed = true;
    }

    private Blinds onPostStop() {
        getContext().getLog().info("Blinds actor stopped");
        return this;
    }
}
