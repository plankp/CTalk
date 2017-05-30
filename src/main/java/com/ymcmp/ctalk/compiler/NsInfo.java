/**
 * MIT License
 *
 * Copyright (c) 2017 Paul T.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.ymcmp.ctalk.compiler;

import java.io.Serializable;

/**
 *
 * @author YTENG
 */
public class NsInfo implements Serializable {

    private static final long serialVersionUID = 324191236315L;

    public enum Visibility {
        EXPORT, INTERNAL, HIDDEN
    }

    public final Visibility visibility;
    public final String name;
    public final String hierachy;

    public NsInfo(Visibility visibility, String name, String hierachy) {
        this.visibility = visibility;
        this.name = name;
        this.hierachy = hierachy;
    }

    @Override
    public String toString() {
        return String.format("%s", visibility);
    }

    public static String toExternalName(String qualId) {
        switch (qualId.substring(0, 2)) {
        case "_T":
            return qualId.substring(2);
        case "_C":
            break;
        default:
            return qualId;
        }
        final StringBuilder sb = new StringBuilder();
        int idx = 2;
        final char[] arr = qualId.toCharArray();
        while (idx < arr.length && Character.isDigit(arr[idx])) {
            int extLen = 0;
            while (idx < arr.length && Character.isDigit(arr[idx])) {
                extLen = extLen * 10 + Character.digit(arr[idx], 10);
                ++idx;
            }
            sb.append(qualId.substring(idx, idx + extLen)).append("::");
            idx += extLen;
        }
        sb.delete(sb.length() - 2, sb.length());

        while (idx < arr.length && arr[idx] == '_') {
            ++idx;
            if (arr[idx] == 'v') {
                sb.append("()");
                break;
            }
            int extLen = 0;
            while (idx < arr.length && Character.isDigit(arr[idx])) {
                extLen = extLen * 10 + Character.digit(arr[idx], 10);
                ++idx;
            }
            sb.append(':').append(qualId.substring(idx, idx + extLen));
            idx += extLen;
        }
        return sb.toString();
    }
}
