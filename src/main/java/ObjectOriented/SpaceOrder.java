package ObjectOriented;


/**
 * Example comes from Essentials of Software Testing by Ralf Beirig, Stephen Brown, Edgar Galvin,
 * Joe Timothy, Cambridge UP, 2022.
 * I have added some slight modifications
 * All sizes imagined to be square meters.
 */

public class SpaceOrder {

    private final Integer MAX_SIZE = 1024;
    private final Integer NORMAL_MIN_SIZE = 16;
    private final Integer ABSOLUTE_MIN_SIZE = 0;
    private boolean special;
    private boolean accept;

    public SpaceOrder() {
        special = false;
    }

    /**
     * Constructor
     * @param special boolean to determine whether client can go below normal min size
     */
    public SpaceOrder(boolean special) {
        this.special = special;
    }

    /**
     * Determine whether to accept an order based on requested space
     * @param space square meters requested
     * @return boolean expressing that the space request was above 0.  NOT whether the request has been accepted.
     */
    public boolean acceptOrder(int space) {
        boolean status = true;
        this.accept = false;
        if (space <= ABSOLUTE_MIN_SIZE) {
            status = false;
        } else if (space <= MAX_SIZE && (space >= NORMAL_MIN_SIZE || this.special)) {
            this.accept = true;
        }
        return status;
    }

    public Integer getMAX_SIZE() {
        return MAX_SIZE;
    }

    public Integer getNORMAL_MIN_SIZE() {
        return NORMAL_MIN_SIZE;
    }

    public Integer getABSOLUTE_MIN_SIZE() {
        return ABSOLUTE_MIN_SIZE;
    }

    public boolean isSpecial() {
        return special;
    }

    public boolean isAccept() {
        return accept;
    }

    public void setSpecial(boolean special) {
        this.special = special;
    }
}