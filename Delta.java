package VivinPaliath;

/**
 * Helper class to hold information about the delta (in coordinates) between a cell and its neighbors. For example
 * if the coordinates of a cell are (r, c), then its northern neighbor is (r - 1, c). This would be encoded as a delta
 * of (-1, 0).
*/
public class Delta {
    private int dr;
    private int dc;

    public Delta(int dr, int dc) {
        this.dr = dr;
        this.dc = dc;
    }

    public int getDr() {
        return dr;
    }

    public int getDc() {
        return dc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Delta delta = (Delta) o;

        if (dr != delta.dr) return false;
        if (dc != delta.dc) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = dr;
        result = 31 * result + dc;
        return result;
    }
}
