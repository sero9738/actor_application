package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Optional;

public class FridgeSpaceSensor extends AbstractBehavior<FridgeSpaceSensor.FridgeSpaceSensorCommand> {

    public interface FridgeSpaceSensorCommand {
    }

    public static final class AddSpace implements FridgeSpaceSensorCommand {
        final Optional<Integer> additionalSpace;
        final ActorRef<OrderProcessor.OrderProcessorCommand> orderProcessor;

        public AddSpace(Optional<Integer> additionalSpace, ActorRef<OrderProcessor.OrderProcessorCommand> orderProcessor) {
            this.additionalSpace = additionalSpace;
            this.orderProcessor = orderProcessor;
        }
    }

    public static final class SubSpace implements FridgeSpaceSensorCommand {
        final Optional<Integer> subtractionalSpace;
        final ActorRef<OrderProcessor.OrderProcessorCommand> orderProcessor;

        public SubSpace(Optional<Integer> subtractionalSpace, ActorRef<OrderProcessor.OrderProcessorCommand> orderProcessor) {
            this.subtractionalSpace = subtractionalSpace;
            this.orderProcessor = orderProcessor;
        }
    }

    public static Behavior<FridgeSpaceSensorCommand> create() {
        return Behaviors.setup(context -> new FridgeSpaceSensor(context));
    }

    private Integer space = 0;

    public FridgeSpaceSensor(ActorContext<FridgeSpaceSensorCommand> context) {
        super(context);

        getContext().getLog().info("FridgeSpaceSensor started");
    }

    @Override
    public Receive<FridgeSpaceSensorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(AddSpace.class, this::onAddSpace)
                .onMessage(SubSpace.class, this::onSubSpace)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<FridgeSpaceSensorCommand> onAddSpace(AddSpace c) {
        getContext().getLog().info("Fridge Space increases {}", c.additionalSpace.get());
        space = space + c.additionalSpace.get();
        c.orderProcessor.tell(new OrderProcessor.ResponseFromSpaceSensor(Optional.of(space)));
        return this;
    }

    private Behavior<FridgeSpaceSensorCommand> onSubSpace(SubSpace c) {
        getContext().getLog().info("Fridge Space decreases {}", c.subtractionalSpace.get());
        space = space - c.subtractionalSpace.get();
        c.orderProcessor.tell(new OrderProcessor.ResponseFromSpaceSensor(Optional.of(space)));
        return this;
    }

    private FridgeSpaceSensor onPostStop() {
        getContext().getLog().info("FridgeSpaceSensor actor stopped");
        return this;
    }

}
