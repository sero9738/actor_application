package at.fhv.sysarch.lab2.homeautomation.Entities;

import java.util.Objects;

public class Product {

    private String productName;
    private double weight;

    public Product(String productName, double weight) {
        this.productName = productName;
        this.weight = weight;
    }

    public String getProductname() {
        return productName;
    }

    public double getWeight() {
        return weight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Double.compare(product.weight, weight) == 0 && Objects.equals(productName, product.productName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productName, weight);
    }
}
