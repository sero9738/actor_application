package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.TypedActor;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;

import java.time.Duration;
import java.util.Optional;

public class Environment extends AbstractBehavior<Environment.EnvironmentCommand> {

    public interface EnvironmentCommand {}

    public static final class TemperatureChanged implements EnvironmentCommand {
        final Optional<Double> temperature;
        public TemperatureChanged(Optional<Double> temperature) {
            this.temperature = temperature;
        }
    }
    public static final class WeatherChanged implements EnvironmentCommand {
        final Optional<Boolean> isSunny;
        public WeatherChanged(Optional<Boolean> isSunny) {
            this.isSunny = isSunny;
        }
    }

    private final TimerScheduler<EnvironmentCommand> temperatureTimeScheduler;
    private final TimerScheduler<EnvironmentCommand> weatherTimeScheduler;
    private ActorRef<WeatherSensor.WeatherCommand> weatherSensor;
    private ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor;
    private double temperature = 20.0;
    private boolean isSunny = false;
    private boolean riseTemperature = true;

    private Environment(ActorContext<EnvironmentCommand> context, ActorRef<WeatherSensor.WeatherCommand> weatherSensor, ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor, TimerScheduler<EnvironmentCommand> tempTimer, TimerScheduler<EnvironmentCommand> weatherTimer) {
        super(context);
        this.weatherSensor = weatherSensor;
        this.temperatureSensor = temperatureSensor;
        this.temperatureTimeScheduler = tempTimer;
        this.weatherTimeScheduler = weatherTimer;
        this.temperatureTimeScheduler.startTimerAtFixedRate(new TemperatureChanged(Optional.of(temperature)), Duration.ofSeconds(15));
        this.weatherTimeScheduler.startTimerAtFixedRate(new WeatherChanged(Optional.of(isSunny)), Duration.ofSeconds(60));
    }

    public static Behavior<EnvironmentCommand> create (ActorRef<WeatherSensor.WeatherCommand> weatherSensor, ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor){
        return Behaviors.setup(context -> Behaviors.withTimers(timer -> new Environment(context, weatherSensor, temperatureSensor, timer, timer)));
    }

    @Override
    public Receive<EnvironmentCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(TemperatureChanged.class, this::onTemperatureChanged)
                .onMessage(WeatherChanged.class, this::onWeatherChanged)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<EnvironmentCommand> onTemperatureChanged (TemperatureChanged command){

        if (riseTemperature){
            temperature += 1.0;
        }else{
            temperature -= 1.0;
        }

        if (temperature >= 30.0){
            riseTemperature = false;
        }
        if (temperature <= 10.0){
            riseTemperature = true;
        }

        getContext().getLog().info("Environment changed Temperature to {}", temperature);
        this.temperatureSensor.tell(new TemperatureSensor.ReadTemperature(Optional.of(temperature)));
        return this;
    }

    private Behavior<EnvironmentCommand> onWeatherChanged (WeatherChanged command){
        getContext().getLog().info("Environment changed Weather to isSunny = {}", !isSunny);
        isSunny = !isSunny;

        this.weatherSensor.tell(new WeatherSensor.ReadWeather(Optional.of(isSunny)));
        return this;
    }

    private Environment onPostStop() {
        getContext().getLog().info("Environment actor stopped");
        return this;
    }
}
