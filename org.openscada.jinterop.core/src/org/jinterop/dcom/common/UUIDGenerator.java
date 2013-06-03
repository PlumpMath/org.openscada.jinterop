/**j-Interop (Pure Java implementation of DCOM protocol)  
 * Copyright (C) 2013  Jens Reimann (ctron@dentrassi.de)
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
package org.jinterop.dcom.common;

public final class UUIDGenerator
{
    private UUIDGenerator ()
    {
    }

    public static String generateID ()
    {
        return org.openscada.uuid.UUIDGenerator.generateID ().toString ();
    }
}
