package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Optional;

public class WeatherSensor extends AbstractBehavior<WeatherSensor.WeatherCommand> {

    public interface WeatherCommand {
    }

    public static final class ReadWeather implements WeatherCommand {
        final Optional<Boolean> value;

        public ReadWeather(Optional<Boolean> value) {
            this.value = value;
        }
    }

    private ActorRef<Blinds.BlindsCommand> blinds;

    private WeatherSensor(ActorContext<WeatherCommand> context, ActorRef<Blinds.BlindsCommand> blinds) {
        super(context);
        this.blinds = blinds;
    }

    public static Behavior<WeatherCommand> create(ActorRef<Blinds.BlindsCommand> blinds) {
        return Behaviors.setup(context -> new WeatherSensor(context, blinds));
    }

    @Override
    public Receive<WeatherCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ReadWeather.class, this::onReadWeather)
                .build();
    }

    private Behavior<WeatherCommand> onReadWeather(ReadWeather command) {
        getContext().getLog().info("WeatherSensor says 'IsSunny? = ' {}", command.value.get());
        this.blinds.tell(new Blinds.WeatherChanged(Optional.of(command.value.get())));
        return this;
    }

    private WeatherSensor onPostStop() {
        getContext().getLog().info("WeatherSensor actor stopped");
        return this;
    }
}
