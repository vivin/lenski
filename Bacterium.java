package VivinPaliath;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This class represents the actual bacterial agent. The agent's behaviors are implemented in this class.
 */
public class Bacterium {

    //agent attributes
    private String genome = "";
    private int maximumLifespan = 0;
    private int lifespan = 0;
    private double freeEnergy = 0;
    private double maximumFreeEnergy = 0;
    private double reproductionThreshold = 0;
    private double metabolicEnergy = 0;
    private List<Enzyme> enzymes = new ArrayList<Enzyme>();

    //offsets into genome
    private static final int LIFESPAN_OFFSET = 0;
    private static final int LIFESPAN_SIZE = 8;
    private static final int FREE_ENERGY_OFFSET = LIFESPAN_OFFSET + LIFESPAN_SIZE;
    private static final int FREE_ENERGY_SIZE = 16;
    private static final int REPRODUCTION_THRESHOLD_OFFSET = FREE_ENERGY_OFFSET + FREE_ENERGY_SIZE;
    private static final int REPRODUCTION_THRESHOLD_SIZE = 16;
    private static final int METABOLIC_ENERGY_OFFSET = REPRODUCTION_THRESHOLD_OFFSET + REPRODUCTION_THRESHOLD_SIZE;
    private static final int METABOLIC_ENERGY_SIZE = 8;
    private static final int ENZYMES_OFFSET = METABOLIC_ENERGY_OFFSET + METABOLIC_ENERGY_SIZE;
    private static final int ENZYME_SIZE = 16;

    private final Random random = new Random(Double.doubleToLongBits(Math.random()));

    //constants that represent mutation types
    private static final int DELETION = 0;
    private static final int REPETITION = 1;
    private static final int INVERSION = 2;
    private static final int INSERTION = 3;

    //Controls the maximum number of bits that can be modified at a time:
    //
    // DELETION: 4 bits is the maximum that may be deleted from parent's genome.
    // REPETITION: a bit from the parent may be repeated a maximum of 4 times.
    // INVERSION: a maximum of 4 consecutive bits from the parent may be inverted.
    // INSERTION: a maximum of 4 random-bits may be inserted.

    private static final int maxModifiedBits = 4;

    private Bacterium() {}

    //One way to instantiate an instance of the agent is to provide the individual attributes. The code will then build up the genome itself
    public Bacterium(int lifespan, int freeEnergy, int reproductionThreshold, int metabolicEnergy, List<Enzyme> enzymes) {
        if(lifespan <= 0 || lifespan > 255) {
            throw new IllegalArgumentException("Lifespan needs to be greater than 0 and lesser than 256");
        }
        this.lifespan = lifespan;
        this.maximumLifespan = lifespan;

        if(freeEnergy <= 0 || freeEnergy > 65535) {
            throw new IllegalArgumentException("Free energy needs to be greater than 0 and lesser than 65536");
        }
        this.freeEnergy = freeEnergy;
        this.maximumFreeEnergy = freeEnergy;

        if(reproductionThreshold <= 0 || reproductionThreshold > 65535) {
            throw new IllegalArgumentException("Reproduction threshold needs to be greater than 0 and lesser than 65536");
        }
        this.reproductionThreshold = reproductionThreshold;

        if(metabolicEnergy <= 0 || metabolicEnergy > 255) {
            throw new IllegalArgumentException("Metabolic energy needs to be greater than 0 and lesser than 256");
        }
        this.metabolicEnergy = metabolicEnergy;

        this.enzymes = enzymes;

        this.genome = pad(Integer.toBinaryString(lifespan), LIFESPAN_SIZE) +
                pad(Integer.toBinaryString(freeEnergy), FREE_ENERGY_SIZE) +
                pad(Integer.toBinaryString(reproductionThreshold), REPRODUCTION_THRESHOLD_SIZE) +
                pad(Integer.toBinaryString(metabolicEnergy), METABOLIC_ENERGY_SIZE);

        for(Enzyme enzyme : enzymes) {
            this.genome += enzyme.getBitPattern();
        }
    }

    //Pads short strings with leading zeroes.
    private String pad(String bitPattern, int size) {
        int numZeroes = size - bitPattern.length();
        StringBuilder builder = new StringBuilder();

        for(int i = 0; i < numZeroes; i++) {
            builder.append("0");
        }

        return builder.append(bitPattern).toString();
    }

