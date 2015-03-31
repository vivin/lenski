package VivinPaliath;

public class Enzyme {

    private String bitPattern = null;

    private Enzyme() {}

    public Enzyme(String bitPattern) {
        if(bitPattern.replaceAll(" ", "").length() > 16) {
            throw new IllegalArgumentException("Enzyme must be a 16-bit string");
        }
        this.bitPattern = bitPattern.replaceAll(" ", "");
    }

    public String getBitPattern() {
        return this.bitPattern;
    }
}
