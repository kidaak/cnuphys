package cnuphys.swim;

import java.util.Vector;

import cnuphys.lund.GeneratedParticleRecord;
import cnuphys.lund.LundId;
import cnuphys.magfield.IField;

/**
 * Combines a generated particle record with a path (trajectory). A trajectory
 * is a collection of state vectors. A state vector is the six component vector: <BR>
 * Q = [x, y, z, px/p, py/p, pz/p] <BR>
 * 
 * @author heddle
 * 
 */
@SuppressWarnings("serial")
public class SwimTrajectory extends Vector<double[]> {

    // the particle that we swam
    private GeneratedParticleRecord _genPartRec;

    // the lund id, if it is known (i.e. from montecarlo truth)
    private LundId _lundId;

    // flag indicating whether bdl was computed
    private boolean _computedBDL;

    /** index for the x component (m) */
    public static final int X_IDX = 0;

    /** index for the y component (m) */
    public static final int Y_IDX = 1;

    /** index for the z component (m) */
    public static final int Z_IDX = 2;

    /** index for the px/p direction cosine */
    public static final int DIRCOSX_IDX = 3;

    /** index for the py/p direction cosine */
    public static final int DIRCOSY_IDX = 4;

    /** index for the pz/p direction cosine */
    public static final int DIRCOSZ_IDX = 5;

    /** index for the accumulated path length (m) */
    public static final int PATHLEN_IDX = 6;

    /** index for the accumulated integral |B x dL| component (kG-m) */
    public static final int BXDL_IDX = 7;

    /** user object */
    public Object userObject;

    /**
     * Create a one point trajectory. Used hrn the initial momentum is lower
     * than some minimum value.
     * 
     * @param charge
     *            the charge of the particle (-1 for electron, +1 for proton,
     *            etc.)
     * @param xo
     *            the x vertex position in m
     * @param yo
     *            the y vertex position in m
     * @param zo
     *            the z vertex position in m
     * @param momentum
     *            initial momentum in GeV/c
     * @param theta
     *            initial polar angle in degrees
     * @param phi
     *            initial azimuthal angle in degrees
     */
    public SwimTrajectory(int charge, double xo, double yo, double zo,
	    double momentum, double theta, double phi) {
	this(charge, xo, yo, zo, momentum, theta, phi, 1, 1);

	double thetRad = Math.toRadians(theta);
	double phiRad = Math.toRadians(phi);

	double pz = Math.cos(thetRad);
	double rho = Math.sin(thetRad);
	double px = rho * Math.cos(phiRad);
	double py = rho * Math.sin(phiRad);
	double v[] = new double[6];
	v[0] = xo;
	v[1] = yo;
	v[2] = zo;
	v[3] = px;
	v[4] = py;
	v[5] = pz;
	add(0, v);
    }

    /**
     * @param charge
     *            the charge of the particle (-1 for electron, +1 for proton,
     *            etc.)
     * @param xo
     *            the x vertex position in m
     * @param yo
     *            the y vertex position in m
     * @param zo
     *            the z vertex position in m
     * @param momentum
     *            initial momentum in GeV/c
     * @param theta
     *            initial polar angle in degrees
     * @param phi
     *            initial azimuthal angle in degrees
     * @param initialCapacity
     *            the initial capacity of the trajectory list
     * @param increment
     *            the size increment when the list needs to expand
     */
    public SwimTrajectory(int charge, double xo, double yo, double zo,
	    double momentum, double theta, double phi, int initialCapacity,
	    int increment) {
	this(new GeneratedParticleRecord(charge, xo, yo, zo, momentum, theta,
		phi), initialCapacity, increment);
    }

    /**
     * @param genPartRec
     *            the generated particle record
     * @param initialCapacity
     *            the initial capacity of the trajectory list
     * @param increment
     *            the size increment when the list needs to expand
     */
    public SwimTrajectory(GeneratedParticleRecord genPartRec,
	    int initialCapacity, int increment) {
	super(initialCapacity, increment);
	_genPartRec = genPartRec;
    }

