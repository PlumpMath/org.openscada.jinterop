package org.jinterop.dcom.test;

import java.net.UnknownHostException;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JIProgId;
import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIVariant;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;
import org.jinterop.dcom.impls.automation.IJITypeInfo;

public class MSADO
{

    private JIComServer comServer = null;

    private IJIDispatch dispatch = null;

    private IJIComObject unknown = null;

    private JISession session = null;

    public MSADO ( final String address, final String[] args ) throws JIException, UnknownHostException
    {
        this.session = JISession.createSession ( args[1], args[2], args[3] );
        this.comServer = new JIComServer ( JIProgId.valueOf ( "ADODB.Connection" ), address, this.session );
    }

    public void performOp () throws JIException, InterruptedException
    {
        this.unknown = this.comServer.createInstance ();
        this.dispatch = (IJIDispatch)JIObjectFactory.narrowObject ( this.unknown.queryInterface ( IJIDispatch.IID ) );
        final IJITypeInfo typeInfo = this.dispatch.getTypeInfo ( 0 );
        typeInfo.getFuncDesc ( 0 );

        this.dispatch.callMethod ( "Open", new Object[] { new JIString ( "driver=Microsoft Access Driver (*.mdb);dbq=C:\\temp\\products.mdb" ), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), new Integer ( -1 ) } );

        JIVariant variant[] = this.dispatch.callMethodA ( "Execute", new Object[] { new JIString ( "SELECT * FROM Products" ), new Integer ( -1 ) } );
        if ( variant[0].isNull () )
        {
            System.out.println ( "Recordset is empty." );
        }
        else
        {
            final IJIDispatch resultSet = (IJIDispatch)JIObjectFactory.narrowObject ( variant[0].getObjectAsComObject () );
            //variant = resultSet.get("EOF");
            while ( !resultSet.get ( "EOF" ).getObjectAsBoolean () )
            {
                JIVariant variant2 = resultSet.get ( "Fields" );
                final IJIDispatch fields = (IJIDispatch)JIObjectFactory.narrowObject ( variant2.getObjectAsComObject () );
                final int count = fields.get ( "Count" ).getObjectAsInt ();
                for ( int i = 0; i < count; i++ )
                {
                    variant = fields.get ( "Item", new Object[] { new Integer ( i ) } );
                    final IJIDispatch field = (IJIDispatch)JIObjectFactory.narrowObject ( variant[0].getObjectAsComObject () );
                    variant2 = field.get ( "Value" );
                    Object val = null;
                    if ( variant2.getType () == JIVariant.VT_BSTR )
                    {
                        val = variant2.getObjectAsString ().getString ();
                    }
                    if ( variant2.getType () == JIVariant.VT_I4 )
                    {
                        val = new Integer ( variant2.getObjectAsInt () );
                    }
                    System.out.println ( field.get ( "Name" ).getObjectAsString ().getString () + " = " + val + "[" + variant2.getType () + "]" );
                }
                resultSet.callMethod ( "MoveNext" );
            }

        }

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
            final MSADO test = new MSADO ( args[0], args );
            test.performOp ();
        }
        catch ( final Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        }
    }

}