    //Another way to instantiate an agent is from the genome
    public Bacterium(String genome) {
        this.genome = genome.replaceAll(" ", "");

        initializeLifespan();
        initializeFreeEnergy();
        initializeReproductionThreshold();
        initializeMetabolicEnergy();
        initializeEnzymes();
    }

    private void initializeLifespan() {
        if(genome.length() >= LIFESPAN_OFFSET + LIFESPAN_SIZE) {
            this.lifespan = Integer.parseInt(genome.substring(LIFESPAN_OFFSET, LIFESPAN_OFFSET + LIFESPAN_SIZE), 2);
            this.maximumLifespan = this.lifespan;
        } else if(genome.length() > LIFESPAN_OFFSET) {
            this.lifespan = Integer.parseInt(genome.substring(LIFESPAN_OFFSET));
            this.maximumLifespan = this.lifespan;
        }
    }

    private void initializeFreeEnergy() {
        if(genome.length() >= FREE_ENERGY_OFFSET + FREE_ENERGY_SIZE) {
            this.freeEnergy = Integer.parseInt(genome.substring(FREE_ENERGY_OFFSET, FREE_ENERGY_OFFSET + FREE_ENERGY_SIZE), 2);
            this.maximumFreeEnergy = freeEnergy;
        } else if(genome.length() > FREE_ENERGY_OFFSET) {
            this.freeEnergy = Integer.parseInt(genome.substring(FREE_ENERGY_OFFSET), 2);
            this.maximumFreeEnergy = freeEnergy;
        }
    }

    private void initializeReproductionThreshold() {
        if(genome.length() >= REPRODUCTION_THRESHOLD_OFFSET + REPRODUCTION_THRESHOLD_SIZE) {
            this.reproductionThreshold = Integer.parseInt(genome.substring(REPRODUCTION_THRESHOLD_OFFSET, REPRODUCTION_THRESHOLD_OFFSET + REPRODUCTION_THRESHOLD_SIZE), 2);
        } else if(genome.length() > REPRODUCTION_THRESHOLD_OFFSET) {
            this.reproductionThreshold = Integer.parseInt(genome.substring(REPRODUCTION_THRESHOLD_OFFSET), 2);
        }
    }

    private void initializeMetabolicEnergy() {
        if(genome.length() >= METABOLIC_ENERGY_OFFSET + METABOLIC_ENERGY_SIZE) {
            this.metabolicEnergy = Integer.parseInt(genome.substring(METABOLIC_ENERGY_OFFSET, METABOLIC_ENERGY_OFFSET + METABOLIC_ENERGY_SIZE), 2);
        } else if(genome.length() > METABOLIC_ENERGY_OFFSET) {
            this.metabolicEnergy = Integer.parseInt(genome.substring(METABOLIC_ENERGY_OFFSET), 2);
        }
    }

    private void initializeEnzymes() {
        if(genome.length() > ENZYMES_OFFSET) {
            String enzymesBitPattern = genome.substring(ENZYMES_OFFSET);

            while(enzymesBitPattern.length() >= ENZYME_SIZE) {
                enzymes.add(new Enzyme(enzymesBitPattern.substring(0, ENZYME_SIZE)));

                if(enzymesBitPattern.length() > ENZYME_SIZE) {
                    enzymesBitPattern = enzymesBitPattern.substring(ENZYME_SIZE);
                } else {
                    enzymesBitPattern = "";
                }
            }

            if(enzymesBitPattern.length() > 0) {
                enzymes.add(new Enzyme(enzymesBitPattern));
            }
        }
    }

