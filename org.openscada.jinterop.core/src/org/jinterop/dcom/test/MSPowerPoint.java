package org.jinterop.dcom.test;

import java.net.UnknownHostException;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIClsid;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIVariant;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;

public class MSPowerPoint
{

    private JIComServer comStub = null;

    private IJIDispatch dispatch = null;

    private IJIComObject unknown = null;

    public MSPowerPoint ( final String address, final String[] args ) throws JIException, UnknownHostException
    {
        final JISession session = JISession.createSession ( args[1], args[2], args[3] );
        this.comStub = new JIComServer ( JIClsid.valueOf ( "91493441-5A91-11CF-8700-00AA0060263B" ), address, session );
    }

    public void startPowerPoint () throws JIException
    {
        this.unknown = this.comStub.createInstance ();
        this.dispatch = (IJIDispatch)JIObjectFactory.narrowObject ( this.unknown.queryInterface ( IJIDispatch.IID ) );
    }

    public void showPowerPoint () throws JIException
    {
        final int dispId = this.dispatch.getIDsOfNames ( "Visible" );
        final JIVariant variant = new JIVariant ( -1 );
        this.dispatch.put ( dispId, variant );
    }

    public void performOp () throws JIException, InterruptedException
    {
        //JIVariant variant = dispatch.get("Presentations");
        //JIInterfacePointer ptr = variant.getObjectAsInterfacePointer();
        //IJIDispatch presentations = (IJIDispatch)JIObjectFactory.createCOMInstance(unknown,ptr);
        final IJIDispatch presentations = (IJIDispatch)JIObjectFactory.narrowObject ( this.dispatch.get ( "Presentations" ).getObjectAsComObject () );

        for ( int i = 0; i < 2; i++ )
        {
            JIVariant results[] = presentations.callMethodA ( "Add", new Object[] { JIVariant.OPTIONAL_PARAM () } );
            //variant = results[0];
            //ptr = variant.getObjectAsInterfacePointer();
            //IJIDispatch presentation = (IJIDispatch)JIObjectFactory.createCOMInstance(unknown,ptr);
            final IJIDispatch presentation = (IJIDispatch)JIObjectFactory.narrowObject ( results[0].getObjectAsComObject () );
            //variant = presentation.get("Slides");
            //ptr = variant.getObjectAsInterfacePointer();
            //IJIDispatch slides = (IJIDispatch)JIObjectFactory.createCOMInstance(unknown,ptr);
            final IJIDispatch slides = (IJIDispatch)JIObjectFactory.narrowObject ( presentation.get ( "Slides" ).getObjectAsComObject () );

            results = slides.callMethodA ( "Add", new Object[] { new Integer ( 1 ), new Integer ( 1 ) } );
            //variant = results[0];
            //ptr = variant.getObjectAsInterfacePointer();
            final IJIDispatch slide = (IJIDispatch)JIObjectFactory.narrowObject ( results[0].getObjectAsComObject () );

            //variant = slide.get("Shapes");
            //ptr = variant.getObjectAsInterfacePointer();
            final IJIDispatch shapes = (IJIDispatch)JIObjectFactory.narrowObject ( slide.get ( "Shapes" ).getObjectAsComObject () );

            //variant = shapes.get("Title");
            //ptr = variant.getObjectAsInterfacePointer();
            final IJIDispatch shape = (IJIDispatch)JIObjectFactory.narrowObject ( shapes.get ( "Title" ).getObjectAsComObject () );

            //variant = shape.get("TextFrame");
            //ptr = variant.getObjectAsInterfacePointer();
            final IJIDispatch textframe = (IJIDispatch)JIObjectFactory.narrowObject ( shape.get ( "TextFrame" ).getObjectAsComObject () );

            //variant = textframe.get("TextRange");
            //ptr = variant.getObjectAsInterfacePointer();
            final IJIDispatch textrange = (IJIDispatch)JIObjectFactory.narrowObject ( textframe.get ( "TextRange" ).getObjectAsComObject () );

            if ( i == 0 )
            {
                textrange.put ( "Text", new JIString ( "Presentation1" ).Variant );
                presentation.callMethod ( "SaveAs", new Object[] { new JIString ( "C:\\temp\\presentation1.ppt" ).Variant, JIVariant.OPTIONAL_PARAM (), new Integer ( -1 ) } );
                Thread.sleep ( 3000 );
                presentation.callMethod ( "Close" );
            }
            else
            {
                textrange.put ( "Text", new JIString ( "Presentation2" ).Variant );
                slides.callMethod ( "InsertFromFile", new Object[] { new JIString ( "C:\\temp\\presentation1.ppt" ), new Integer ( 1 ), new Integer ( 1 ), new Integer ( 1 ) } );
                presentation.callMethod ( "SaveAs", new Object[] { new JIString ( "C:\\temp\\presentation2.ppt" ), JIVariant.OPTIONAL_PARAM (), new Integer ( -1 ) } );
                Thread.sleep ( 3000 );
                presentation.callMethod ( "Close" );

                this.dispatch.callMethod ( "Quit" );
            }

        }

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
            final MSPowerPoint test = new MSPowerPoint ( args[0], args );
            test.startPowerPoint ();
            test.showPowerPoint ();
            test.performOp ();
        }
        catch ( final Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        }
    }

}
