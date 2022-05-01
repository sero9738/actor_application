package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Optional;

public class FridgeWeightSensor extends AbstractBehavior<FridgeWeightSensor.FridgeWeightSensorCommand> {

    public interface FridgeWeightSensorCommand {
    }

    public static final class AddWeight implements FridgeWeightSensorCommand {
        final Optional<Double> productWeight;
        final ActorRef<OrderProcessor.OrderProcessorCommand> orderProcessor;

        public AddWeight(Optional<Double> productWeight, ActorRef<OrderProcessor.OrderProcessorCommand> orderProcessor) {
            this.productWeight = productWeight;
            this.orderProcessor = orderProcessor;
        }
    }

    public static final class SubWeight implements FridgeWeightSensorCommand {
        final Optional<Double> productWeight;
        final ActorRef<OrderProcessor.OrderProcessorCommand> orderProcessor;

        public SubWeight(Optional<Double> productWeight, ActorRef<OrderProcessor.OrderProcessorCommand> orderProcessor) {
            this.productWeight = productWeight;
            this.orderProcessor = orderProcessor;
        }
    }

    private Double weight = 0.0;

    public static Behavior<FridgeWeightSensorCommand> create() {
        return Behaviors.setup(context -> new FridgeWeightSensor(context));
    }

    public FridgeWeightSensor(ActorContext<FridgeWeightSensorCommand> context) {
        super(context);
    }

    @Override
    public Receive<FridgeWeightSensorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(AddWeight.class, this::onAddWeight)
                .onMessage(SubWeight.class, this::onSubWeight)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    public Behavior<FridgeWeightSensorCommand> onAddWeight(AddWeight c) {
        getContext().getLog().info("Fridge Weight increases {}", c.productWeight.get());
        weight = this.weight + c.productWeight.get();
        c.orderProcessor.tell(new OrderProcessor.ResponseFromWeightSensor(Optional.of(weight)));
        return this;
    }

    public Behavior<FridgeWeightSensorCommand> onSubWeight(SubWeight c) {
        getContext().getLog().info("Fridge Weight decreases {}", c.productWeight.get());
        weight = this.weight - c.productWeight.get();
        c.orderProcessor.tell(new OrderProcessor.ResponseFromWeightSensor(Optional.of(weight)));
        return this;
    }

    private FridgeWeightSensor onPostStop() {
        getContext().getLog().info("FridgeWeightSensor actor stopped");
        return this;
    }
}
