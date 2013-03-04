/**j-Interop (Pure Java implementation of DCOM protocol)
 * Copyright (C) 2006  Vikram Roopchand
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * Though a sincere effort has been made to deliver a professional,
 * quality product,the library itself is distributed WITHOUT ANY WARRANTY;
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110, USA
 */

package org.jinterop.dcom.impls.automation;

import java.io.Serializable;

import org.jinterop.dcom.core.JIPointer;
import org.jinterop.dcom.core.JIStruct;
import org.jinterop.dcom.core.JIUnion;

/**
 * Implements the <i>VARDESC</i> structure of COM Automation
 * 
 * @since 1.0
 */
public final class VarDesc implements Serializable
{

    private static final long serialVersionUID = -3874889610447398180L;

    public static final int VAR_PERINSTANCE = 0;

    public static final int VAR_STATIC = 1;

    public static final int VAR_CONST = 2;

    public static final int VAR_DISPATCH = 3;

    public final int memberId;

    public final JIPointer lpstrSchema;

    public final JIUnion u;

    /**
     * Contains the variable type.
     */
    public final ElemDesc elemdescVar;

    /**
     * Definition of flags follows
     */
    public final short wVarFlags;

    public final int varkind;

    VarDesc ( final JIPointer values )
    {
        this ( values.isNull () ? null : (JIStruct)values.getReferent () );
    }

    VarDesc ( final JIStruct filledStruct )
    {
        if ( filledStruct == null )
        {
            this.memberId = -1;
            this.lpstrSchema = null;
            this.u = null;
            this.elemdescVar = null;
            this.wVarFlags = -1;
            this.varkind = -1;
            return;
        }

        this.memberId = ( (Integer)filledStruct.getMember ( 0 ) ).intValue ();
        this.lpstrSchema = (JIPointer)filledStruct.getMember ( 1 );
        this.u = (JIUnion)filledStruct.getMember ( 2 );
        this.elemdescVar = new ElemDesc ( (JIStruct)filledStruct.getMember ( 3 ) );
        this.wVarFlags = ( (Short)filledStruct.getMember ( 4 ) ).shortValue ();
        this.varkind = ( (Integer)filledStruct.getMember ( 5 ) ).intValue ();
    }

}
