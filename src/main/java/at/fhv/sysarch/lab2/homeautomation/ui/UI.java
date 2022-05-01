package at.fhv.sysarch.lab2.homeautomation.ui;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.Entities.Product;
import at.fhv.sysarch.lab2.homeautomation.devices.*;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Scanner;

public class UI extends AbstractBehavior<Void> {

    private ActorRef<TemperatureSensor.TemperatureCommand> tempSensor;
    private ActorRef<AirCondition.AirConditionCommand> airCondition;
    private ActorRef<WeatherSensor.WeatherCommand> weatherSensor;
    private ActorRef<MediaStation.MediaStationCommand> mediaStation;
    private ActorRef<Fridge.FridgeCommand> fridge;

    public static Behavior<Void> create(ActorRef<TemperatureSensor.TemperatureCommand> tempSensor, ActorRef<AirCondition.AirConditionCommand> airCondition, ActorRef<WeatherSensor.WeatherCommand> weatherSensor, ActorRef<MediaStation.MediaStationCommand> mediaStation, ActorRef<Fridge.FridgeCommand> fridge) {
        return Behaviors.setup(context -> new UI(context, tempSensor, airCondition, weatherSensor, mediaStation, fridge));
    }

    private  UI(ActorContext<Void> context, ActorRef<TemperatureSensor.TemperatureCommand> tempSensor, ActorRef<AirCondition.AirConditionCommand> airCondition, ActorRef<WeatherSensor.WeatherCommand> weatherSensor, ActorRef<MediaStation.MediaStationCommand> mediaStation, ActorRef<Fridge.FridgeCommand> fridge) {
        super(context);
        // TODO: implement actor and behavior as needed
        // TODO: move UI initialization to appropriate place
        this.airCondition = airCondition;
        this.tempSensor = tempSensor;
        this.weatherSensor = weatherSensor;
        this.mediaStation = mediaStation;
        this.fridge = fridge;

        new Thread(() -> { this.runCommandLine(); }).start();

        getContext().getLog().info("UI started");
    }

    @Override
    public Receive<Void> createReceive() {
        return newReceiveBuilder().onSignal(PostStop.class, signal -> onPostStop()).build();
    }

    private UI onPostStop() {
        getContext().getLog().info("UI stopped");
        return this;
    }

    public void runCommandLine() {
        // TODO: Create Actor for UI Input-Handling
        Scanner scanner = new Scanner(System.in);
        String[] input = null;
        String reader = "";

        while (!reader.equalsIgnoreCase("quit") && scanner.hasNextLine()) {
            reader = scanner.nextLine();
            // TODO: change input handling
            String[] command = reader.split(" ");
            if(command[0].equals("t")) {
                this.tempSensor.tell(new TemperatureSensor.ReadTemperature(Optional.of(Double.valueOf(command[1]))));
            }
            if(command[0].equals("a")) {
                this.airCondition.tell(new AirCondition.PowerAirCondition(Optional.of(Boolean.valueOf(command[1]))));
            }
            if(command[0].equals("w")) {
                this.weatherSensor.tell(new WeatherSensor.ReadWeather(Optional.of(Boolean.valueOf(command[1]))));
            }
            if(command[0].equals("m")) {
                this.mediaStation.tell(new MediaStation.PowerMediaStation(Optional.of(Boolean.valueOf(command[1]))));
            }
            if(command[0].equals("movie")) {
                this.mediaStation.tell(new MediaStation.StartMovie(Optional.of(true)));
            }
            if(command[0].equals("fo")) {
                Product p = new Product(command[1], 3.0);
                this.fridge.tell(new Fridge.OrderProduct(Optional.of(p)));
            }
            if(command[0].equals("fc")) {
                Product p = new Product(command[1], 3.0);
                this.fridge.tell(new Fridge.ConsumeProduct(Optional.of(p)));
            }
            if(command[0].equals("fsh")){
                this.fridge.tell(new Fridge.ShowHistory());
            }
            if(command[0].equals("fsc")){
                this.fridge.tell(new Fridge.ShowContent());
            }
        }
        getContext().getLog().info("UI done");
    }
}
