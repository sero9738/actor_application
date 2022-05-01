package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.Entities.Product;

import java.util.Optional;

public class OrderProcessor extends AbstractBehavior<OrderProcessor.OrderProcessorCommand> {

    public interface OrderProcessorCommand {
    }

    public static final class OrderForFridge implements OrderProcessorCommand {
        final Optional<Product> product;

        public OrderForFridge(Optional<Product> product) {
            this.product = product;
        }
    }

    public static final class RemoveFromFridge implements OrderProcessorCommand {
        final Optional<Product> product;

        public RemoveFromFridge(Optional<Product> product) {
            this.product = product;
        }
    }

    public static final class ResponseFromSpaceSensor implements OrderProcessorCommand {
        final Optional<Integer> space;

        public ResponseFromSpaceSensor(Optional<Integer> space) {
            this.space = space;
        }
    }

    public static final class ResponseFromWeightSensor implements OrderProcessorCommand {
        final Optional<Double> weight;

        public ResponseFromWeightSensor(Optional<Double> weight) {
            this.weight = weight;
        }
    }

    public static final class Shutdown implements OrderProcessorCommand {
    }

    private ActorRef<FridgeSpaceSensor.FridgeSpaceSensorCommand> fridgeSpaceSensor;
    private ActorRef<FridgeWeightSensor.FridgeWeightSensorCommand> fridgeWeightSensor;
    private ActorRef<Fridge.FridgeCommand> fridge;

    public static Behavior<OrderProcessorCommand> create(ActorRef<FridgeSpaceSensor.FridgeSpaceSensorCommand> fridgeSpaceSensor, ActorRef<FridgeWeightSensor.FridgeWeightSensorCommand> fridgeWeightSensor, ActorRef<Fridge.FridgeCommand> fridge) {
        return Behaviors.setup(context -> new OrderProcessor(context, fridgeSpaceSensor, fridgeWeightSensor, fridge));
    }

    public OrderProcessor(ActorContext<OrderProcessorCommand> context, ActorRef<FridgeSpaceSensor.FridgeSpaceSensorCommand> fridgeSpaceSensor, ActorRef<FridgeWeightSensor.FridgeWeightSensorCommand> fridgeWeightSensor, ActorRef<Fridge.FridgeCommand> fridge) {
        super(context);
        this.fridgeSpaceSensor = fridgeSpaceSensor;
        this.fridgeWeightSensor = fridgeWeightSensor;
        this.fridge = fridge;
        getContext().getLog().info("OrderProcessor started");
    }

    @Override
    public Receive<OrderProcessorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(OrderForFridge.class, this::onOrder)
                .onMessage(RemoveFromFridge.class, this::onRemove)
                .onMessage(ResponseFromSpaceSensor.class, this::onResponseSpace)
                .onMessage(ResponseFromWeightSensor.class, this::onResponseWeight)
                .onMessage(Shutdown.class, message -> shutdown())
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<OrderProcessorCommand> onOrder(OrderForFridge c) {
        Optional<Product> product = c.product;
        Optional<Double> weight = Optional.of(product.get().getWeight());
        this.fridgeSpaceSensor.tell(new FridgeSpaceSensor.AddSpace(Optional.of(Integer.valueOf(1)), this.getContext().getSelf()));
        this.fridgeWeightSensor.tell(new FridgeWeightSensor.AddWeight(weight, this.getContext().getSelf()));
        return this;
    }

    private Behavior<OrderProcessorCommand> onRemove(RemoveFromFridge c) {
        Optional<Product> product = c.product;
        Optional<Double> weight = Optional.of(product.get().getWeight());
        this.fridgeSpaceSensor.tell(new FridgeSpaceSensor.SubSpace(Optional.of(Integer.valueOf(1)), this.getContext().getSelf()));
        this.fridgeWeightSensor.tell(new FridgeWeightSensor.SubWeight(weight, this.getContext().getSelf()));
        return this;
    }

    private Behavior<OrderProcessorCommand> onResponseSpace(ResponseFromSpaceSensor c) {
        this.fridge.tell(new Fridge.OrderResponseSpace(c.space));
        return this;
    }

    private Behavior<OrderProcessorCommand> onResponseWeight(ResponseFromWeightSensor c) {
        this.fridge.tell(new Fridge.OrderResponseWeight(c.weight));
        return this;
    }

    private Behavior<OrderProcessorCommand> shutdown() {
        getContext().getSystem().log().info("Kill Actor Process Order");
        return Behaviors.stopped();
    }

    private OrderProcessor onPostStop() {
        getContext().getLog().info("OrderProcessor actor stopped");
        return this;
    }
}