    //Implements the feeding behavior.
    public MetabolysisResult feed(Nutrient nutrient) {
        MetabolysisResult metabolysisResult = null;

        double maxEnergy = -1;
        String nutrientBitPattern = nutrient.getBitPattern();
        double nutrientEnergyContent = Integer.parseInt(nutrientBitPattern, 2);
        double efficiency = 0;
        Enzyme efficientEnzyme = null;

        for(Enzyme enzyme : enzymes) {
            String enzymeBitPattern = enzyme.getBitPattern();
            if(nutrientBitPattern.length() > enzymeBitPattern.length()) {
                //If the enzyme is not long enough, we will only extract the matching number of bits from the nutrient, starting with the LSB
                nutrientBitPattern = nutrientBitPattern.substring(nutrientBitPattern.length() - enzymeBitPattern.length());
            }

            String xorResult = Integer.toBinaryString(Integer.parseInt(nutrientBitPattern, 2) ^ Integer.parseInt(enzymeBitPattern, 2));

            int numOnes = 0;
            for(int i = 0; i < xorResult.length(); i++) {
                if(xorResult.charAt(i) == '1') {
                    numOnes++;
                }
            }

            //Calculate the energy extracted. If we have more than one enzyme, we will check to see which enzyme is most efficient.
            double energyFromMetabolysis = (numOnes / 16.0) * nutrientEnergyContent;
            if(energyFromMetabolysis > maxEnergy) {
                maxEnergy = energyFromMetabolysis;
                efficientEnzyme = enzyme;
                efficiency = (numOnes / 16.0);
            }
        }

        if(efficientEnzyme != null) {
            metabolysisResult = new MetabolysisResult(nutrient, efficientEnzyme, efficiency);
        }

        freeEnergy += maxEnergy;
        return metabolysisResult;
    }

    //Implements the rest behavior
    public void rest() {
        int numberOfEnzymes = enzymes.size() == 0 ? 1 : enzymes.size(); //we want to use up energy even if we have no enzymes
        freeEnergy -= (numberOfEnzymes * metabolicEnergy);
        lifespan--;
    }

    //Implements the reproduce behavior
    public String reproduce() {
        StringBuilder genomeBuilder = new StringBuilder();

        //Flags that say whether we are inverting or deleting bits
        boolean invert = false;
        boolean delete = false;
        int numModify = 0; //total number of bits to modify for this mutation

        //We start at bit 48 because we only want to mutate the enzymes
        for(int i = 48; i < genome.length(); i++) {

            //Check to see if we are inverting or deleting.
            if(invert || delete) {
                 if(invert) {
                    genomeBuilder.append(genome.charAt(i) == '1' ? '0' : '1');
                    numModify--;
                    invert = (numModify > 0);

                } else {
                    numModify--;
                    delete = (numModify > 0);
                }
            } else {
                //Check to see if a mutation is happening
                boolean mutate = (random.nextInt(100) == 49); //1% mutation rate
                if(mutate) {

                    //1 bit 55% of the time, 2 bits 30% of the time, 3 bits 10% of the time, and 4 bits 5% of the time.
                    numModify = 0;
                    int rand = random.nextInt(100);
                    if(rand <= 54) {
                        numModify = 1;
                    } else if(rand <= 84) {
                        numModify = 2;
                    } else if(rand <= 94) {
                        numModify = 3;
                    } else if(rand <= 99) {
                        numModify = 4;
                    }

                    //Figure out the type of mutation
                    int mutationType = (random.nextInt(maxModifiedBits));
                    if(mutationType == DELETION) {
                        delete = true;
                    } else if(mutationType == REPETITION) {
                        for(int j = 0; j < numModify; j++) {
                            genomeBuilder.append(genome.charAt(i));
                        }
                    } else if(mutationType == INVERSION) {
                        invert = true;
                    } else if(mutationType == INSERTION) {
                        for(int j = 0; j < numModify; j++) {
                            genomeBuilder.append(random.nextInt(2));
                        }
                    }
                } else {
                    genomeBuilder.append(genome.charAt(i));
                }
            }
        }

        freeEnergy -= (0.5 * reproductionThreshold);
        return genome.substring(0, 47) + genomeBuilder.toString();
    }

    public String getGenome() {
        return genome;
    }

    public int getLifespan() {
        return lifespan;
    }

    public int getMaximumLifespan() {
        return maximumLifespan;
    }

    public double getFreeEnergy() {
        return freeEnergy;
    }

    public double getMaximumFreeEnergy() {
        return maximumFreeEnergy;
    }

    public double getReproductionThreshold() {
        return reproductionThreshold;
    }

    public double getMetabolicEnergy() {
        return metabolicEnergy;
    }

    public List<Enzyme> getEnzymes() {
        return enzymes;
    }
}
