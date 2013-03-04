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

public class MSWord
{

    private JIComServer comStub = null;

    private IJIDispatch dispatch = null;

    private IJIComObject unknown = null;

    public MSWord ( final String address, final String[] args ) throws JIException, UnknownHostException
    {
        final JISession session = JISession.createSession ( args[1], args[2], args[3] );
        session.useSessionSecurity ( true );
        this.comStub = new JIComServer ( JIProgId.valueOf ( "Word.Application" ), address, session );
    }

    public void startWord () throws JIException
    {
        this.unknown = this.comStub.createInstance ();
        this.dispatch = (IJIDispatch)JIObjectFactory.narrowObject ( this.unknown.queryInterface ( IJIDispatch.IID ) );
    }

    public void showWord () throws JIException
    {
        final int dispId = this.dispatch.getIDsOfNames ( "Visible" );
        final JIVariant variant = new JIVariant ( true );
        this.dispatch.put ( dispId, variant );
    }

    public void performOp () throws JIException, InterruptedException
    {

        /* JISystem config *
         *
         */
        JISystem.setJavaCoClassAutoCollection ( true );

        System.out.println ( this.dispatch.get ( "Version" ).getObjectAsString ().getString () );
        System.out.println ( this.dispatch.get ( "Path" ).getObjectAsString ().getString () );
        JIVariant variant = this.dispatch.get ( "Documents" );

        System.out.println ( "Open document..." );
        final IJIDispatch documents = (IJIDispatch)JIObjectFactory.narrowObject ( variant.getObjectAsComObject () );
        final JIString filePath = new JIString ( "c:\\temp\\test.doc" );
        final JIVariant variant2[] = documents.callMethodA ( "open", new Object[] { filePath, JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM () } );

        System.out.println ( "doc opened" );
        //10
        sleep ( 10 );

        System.out.println ( "Get content..." );
        final IJIDispatch document = (IJIDispatch)JIObjectFactory.narrowObject ( variant2[0].getObjectAsComObject () );
        variant = document.get ( "Content" );
        final IJIDispatch range = (IJIDispatch)JIObjectFactory.narrowObject ( variant.getObjectAsComObject () );

        //10
        sleep ( 10 );
        System.out.println ( "Running find..." );
        variant = range.get ( "Find" );
        final IJIDispatch find = (IJIDispatch)JIObjectFactory.narrowObject ( variant.getObjectAsComObject () );

        //2
        sleep ( 5 );

        System.out.println ( "Running execute..." );
        final JIString findString = new JIString ( "ow" );
        final JIString replaceString = new JIString ( "igh" );
        find.callMethodA ( "Execute", new Object[] { findString.VariantByRef, JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), replaceString.VariantByRef, JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM () } );

        //1
        sleep ( 2 );

        System.out.println ( "Closing document..." );
        document.callMethod ( "Close" );

    }

    private void sleep ( final int minutes ) throws InterruptedException
    {
        System.out.println ( "Sleeping " + minutes + " minute(s)..." );
        Thread.sleep ( minutes * 60 * 1000 );
    }

    /**
     * @throws JIException
     */
    private void quitAndDestroy () throws JIException
    {
        System.out.println ( "Quit..." );
        this.dispatch.callMethod ( "Quit", new Object[] { new JIVariant ( -1, true ), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM () } );
        JISession.destroySession ( this.dispatch.getAssociatedSession () );
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

            // JR: JISystem.getLogger ().setLevel ( Level.INFO );
            // JR: configure using slf4j now
            JISystem.setInBuiltLogHandler ( false );
            final MSWord test = new MSWord ( args[0], args );
            test.startWord ();
            test.showWord ();

            //			for (int i = 0; i < 10; i++) {
            test.performOp ();
            //			}

            test.quitAndDestroy ();

        }
        catch ( final Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        }
    }

}
