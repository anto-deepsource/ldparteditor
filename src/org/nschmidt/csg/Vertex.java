/**
 * Vertex.java
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
 * Represents a vertex of a polygon.
 */
public class Vertex {

    /**
     * Vertex position.
     */
    public Vector3d pos;

    /**
     * Constructor. Creates a vertex.
     *
     * @param pos
     *            position
     * @param normal
     *            normal
     */
    public Vertex(Vector3d pos) {
        this.pos = pos;
    }

    @Override
    public Vertex clone() {
        return new Vertex(pos.clone());
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
    public Vertex interpolate(Vertex other, double t) {
        return new Vertex(pos.lerp(other.pos, t));
    }

    /**
     * Returns this vertex in STL string format.
     *
     * @return this vertex in STL string format
     */
    public String toStlString() {
        return "vertex " + this.pos.toStlString(); //$NON-NLS-1$
    }

    /**
     * Returns this vertex in STL string format.
     *
     * @param sb
     *            string builder
     * @return the specified string builder
     */
    public StringBuilder toStlString(StringBuilder sb) {
        sb.append("vertex "); //$NON-NLS-1$
        return this.pos.toStlString(sb);
    }

    /**
     * Returns this vertex in OBJ string format.
     *
     * @param sb
     *            string builder
     * @return the specified string builder
     */
    public StringBuilder toObjString(StringBuilder sb) {
        sb.append("v "); //$NON-NLS-1$
        return this.pos.toObjString(sb).append("\n"); //$NON-NLS-1$
    }

    /**
     * Returns this vertex in OBJ string format.
     *
     * @return this vertex in OBJ string format
     */
    public String toObjString() {
        return toObjString(new StringBuilder()).toString();
    }

    /**
     * Applies the specified transform to this vertex.
     *
     * @param transform
     *            the transform to apply
     * @return this vertex
     */
    public Vertex transform(Transform transform) {
        pos = pos.transform(transform);
        return this;
    }

    /**
     * Applies the specified transform to a copy of this vertex.
     *
     * @param transform
     *            the transform to apply
     * @return a copy of this transform
     */
    public Vertex transformed(Transform transform) {
        return clone().transform(transform);
    }
}
