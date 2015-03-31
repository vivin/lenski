package VivinPaliath;

public class Nutrient {

    private String bitPattern = null;

    private Nutrient(){}

    public Nutrient(String bitPattern) {
        if(bitPattern.replaceAll(" ", "").length() > 16) {
            System.out.println("Nutrient must be a 16-bit string");
        }
        this.bitPattern = bitPattern.replaceAll(" ", "");
    }

    public int energyContent() {
        return Integer.parseInt(bitPattern, 2);
    }

    public String getBitPattern() {
        return this.bitPattern;
    }
}
