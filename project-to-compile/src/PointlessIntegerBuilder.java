public class PointlessIntegerBuilder {
    private int pointlessInteger;

    public PointlessIntegerBuilder foo(int newValue) {
        pointlessInteger = newValue;
        return this;
    }

    public int build() {
        return pointlessInteger;
    }
}
