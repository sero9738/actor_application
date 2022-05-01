package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.Entities.Order;
import at.fhv.sysarch.lab2.homeautomation.Entities.Product;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class Fridge extends AbstractBehavior<Fridge.FridgeCommand> {

    public interface FridgeCommand {
    }

    public static final class ConsumeProduct implements FridgeCommand {
        final Optional<Product> product;

        public ConsumeProduct(Optional<Product> product) {
            this.product = product;
        }
    }

    public static final class OrderProduct implements FridgeCommand {
        final Optional<Product> product;

        public OrderProduct(Optional<Product> product) {
            this.product = product;
        }
    }

    public static final class ShowContent implements FridgeCommand {
    }

    public static final class ShowHistory implements FridgeCommand {
    }

    public static final class WeightCommand implements FridgeCommand {
        final Optional<Double> weight;

        public WeightCommand(Optional<Double> weight) {
            this.weight = weight;
        }
    }

    public static final class SpaceCommand implements FridgeCommand {
        final Optional<Integer> space;

        public SpaceCommand(Optional<Integer> space) {
            this.space = space;
        }
    }

    public static final class OrderResponseSpace implements FridgeCommand {
        final Optional<Integer> space;

        public OrderResponseSpace(Optional<Integer> space) {
            this.space = space;
        }
    }

    public static final class OrderResponseWeight implements FridgeCommand {
        final Optional<Double> weight;

        public OrderResponseWeight(Optional<Double> weight) {
            this.weight = weight;
        }
    }

    private ActorRef<FridgeWeightSensor.FridgeWeightSensorCommand> fridgeWeightSensor;
    private ActorRef<FridgeSpaceSensor.FridgeSpaceSensorCommand> fridgeSpaceSensor;

    private List<Product> products;
    private List<Order> history;
    private Double currentLoad;
    private final Double weightLimit = 10.0;
    private int currentSpace;
    private final int spaceLimit = 20;

    private ActorRef<OrderProcessor.OrderProcessorCommand> orderProcessor;
    boolean terminate = false;

    private Fridge(ActorContext<FridgeCommand> context) {
        super(context);
        this.fridgeWeightSensor = getContext().spawn(FridgeWeightSensor.create(), "fridgeWeightSensor");
        this.fridgeSpaceSensor = getContext().spawn(FridgeSpaceSensor.create(), "fridgeSpaceSensor");
        products = new LinkedList<>();
        history = new LinkedList<>();
        currentLoad = 0.0;
        currentSpace = 0;
    }

    public static Behavior<FridgeCommand> create() {
        return Behaviors.setup(context -> new Fridge(context));
    }

    @Override
    public Receive<FridgeCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ConsumeProduct.class, this::onConsumeProduct)
                .onMessage(OrderProduct.class, this::onOrderProduct)
                .onMessage(ShowContent.class, this::onShowContent)
                .onMessage(ShowHistory.class, this::onShowHistory)
                .onMessage(WeightCommand.class, this::onReadWeight)
                .onMessage(SpaceCommand.class, this::onReadSpace)
                .onMessage(OrderResponseWeight.class, this::onOrderResponseWeight)
                .onMessage(OrderResponseSpace.class, this::onOrderResponseSpace)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<FridgeCommand> onConsumeProduct(ConsumeProduct command) {
        if (this.products.isEmpty()) {
            getContext().getLog().info("Fridge is empty");
        } else {
            Product p = command.product.get();
            if (this.products.contains(p)) {
                products.remove(p);
                getContext().getLog().info("Consumed Product: '" + p.getProductname() + "'");
                orderProcessor = getContext().spawn(OrderProcessor.create(fridgeSpaceSensor, fridgeWeightSensor, this.getContext().getSelf()), "orderProcessorOrder");
                orderProcessor.tell(new OrderProcessor.RemoveFromFridge(Optional.of(p)));
            } else {
                getContext().getLog().info("Info: The is no Product '" + p.getProductname() + "' in the fridge!");
            }
        }
        return this;
    }

    private Behavior<FridgeCommand> onOrderProduct(OrderProduct command) {
        Order o = new Order(command.product.get());
        Product p = command.product.get();
        if ((p.getWeight() + currentLoad) <= weightLimit && (currentSpace + 1) <= spaceLimit) {
            products.add(command.product.get());
            history.add(o);
            getContext().getLog().info(o.toString());

            orderProcessor = getContext().spawn(OrderProcessor.create(fridgeSpaceSensor, fridgeWeightSensor, this.getContext().getSelf()), "orderProcessorOrder");
            orderProcessor.tell(new OrderProcessor.OrderForFridge(Optional.of(p)));
        } else {
            getContext().getLog().info("Order denied. Fridge weight or space limit exceeded");
        }
        return this;
    }

    private Behavior<FridgeCommand> onShowContent(ShowContent command) {
        getContext().getLog().info("Fridge contains:");
        for (Product p : products) {
            getContext().getLog().info(p.getProductname());
        }
        return this;
    }

    private Behavior<FridgeCommand> onShowHistory(ShowHistory command) {
        getContext().getLog().info("Fridge History:");
        for (Order o : history) {
            getContext().getLog().info(o.toString());
        }
        return this;
    }

    public Behavior<FridgeCommand> onReadWeight(WeightCommand c) {
        getContext().getLog().info("Fridge gets new Weight {}", c.weight.get());
        this.currentLoad = c.weight.get();
        return this;
    }

    public Behavior<FridgeCommand> onReadSpace(SpaceCommand c) {
        getContext().getLog().info("Fridge gets new Space {}", c.space.get());
        this.currentSpace = c.space.get();
        return this;
    }

    public Behavior<FridgeCommand> onOrderResponseSpace(OrderResponseSpace c) {
        getContext().getLog().info("Fridge gets new Space {}", c.space.get());
        this.currentSpace = c.space.get();
        if (terminate) {
            orderProcessor.tell(new OrderProcessor.Shutdown());
            terminate = false;
        } else {
            terminate = true;
        }
        return this;
    }

    public Behavior<FridgeCommand> onOrderResponseWeight(OrderResponseWeight c) {
        getContext().getLog().info("Fridge gets new Weight {}", c.weight.get());
        this.currentLoad = c.weight.get();
        if (terminate) {
            orderProcessor.tell(new OrderProcessor.Shutdown());
            terminate = false;
        } else {
            terminate = true;
        }
        return this;
    }

    private Fridge onPostStop() {
        getContext().getLog().info("Fridge actor stopped");
        return this;
    }
}
