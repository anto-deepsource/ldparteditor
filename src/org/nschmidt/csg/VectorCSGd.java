/**
 * Vector3d.java
 *
 * Copyright 2014-2014 Michael Hoffer <info@michaelhoffer.de>. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY Michael Hoffer <info@michaelhoffer.de> "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL Michael Hoffer <info@michaelhoffer.de> OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of Michael Hoffer
 * <info@michaelhoffer.de>.
 */
package org.nschmidt.csg;

/**
 * 3D Vector3d.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
public class VectorCSGd implements Comparable<VectorCSGd>{

    private static final double epsilon = 0.0001f;

    public double x;
    public double y;
    public double z;

    public static final VectorCSGd ZERO = new VectorCSGd(0, 0, 0);
    public static final VectorCSGd UNITY = new VectorCSGd(1, 1, 1);
    public static final VectorCSGd X_ONE = new VectorCSGd(1, 0, 0);
    public static final VectorCSGd Y_ONE = new VectorCSGd(0, 1, 0);
    public static final VectorCSGd Z_ONE = new VectorCSGd(0, 0, 1);

    /**
     * Creates a new vector.
     *
     * @param x
     *            x value
     * @param y
     *            y value
     * @param z
     *            z value
     */
    public VectorCSGd(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public VectorCSGd clone() {
        return new VectorCSGd(x, y, z);
    }

    /**
     * Returns a negated copy of this vector.
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @return a negated copy of this vector
     */
    public VectorCSGd negated() {
        return new VectorCSGd(-x, -y, -z);
    }

    /**
     * Returns the sum of this vector and the specified vector.
     *
     * @param v
     *            the vector to add
     *
     *            <b>Note:</b> this vector is not modified.
     *
     * @return the sum of this vector and the specified vector
     */
    public VectorCSGd plus(VectorCSGd v) {
        return new VectorCSGd(x + v.x, y + v.y, z + v.z);
    }

    /**
     * Returns the difference of this vector and the specified vector.
     *
     * @param v
     *            the vector to subtract
     *
     *            <b>Note:</b> this vector is not modified.
     *
     * @return the difference of this vector and the specified vector
     */
    public VectorCSGd minus(VectorCSGd v) {
        return new VectorCSGd(x - v.x, y - v.y, z - v.z);
    }

    /**
     * Returns the product of this vector and the specified value.
     *
     * @param a
     *            the value
     *
     *            <b>Note:</b> this vector is not modified.
     *
     * @return the product of this vector and the specified value
     */
    public VectorCSGd times(double a) {
        return new VectorCSGd(x * a, y * a, z * a);
    }

    /**
     * Returns the product of this vector and the specified vector.
     *
     * @param a
     *            the vector
     *
     *            <b>Note:</b> this vector is not modified.
     *
     * @return the product of this vector and the specified vector
     */
    public VectorCSGd times(VectorCSGd a) {
        return new VectorCSGd(x * a.x, y * a.y, z * a.z);
    }

    /**
     * Returns this vector devided by the specified value.
     *
     * @param a
     *            the value
     *
     *            <b>Note:</b> this vector is not modified.
     *
     * @return this vector devided by the specified value
     */
    public VectorCSGd dividedBy(double a) {
        return new VectorCSGd(x / a, y / a, z / a);
    }

    /**
     * Returns the dot product of this vector and the specified vector.
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @param a
     *            the second vector
     *
     * @return the dot product of this vector and the specified vector
     */
    public double dot(VectorCSGd a) {
        return this.x * a.x + this.y * a.y + this.z * a.z;
    }

    /**
     * Returns the magnitude of this vector.
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @return the magnitude of this vector
     */
    public double magnitude() {
        return Math.sqrt(this.dot(this));
    }

    /**
     * Returns a normalized copy of this vector with {@code length}.
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @return a normalized copy of this vector with {@code length}
     */
    public VectorCSGd unit() {
        return this.dividedBy(this.magnitude());
    }

    /**
     * Returns the cross product of this vector and the specified vector.
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @param a
     *            the vector
     *
     * @return the cross product of this vector and the specified vector.
     */
    public VectorCSGd cross(VectorCSGd a) {
        return new VectorCSGd(y * a.z - z * a.y, z * a.x - x * a.z, x * a.y - y * a.x);
    }

    @Override
    public String toString() {
        return "[" + x + ", " + y + ", " + z + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final VectorCSGd other = (VectorCSGd) obj;
        if (Double.doubleToLongBits(this.x) != Double.doubleToLongBits(other.x)) {
            return false;
        }
        if (Double.doubleToLongBits(this.y) != Double.doubleToLongBits(other.y)) {
            return false;
        }
        if (Double.doubleToLongBits(this.z) != Double.doubleToLongBits(other.z)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (int) (Double.doubleToLongBits(x) ^ Double.doubleToLongBits(x) >>> 32);
        hash = 97 * hash + (int) (Double.doubleToLongBits(y) ^ Double.doubleToLongBits(y) >>> 32);
        hash = 97 * hash + (int) (Double.doubleToLongBits(z) ^ Double.doubleToLongBits(z) >>> 32);
        return hash;
    }


    /**
     * Create a new vertex between this vertex and the specified vertex by
     * linearly interpolating all properties using a parameter t.
     *
     * @param other
     *            vertex
     * @param t
     *            interpolation parameter
     * @return a new vertex between this and the specified vertex
     */
    public VectorCSGd interpolate(VectorCSGd other, double t) {
        return plus(other.minus(this).times(t));
    }

    @Override
    public int compareTo(VectorCSGd o) {
        double d1 = x - o.x;
        switch (Double.compare(Math.abs(d1), epsilon)) {
        case 0:
        case 1:
            return d1 < 0f ? -1 : 1;
        default:
            break;
        }
        d1 = y - o.y;
        switch (Double.compare(Math.abs(d1), epsilon)) {
        case 0:
        case 1:
            return d1 < 0f ? -1 : 1;
        default:
            break;
        }
        d1 = z - o.z;
        switch (Double.compare(Math.abs(d1), epsilon)) {
        case 0:
        case 1:
            return d1 < 0f ? -1 : 1;
        default:
            break;
        }
        return 0;
    }
}
