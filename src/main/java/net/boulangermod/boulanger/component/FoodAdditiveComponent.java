package net.boulangermod.boulanger.component;

public class FoodAdditiveComponent {
    private final String id;
    private final float weight;

    public FoodAdditiveComponent(String id, float weight) {
        this.id = id;
        this.weight = weight;
    }

    // Optionally, add getters or additional methods if needed
    public String getId() {
        return id;
    }

    public float getWeight() {
        return weight;
    }
}
