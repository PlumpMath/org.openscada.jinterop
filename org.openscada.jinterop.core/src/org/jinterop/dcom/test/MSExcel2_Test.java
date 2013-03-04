package org.jinterop.dcom.test;

import java.net.UnknownHostException;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIArray;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JIProgId;
import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIVariant;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;

public class MSExcel2_Test
{

    private JIComServer comServer = null;

    private IJIDispatch dispatch = null;

    private IJIComObject unknown = null;

    private IJIDispatch dispatchOfWorkSheets = null;

    private IJIDispatch dispatchOfWorkBook = null;

    private IJIDispatch dispatchOfWorkSheet = null;

    private JISession session = null;

    public MSExcel2_Test ( final String address, final String[] args ) throws JIException, UnknownHostException
    {
        this.session = JISession.createSession ( args[1], args[2], args[3] );
        this.session.useSessionSecurity ( true );
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

    public void pasteArrayToWorkSheet ( final int nRow ) throws JIException
    {
        final int dispId = this.dispatchOfWorkSheet.getIDsOfNames ( "Range" );
        final JIVariant variant = new JIVariant ( new JIString ( "A1:C" + nRow ) );
        final Object[] out = new Object[] { JIVariant.class };
        final JIVariant[] outVal2 = this.dispatchOfWorkSheet.get ( dispId, new Object[] { variant } );
        final IJIDispatch dispRange = (IJIDispatch)JIObjectFactory.narrowObject ( outVal2[0].getObjectAsComObject () );

        JIVariant[][] newValue = new JIVariant[nRow][3];

        for ( int i = 0; i < newValue.length; i++ )
        {
            for ( int j = 0; j < newValue[i].length; j++ )
            {
                newValue[i][j] = new JIVariant ( 10.0 * Math.random () );
            }
        }

        dispRange.put ( "Value2", new JIVariant ( new JIArray ( newValue ) ) );

        try
        {
            Thread.sleep ( 20000 );
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

        //Now write the value down
        dispRange.put ( "Value2", new JIVariant ( newValue2 ) );

        try
        {
            Thread.sleep ( 20000 );
        }
        catch ( final InterruptedException e )
        {
            e.printStackTrace ();
        }

        this.dispatchOfWorkBook.callMethod ( "close", new Object[] { Boolean.FALSE, JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM () } );
        this.dispatch.callMethod ( "Quit" );
        JISession.destroySession ( this.session );

    }

    public static void main ( final String[] args )
    {

        try
        {

            // JR: JISystem.getLogger ().setLevel ( Level.FINEST );
            // JR: configure using slf4j now

            if ( args.length < 4 )
            {
                System.out.println ( "Please provide address domain username password" );
                return;
            }

            //JISystem.setInBuiltLogHandler(false);
            //Logger l = Logger.getLogger("org.jinterop");
            //l.setLevel(Level.FINEST);

            int nRow = 600;

            if ( args.length > 4 )
            {
                try
                {
                    nRow = Integer.parseInt ( args[4] );
                }
                catch ( final NumberFormatException e )
                {

                }
            }

            final MSExcel2_Test test = new MSExcel2_Test ( args[0], args );

            test.startExcel ();
            test.showExcel ();
            test.createWorkSheet ();

            test.pasteArrayToWorkSheet ( nRow );

        }
        catch ( final Exception e )
        {
            e.printStackTrace ();
        }

    }

}