    /**
     * Set the lund id. This is not needed for swimming, but is useful for ced
     * or when MonteCarlo truth is known.
     * 
     * @param lundId
     *            the Lund Id.
     */
    public void setLundId(LundId lundId) {
	_lundId = lundId;
    }

    /**
     * Get the lund id. This is not needed for swimming, and may be
     * <code>null</code>. It is useful for ced or when MonteCarlo truth is
     * known. return the Lund Id.
     */
    public LundId getLundId() {
	return _lundId;
    }

    /**
     * Get the underlying generated particle record
     * 
     * @return the underlying generated particle record
     */
    public GeneratedParticleRecord getGeneratedParticleRecord() {
	return _genPartRec;
    }

    /**
     * Get the original theta for this trajectory
     * 
     * @return the original theta for this trajectory in degrees
     */
    public double getOriginalTheta() {
	return _genPartRec.getTheta();
    }

    /**
     * Get the original phi for this trajectory
     * 
     * @return the original phi for this trajectory in degrees
     */
    public double getOriginalPhi() {
	return _genPartRec.getPhi();
    }

    /**
     * Get the average phi for this trajectory based on positions, not
     * directions
     * 
     * @return the average phi value in degrees
     */
    public double getAveragePhi() {
	if (size() < 6) {
	    return getOriginalPhi();
	}

	double phi = 0;
	double count = 0;
	for (int i = 5; i < size(); i += 5) {
	    double pos[] = get(i);
	    double x = pos[X_IDX];
	    double y = pos[Y_IDX];
	    double tp = Math.atan2(y, x);
	    phi += tp;
	    count++;
	}

	return Math.toDegrees(phi / count);
    }

    /**
     * Get the final radial coordinate
     * 
     * @return final radial coordinate in meters
     */
    public double getFinalR() {
	if (isEmpty()) {
	    return Double.NaN;
	}

	double pos[] = getFinalPosition();
	double x = pos[X_IDX];
	double y = pos[Y_IDX];
	double z = pos[Z_IDX];
	return Math.sqrt(x * x + y * y + z * z);
    }

    /**
     * Get the final position
     * 
     * @return the final position
     */
    final double[] getFinalPosition() {
	if (isEmpty()) {
	    return null;
	}
	double[] pos = new double[3];
	double lastQ[] = lastElement();

	for (int i = 0; i < 3; i++) {
	    pos[i] = lastQ[i];
	}
	return pos;
    }

    /**
     * Compute the integral Bdotdl. This will cause the state vector arrays to
     * expand by two, becoming [x, y, z, px/p, py/p, pz/p, l, bdl] where the 7th
     * entry l is cumulative pathlength in m and the eighth entry bdl is the
     * cumulative integral bdl in kG-m.
     * 
     * @param field
     *            the field getter
     */
    public void computeBDL(IField field) {
	if (_computedBDL) {
	    return;
	}

	Bxdl previous = new Bxdl();
	Bxdl current = new Bxdl();
	double[] p0 = this.get(0);
	augment(p0, 0, 0, 0);

	for (int i = 1; i < size(); i++) {
	    double[] p1 = get(i);
	    Bxdl.accumulate(previous, current, p0, p1, field);
	    augment(p1, current.getPathlength(), current.getIntegralBxdl(), i);
	    previous.set(current);
	    p0 = p1;
	}

	_computedBDL = true;
    }

    // relpace the 6D state vector at the given index with
    // and 8D vector that appends pathelength (m) and integral
    // b dot dl (kg-m)
    private void augment(double p[], double pl, double bdl, int index) {
	double newp[] = new double[8];
	System.arraycopy(p, 0, newp, 0, 6);
	newp[PATHLEN_IDX] = pl;
	newp[BXDL_IDX] = bdl;

	set(index, newp);
    }

    /**
     * Check whether the accumulated integral bdl has been computed
     * 
     * @return <code>true</code> if the accumulated integral bdl has been
     *         computed
     */
    public boolean isBDLComputed() {
	return _computedBDL;
    }
}