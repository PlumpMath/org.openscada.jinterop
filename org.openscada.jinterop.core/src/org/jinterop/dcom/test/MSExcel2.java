package org.jinterop.dcom.test;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

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

public class MSExcel2
{

    private JIComServer comServer = null;

    private IJIDispatch dispatch = null;

    private IJIComObject unknown = null;

    private IJIDispatch dispatchOfWorkSheets = null;

    private IJIDispatch dispatchOfWorkBook = null;

    private IJIDispatch dispatchOfWorkSheet = null;

    private JISession session = null;

    public MSExcel2 ( final String address, final String[] args ) throws JIException, UnknownHostException
    {
        this.session = JISession.createSession ( args[1], args[2], args[3] );
        //		session.useSessionSecurity(true);
        this.comServer = new JIComServer ( JIProgId.valueOf ( "Excel.Application" ), address, this.session );
    }

    public void startExcel () throws JIException
    {
        this.unknown = this.comServer.createInstance ();
        this.dispatch = (IJIDispatch)JIObjectFactory.narrowObject ( this.unknown.queryInterface ( IJIDispatch.IID ) );
    }

    public void showExcel () throws JIException
    {
        final int dispId = this.dispatch.getIDsOfNames ( "Visible" );
        final JIVariant variant = new JIVariant ( true );
        this.dispatch.put ( dispId, variant );
    }

    public void createWorkSheet () throws JIException
    {
        final int dispId = this.dispatch.getIDsOfNames ( "Workbooks" );

        JIVariant outVal = this.dispatch.get ( dispId );

        final IJIDispatch dispatchOfWorkBooks = (IJIDispatch)JIObjectFactory.narrowObject ( outVal.getObjectAsComObject () );

        JIVariant[] outVal2 = dispatchOfWorkBooks.callMethodA ( "Add", new Object[] { JIVariant.OPTIONAL_PARAM () } );
        this.dispatchOfWorkBook = (IJIDispatch)JIObjectFactory.narrowObject ( outVal2[0].getObjectAsComObject () );

        outVal = this.dispatchOfWorkBook.get ( "Worksheets" );

        this.dispatchOfWorkSheets = (IJIDispatch)JIObjectFactory.narrowObject ( outVal.getObjectAsComObject () );

        outVal2 = this.dispatchOfWorkSheets.callMethodA ( "Add", new Object[] { JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM () } );
        this.dispatchOfWorkSheet = (IJIDispatch)JIObjectFactory.narrowObject ( outVal2[0].getObjectAsComObject () );
    }

    public void pasteArrayToWorkSheet () throws JIException
    {
        final int dispId = this.dispatchOfWorkSheet.getIDsOfNames ( "Range" );
        final JIVariant variant = new JIVariant ( new JIString ( "A1:C3" ) );
        final Object[] out = new Object[] { JIVariant.class };
        final JIVariant[] outVal2 = this.dispatchOfWorkSheet.get ( dispId, new Object[] { variant } );
        final IJIDispatch dispRange = (IJIDispatch)JIObjectFactory.narrowObject ( outVal2[0].getObjectAsComObject () );

        JIVariant[][] newValue = { { new JIVariant ( new JIString ( "defe" ) ), new JIVariant ( false ), new JIVariant ( ( 98765.0 / 12345.0 ) ) }, { new JIVariant ( new Date () ), new JIVariant ( 5454 ), new JIVariant ( (float) ( 22.0 / 7.0 ) ) }, { new JIVariant ( true ), new JIVariant ( new JIString ( "dffe" ) ), new JIVariant ( new Date () ) } };

        // implement safe array XxX dimension

        dispRange.put ( "Value2", new JIVariant ( new JIArray ( newValue ) ) );

        try
        {
            Thread.sleep ( 10000 );
        }
        catch ( final InterruptedException e )
        {
            e.printStackTrace ();
        }

        final JIVariant variant2 = dispRange.get ( "Value2" );
        final JIArray newValue2 = variant2.getObjectAsArray ();
        newValue = (JIVariant[][])newValue2.getArrayInstance ();
        for ( int i = 0; i < newValue.length; i++ )
        {
            for ( int j = 0; j < newValue[i].length; j++ )
            {
                System.out.print ( newValue[i][j] + "\t" );
            }
            System.out.println ();
        }

        this.dispatchOfWorkBook.callMethod ( "close", new Object[] { Boolean.FALSE, JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM () } );
        this.dispatch.callMethod ( "Quit" );
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
            JISystem.setInBuiltLogHandler ( false );
            final Logger l = Logger.getLogger ( "org.jinterop" );
            l.setLevel ( Level.FINEST );
            final MSExcel2 test = new MSExcel2 ( args[0], args );
            test.startExcel ();
            test.showExcel ();
            test.createWorkSheet ();
            test.pasteArrayToWorkSheet ();
        }
        catch ( final Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        }
    }

}
