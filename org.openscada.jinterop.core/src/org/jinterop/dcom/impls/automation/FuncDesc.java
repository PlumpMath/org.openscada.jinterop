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

import org.jinterop.dcom.core.JIArray;
import org.jinterop.dcom.core.JIPointer;
import org.jinterop.dcom.core.JIStruct;

/**
 * Implements the <i>FUNCDESC</i> structure of COM Automation.
 * <p>
 * Definition from MSDN: <i> Describes a function.</i> More information can be
 * obtained here http://msdn.microsoft.com/en-us/library/ms221425(VS.85).aspx .
 * 
 * @since 1.0
 */
public final class FuncDesc implements Serializable
{

    private static final long serialVersionUID = -1361861233072624432L;

    public static final int FUNCFLAG_FRESTRICTED = 0x1;

    public static final int FUNCFLAG_FSOURCE = 0x2;

    public static final int FUNCFLAG_FBINDABLE = 0x4;

    public static final int FUNCFLAG_FREQUESTEDIT = 0x8;

    public static final int FUNCFLAG_FDISPLAYBIND = 0x10;

    public static final int FUNCFLAG_FDEFAULTBIND = 0x20;

    public static final int FUNCFLAG_FHIDDEN = 0x40;

    public static final int FUNCFLAG_FUSESGETLASTERROR = 0x80;

    public static final int FUNCFLAG_FDEFAULTCOLLELEM = 0x100;

    public static final int FUNCFLAG_FUIDEFAULT = 0x200;

    public static final int FUNCFLAG_FNONBROWSABLE = 0x400;

    public static final int FUNCFLAG_FREPLACEABLE = 0x800;

    public static final int FUNCFLAG_FIMMEDIATEBIND = 0x1000;

    //    MEMBERID memid;                        // Function member ID.
    ///* [size_is] */ SCODE __RPC_FAR *lprgscode;
    ///* [size_is] */ ELEMDESC __RPC_FAR *lprgelemdescParam;
    //	FuncKind funckind;           // Specifies whether the function is virtual, static, or dispatch-only.
    //    InvokeKind invkind;        // Invocation kind. Indicates if this is a property function, and if so, what kind.
    //    CallConv callconv;        // Specifies the function's calling
    //                            // convention.
    //    short cParams;            // Count of total number of parameters.
    //    short cParamsOpt;        // Count of optional parameters (detailed
    //                            // description follows).
    //    short oVft;                // For FUNC_VIRTUAL, specifies the offset in the VTBL.
    //    short cScodes;    // Count of permitted return values.
    //    ELEMDESC elemdescFunc;    // Contains the return type of the function.
    //    WORD wFuncFlags;     // Definition of flags follows.

    private JIStruct values = null;

    /**
     * Function member ID.
     */
    public final int memberId;

    public final JIPointer lprgscode;

    public final JIPointer lprgelemdescParam;

    /**
     * Specifies whether the function is virtual, static, or dispatch-only.
     */
    public final int funcKind;

    /**
     * Invocation kind. Indicates if this is a property function, and if so,
     * what kind.
     */
    public final int invokeKind;

    /**
     * Specifies the function's calling convention.
     */
    public final int callConv;

    /**
     * Count of total number of parameters.
     */
    public final short cParams;

    /**
     * Count of optional parameters (detailed description follows).
     */
    public final short cParamsOpt;

    /**
     * For FUNC_VIRTUAL, specifies the offset in the VTBL.
     */
    public final short oVft;

    /**
     * Count of permitted return values.
     */
    public final short cScodes;

    /**
     * Contains the return type of the function.
     */
    public final ElemDesc elemdescFunc;

    /**
     * Definition of flags follows.
     */
    public final short wFuncFlags;

    FuncDesc ( final JIPointer values )
    {
        this ( values.isNull () ? null : (JIStruct)values.getReferent () );
    }

    FuncDesc ( final JIStruct filledStruct )
    {
        if ( filledStruct == null )
        {
            this.values = null;
            this.memberId = -1;
            this.lprgscode = null;
            this.lprgelemdescParam = null;
            this.funcKind = -1;
            this.invokeKind = -1;
            this.callConv = -1;
            this.cParams = -1;
            this.cParamsOpt = -1;
            this.oVft = -1;
            this.cScodes = -1;
            this.elemdescFunc = null;
            this.wFuncFlags = -1;
            return;
        }
        this.values = filledStruct;
        this.memberId = ( (Integer)this.values.getMember ( 0 ) ).intValue ();
        this.lprgscode = (JIPointer)this.values.getMember ( 1 );
        final JIPointer ptr = (JIPointer)this.values.getMember ( 2 );
        JIArray arrayOfElemDesc = null;
        if ( !ptr.isNull () )
        {
            final JIArray arry = (JIArray)ptr.getReferent ();
            final Object[] obj = (Object[])arry.getArrayInstance ();
            //			ElemDesc[] arry2 = new ElemDesc[obj.length];
            //			for (int i = 0; i < obj.length; i++)
            //			{
            //				arry2[i] = new ElemDesc((JIStruct)obj[i]);
            //			}

            //			arrayOfElemDesc = new JIArray(arry2);
            arrayOfElemDesc = new JIArray ( obj );
        }

        this.lprgelemdescParam = new JIPointer ( arrayOfElemDesc );
        this.funcKind = ( (Integer)this.values.getMember ( 3 ) ).intValue ();
        this.invokeKind = ( (Integer)this.values.getMember ( 4 ) ).intValue ();
        this.callConv = ( (Integer)this.values.getMember ( 5 ) ).intValue ();
        this.cParams = ( (Short)this.values.getMember ( 6 ) ).shortValue ();
        this.cParamsOpt = ( (Short)this.values.getMember ( 7 ) ).shortValue ();
        this.oVft = ( (Short)this.values.getMember ( 8 ) ).shortValue ();
        this.cScodes = ( (Short)this.values.getMember ( 9 ) ).shortValue ();
        this.elemdescFunc = new ElemDesc ( (JIStruct)this.values.getMember ( 10 ) );
        this.wFuncFlags = ( (Short)this.values.getMember ( 11 ) ).shortValue ();
    }

}
