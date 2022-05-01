package at.fhv.sysarch.lab2.homeautomation.Entities;

import java.time.LocalDateTime;

public class Order {

    private Product product;
    private LocalDateTime localDateTime;

    public Order(Product product) {
        this.product = product;
        this.localDateTime = LocalDateTime.now();
    }

    public String toString() {
        return "Product '" + product.toString() + "' ordered on '" + localDateTime.toString() + "'";
    }

}
