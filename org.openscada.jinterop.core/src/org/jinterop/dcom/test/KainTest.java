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

public class KainTest
{

    private JIComServer comServer = null;

    private final IJIDispatch dispatch = null;

    private IJIComObject unknown = null;

    public KainTest ( final String address, final String[] args ) throws JIException, UnknownHostException
    {
        final JISession session = JISession.createSession ( args[1], args[2], args[3] );
        this.comServer = new JIComServer ( JIProgId.valueOf ( "Word.Application" ), address, session );
    }

    public void startWord () throws JIException
    {
        this.unknown = this.comServer.createInstance ();
        final IJIDispatch dispatch = (IJIDispatch)JIObjectFactory.narrowObject ( this.unknown.queryInterface ( IJIDispatch.IID ) );
    }

    public void showWord () throws JIException
    {
        final int dispId = this.dispatch.getIDsOfNames ( "Visible" );
        final JIVariant variant = new JIVariant ( true );
        this.dispatch.put ( dispId, variant );
    }

    public void performOp () throws JIException, InterruptedException
    {
        final String sDir = "c:\\tmp\\";
        final String sInputDoc = sDir + "file_in.doc";
        final String sOutputDoc = sDir + "file_out.doc";

        final String sOldText = "[label:import:1]";
        final String sNewText = "I am some horribly long sentence, so long that [insert something long here]";
        final boolean tVisible = true;
        final boolean tSaveOnExit = false;

        System.out.println ( this.dispatch.get ( "Version" ).getObjectAsString ().getString () );
        System.out.println ( this.dispatch.get ( "Path" ).getObjectAsString ().getString () );

        JIVariant variant = this.dispatch.get ( "Documents" );
        final IJIDispatch documents = (IJIDispatch)JIObjectFactory.narrowObject ( variant.getObjectAsComObject () );
        //String has to be a JIString.
        final JIString filePath = new JIString ( sInputDoc );
        //this "open" is of Word 2003
        final JIVariant variant2[] = documents.callMethodA ( "open", new Object[] { new JIVariant ( filePath, true ), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM () } );

        final IJIDispatch document = (IJIDispatch)JIObjectFactory.narrowObject ( variant2[0].getObjectAsComObject () );
        variant = this.dispatch.get ( "Selection" );
        final IJIDispatch selection = (IJIDispatch)JIObjectFactory.narrowObject ( variant.getObjectAsComObject () );

        variant = selection.get ( "Find" );
        final IJIDispatch find = (IJIDispatch)JIObjectFactory.narrowObject ( variant.getObjectAsComObject () );

        Thread.sleep ( 2000 );

        find.put ( "Text", new JIVariant ( new JIString ( sOldText ) ) );
        find.callMethodA ( "Execute", new Object[] { JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM () } );

        Thread.sleep ( 2000 );

        selection.put ( "Text", new JIVariant ( new JIString ( sNewText ) ) );
        selection.callMethodA ( "MoveDown", new Object[] { JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM () } );
        selection.put ( "Text", new JIVariant ( new JIString ( "\nSo we got the next line including BR.\n" ) ) );

        variant = selection.get ( "Font" );
        final IJIDispatch font = (IJIDispatch)JIObjectFactory.narrowObject ( variant.getObjectAsComObject () );
        font.put ( "Bold", new JIVariant ( 1 ) );
        font.put ( "Italic", new JIVariant ( 1 ) );
        font.put ( "Underline", new JIVariant ( 0 ) );

        variant = selection.get ( "ParagraphFormat" );
        final IJIDispatch align = (IJIDispatch)JIObjectFactory.narrowObject ( variant.getObjectAsComObject () );
        align.put ( "Alignment", new JIVariant ( 3 ) );

        Thread.sleep ( 5000 );

        final JIString sImgFile = new JIString ( sDir + "image.png" );
        selection.callMethodA ( "MoveDown", new Object[] { JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM () } );
        variant = selection.get ( "InLineShapes" );
        final IJIDispatch image = (IJIDispatch)JIObjectFactory.narrowObject ( variant.getObjectAsComObject () );
        image.callMethodA ( "AddPicture", new Object[] { new JIVariant ( sImgFile ), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM () } );

        final JIString sHyperlink = new JIString ( "http://www.google.com" );
        selection.put ( "Text", new JIVariant ( new JIString ( "Text for the link to Google" ) ) );
        variant = selection.get ( "Range" );
        final IJIDispatch range = (IJIDispatch)JIObjectFactory.narrowObject ( variant.getObjectAsComObject () );
        variant = document.get ( "Hyperlinks" );
        final IJIDispatch link = (IJIDispatch)JIObjectFactory.narrowObject ( variant.getObjectAsComObject () );
        link.callMethod ( "Add", new Object[] { range, sHyperlink, JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM (), JIVariant.OPTIONAL_PARAM () } );

        variant = this.dispatch.get ( "WordBasic" );
        final IJIDispatch wordBasic = (IJIDispatch)JIObjectFactory.narrowObject ( variant.getObjectAsComObject () );
        wordBasic.callMethod ( "FileSaveAs", new Object[] { new JIString ( sOutputDoc ) } );

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
            final KainTest test = new KainTest ( args[0], args );
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
