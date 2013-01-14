package org.jinterop.dcom.test;

import java.net.UnknownHostException;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JIProgId;
import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIVariant;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;

public class MSWord2
{

    private JIComServer comStub = null;

    private IJIDispatch dispatch = null;

    private IJIComObject unknown = null;

    public MSWord2 ( final String address, final String[] args ) throws JIException, UnknownHostException
    {
        final JISession session = JISession.createSession ( args[1], args[2], args[3] );
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
        System.out.println ( this.dispatch.get ( "Version" ).getObjectAsString ().getString () );
        System.out.println ( this.dispatch.get ( "Path" ).getObjectAsString ().getString () );
        JIVariant variant = this.dispatch.get ( "Documents" );
        //JIInterfacePointer ptr = variant.getObjectAsInterfacePointer();
        //IJIDispatch documents = (IJIDispatch)JIObjectFactory.createCOMInstance(unknown,ptr);
        final IJIDispatch documents = (IJIDispatch)JIObjectFactory.narrowObject ( variant.getObjectAsComObject () );
        final JIString filePath = new JIString ( "c:/temp/test.doc" );
        final JIVariant variant2[] = documents.callMethodA ( "open", new Object[] { filePath.VariantByRef, JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM () } );
        //IJIDispatch document = (IJIDispatch)JIObjectFactory.createCOMInstance(unknown,variant2[0].getObjectAsInterfacePointer());
        final IJIDispatch document = (IJIDispatch)JIObjectFactory.narrowObject ( variant2[0].getObjectAsComObject () );
        variant = document.get ( "Content" );
        //IJIDispatch range = (IJIDispatch)JIObjectFactory.createCOMInstance(unknown,variant.getObjectAsInterfacePointer());
        final IJIDispatch range = (IJIDispatch)JIObjectFactory.narrowObject ( variant.getObjectAsComObject () );

        variant = range.get ( "Find" );
        final IJIDispatch find = (IJIDispatch)JIObjectFactory.narrowObject ( variant.getObjectAsComObject () );

        Thread.sleep ( 2000 );
        final JIString findString = new JIString ( "ow" );
        final JIString replaceString = new JIString ( "igh" );
        find.callMethodA ( "Execute", new Object[] { findString.VariantByRef, JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), replaceString.VariantByRef, JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM () } );
        Thread.sleep ( 5000 );

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
            final MSWord2 test = new MSWord2 ( args[0], args );
            test.startWord ();
            test.showWord ();
            test.performOp ();
        }
        catch ( final Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        }
    }

}
