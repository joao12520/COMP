package pt.up.fe.comp2023.jasmin;

public class StackManager {
    private int stackCounter;
    private int stackLimit;

    public StackManager() {
        this.stackCounter = 0;
        this.stackLimit = 0;
    }

    public int getStackLimit() {
        return this.stackLimit;
    }

    public void decStackCounter() {
        this.stackCounter--;
    }

    public void decStackCounter(int value) {
        this.stackCounter -= value;
    }

    public void incStackCounter() {
        this.stackCounter++;
        if (this.stackCounter > this.stackLimit) {
            this.stackLimit = this.stackCounter;
        }
    }

}
