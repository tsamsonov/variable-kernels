package ika.proj;

import com.jhlabs.map.MapMath;
import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.ProjectionException;

/**
 * Projection factors used for analysis of distortions
 * @author Bernhard Jenny
 */

/**
 * Changed 09.04.2015.
 * 1. Added calculation of convergence, parallel convergence
 * 2. Added calculation of max scale error azimuth (beta)
 * 3. Added calculation of distorted azimuth and linear scale factor
 * @author tsamsonov
 */
public class ProjectionFactors {
    
    /**
     * derivatives
     */
    public ProjectionDerivatives der = new ProjectionDerivatives();
    
    /**
     * meridinal scale
     */
    public double h;
    
    /**
     * parallel scale
     */
    public double k;
    
    /**
     * angular distortion
     */
    public double omega;
    
    /**
     * theta prime
     */
    public double thetap;
    
    /**
     * meridian convergence
     */
    public double conv;
    
    /**
     * parallel convergence
     */
    public double conv2;
    
    /**
     * areal scale factor
     */
    public double s;
    
    /**
     * max scale error
     */
    public double a;
    
    /**
     * min scale error
     */
    public double b;
    
    /**
     * Tissot ellipse orientation (direction of max scale error)
     */
    public float beta;
    
    /**
     * Computed state flag
     */
    private boolean computed = false;

    /**
     * Initialize the values.
     * @param projection The projection to use.
     * @param lam The longitude in radians.
     * @param phi The latitude in radians.
     * @param dh Delta for computing derivatives.
     */
    public void compute(Projection projection, double lam, double phi, double dh) {
        
        double cosphi, t, n, r;
        final double EPS = 1.0e-12;
        
        // check for latitude or longitude over-range
        if ((t = Math.abs(phi)-MapMath.HALFPI) > EPS || Math.abs(lam) > 10.) {
            throw new ProjectionException("-14");
        }
        
        // errno = pj_errno = 0;
        if (Math.abs(t) <= EPS) {
            phi = phi < 0. ? -MapMath.HALFPI : MapMath.HALFPI;
        }
                /* else if (P->geoc) FIXME
                    phi = atan(P->rone_es * tan(phi));
                 */
        lam = MapMath.normalizeLongitude(lam - projection.getProjectionLongitude()); // compute del lam
                /* FIXME
                if (!P->over)
                    lam = adjlon(lam); // adjust del longitude
                 */
                /* FIXME
                if (P->spc)	// get what projection analytic values
                    P->spc(lp, P, fac);
                 */
                /* FIXME
                if (((fac->code & (IS_ANAL_XL_YL+IS_ANAL_XP_YP)) !=
                        (IS_ANAL_XL_YL+IS_ANAL_XP_YP)) &&
                        pj_deriv(lp, dh, P, &der))
                    return 1;
                if (!(fac->code & IS_ANAL_XL_YL)) {
                    fac->der.x_l = der.x_l;
                    fac->der.y_l = der.y_l;
                }
                if (!(fac->code & IS_ANAL_XP_YP)) {
                    fac->der.x_p = der.x_p;
                    fac->der.y_p = der.y_p;
                }*/
        der.compute(projection, lam, phi, dh);
        cosphi = Math.cos(phi);
            /*
            if (!(fac->code & IS_ANAL_HK)) {
                fac->h = hypot(fac->der.x_p, fac->der.y_p);
                fac->k = hypot(fac->der.x_l, fac->der.y_l) / cosphi;
                if (P->es) {
                    t = sin(phi);
                    t = 1. - P->es * t * t;
                    n = sqrt(t);
                    fac->h *= t * n / P->one_es;
                    fac->k *= n;
                    r = t * t / P->one_es;
                } else
                    r = 1.;
            } else if (P->es) {
                r = sin(phi);
                r = 1. - P->es * r * r;
                r = r * r / P->one_es;
            } else
                r = 1.;
             */
        
        // h = sqrt(E) = sqrt(dx/dphi*dx/dphi + dy/dphi*dy/dphi)
        // Math.hypot is computing the square root of the sum of the squared numbers.
        this.h = Math.hypot(der.x_p, der.y_p);
        // k = sqrt(G)/cosphi = sqrt(dx/dlam*dx/dlam + dy/dlam*dy/dlam)/cosphi
        this.k = Math.hypot(der.x_l, der.y_l) / cosphi;
        
        r = 1.;
        
            /* FIXME
                // convergence
                if (!(fac->code & IS_ANAL_CONV)) {
                    fac->conv = - atan2(fac->der.y_l, fac->der.x_l);
                    if (fac->code & IS_ANAL_XL_YL)
                        fac->code |= IS_ANAL_CONV;
                }
             */
        
        // meridian convergence (tsamsonov)
        
        if(der.x_p != 0){
            conv = Math.atan2(der.x_p, der.y_p);
        } else {
            conv = 0.d;
        }
        
        conv = (Math.abs(conv) < EPS)? 0 : conv;
        
        // areal scale factor
        this.s = (der.y_p * der.x_l - der.x_p * der.y_l) * r / cosphi; // this equals a*b
        
        // meridian-parallel angle theta prime
        
        // fixed by tsamsonov
        double sintheta = s / (h * k);
        if(sintheta > 1)
            sintheta = 1;
        if(sintheta < -1)
            sintheta = -1;
        
        // Theta prime
        this.thetap = Math.PI - Math.asin(sintheta);
        
        // parallel convergence
        conv2 = MapMath.HALFPI + conv - thetap;
        
        conv2 = (Math.abs(conv2) < EPS)? 0 : conv2;
        
        // Tissot ellipse axes
        t = k * k + h * h;
        this.a = Math.sqrt(t + 2. * s);
        t = (t = t - 2. * s) <= 0. ? 0. : Math.sqrt(t);
        this.b = 0.5 * (a - t);
        this.a = 0.5 * (a + t);
        
        // Tissot ellipse orientation (direction of max scale error)
        double err = 0.5*Math.PI - thetap;
        
        float sin = (float)Math.sin(2.0d * thetap);
        float cos = (float)Math.cos(2.0d * thetap);
        float k2 = (float) (k * k);
        float h2 = (float) (h * h);
        
        float dx = h2 * sin;
        float dy = k2 + h2 * cos;
        
        dx = (dx < EPS) ? 0.f : dx;
        dy = (dy < EPS) ? 0.f : dy;

        beta = (float) (0.5 * Math.atan2(dx, dy));
        
        // omega
        this.omega = 2. * Math.asin(Math.abs((a - b)/(a + b)));
    }

    /**
     * Calculates planar azimuth from geodetic azimuth
     * @param geodeticAzimuth
     * @return 
     */
    public double getAzimuth(double geodeticAzimuth){
        if(computed){
            return 0.d;
        } else return Double.NaN;
    }
    
    /**
     * Calculates scale factor in a given (geodetic) azimuth
     * @param geodeticAzimuth
     * @return 
     */
    public double getScaleFactor(double geodeticAzimuth){
        if(computed){
            return 0.d;
        } else return Double.NaN;
    }
}