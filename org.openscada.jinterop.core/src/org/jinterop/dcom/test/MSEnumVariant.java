package org.jinterop.dcom.test;

import java.net.UnknownHostException;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIArray;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JIProgId;
import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIVariant;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;
import org.jinterop.dcom.impls.automation.IJIEnumVariant;

//StdCollection.VBCollection
public class MSEnumVariant
{

    private JIComServer comServer = null;

    private JISession session = null;

    private IJIDispatch dispatch = null;

    public MSEnumVariant ( final String address, final String[] args ) throws JIException, UnknownHostException
    {
        this.session = JISession.createSession ( args[1], args[2], args[3] );
        this.comServer = new JIComServer ( JIProgId.valueOf ( "StdCollection.VBCollection" ), address, this.session );
        final IJIComObject object = this.comServer.createInstance ();
        this.dispatch = (IJIDispatch)JIObjectFactory.narrowObject ( object.queryInterface ( IJIDispatch.IID ) );

    }

    public void performOp () throws JIException
    {
        int i = 0;
        for ( ; i < 5; i++ )
        {
            this.dispatch.callMethod ( "Add", new Object[] { new Integer ( i ), new JIString ( "Key-" + i ) } );
        }

        for ( ; i < 10; i++ )
        {
            this.dispatch.callMethod ( "Add", new Object[] { new Integer ( i ), JIVariant.OPTIONAL_PARAM () } );
        }

        final JIVariant variant = this.dispatch.get ( "_NewEnum" );

        final IJIComObject object2 = variant.getObjectAsComObject ();
        //IJIComObject enumObject = (IJIComObject)object2.queryInterface(IJIEnumVARIANT.IID);

        final IJIEnumVariant enumVARIANT = (IJIEnumVariant)JIObjectFactory.narrowObject ( object2.queryInterface ( IJIEnumVariant.IID ) );

        for ( i = 0; i < 10; i++ )
        {
            final Object[] values = enumVARIANT.next ( 1 );
            final JIArray array = (JIArray)values[0];
            final Object[] arrayObj = (Object[])array.getArrayInstance ();
            for ( int j = 0; j < arrayObj.length; j++ )
            {
                System.out.println ( ( (JIVariant)arrayObj[j] ).getObjectAsInt () + "," + ( (Integer)values[1] ).intValue () );
            }

            final int j = 0;
        }

        enumVARIANT.reset ();
        Object[] values = enumVARIANT.next ( 5 );
        enumVARIANT.next ( 1 );
        enumVARIANT.skip ( 2 );
        values = enumVARIANT.next ( 1 );
        final IJIEnumVariant newenum = enumVARIANT.Clone ();
        newenum.reset ();
        values = newenum.next ( 10 );
        i = 0;

        JISession.destroySession ( this.session );
    }

    public static void main ( final String[] args )
    {

        try
        {
            if ( args.length < 4 )
            {
                System.out.println ( "Please provide address domain username password" );
                return;
            }
            JISystem.setAutoRegisteration ( true );
            final MSEnumVariant enumVariant = new MSEnumVariant ( args[0], args );
            enumVariant.performOp ();
        }
        catch ( final Exception e )
        {
            e.printStackTrace ();
        }
    }

}
