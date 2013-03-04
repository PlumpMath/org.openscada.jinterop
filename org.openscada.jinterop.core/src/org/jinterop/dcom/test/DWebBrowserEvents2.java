package org.jinterop.dcom.test;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIVariant;
import org.jinterop.dcom.impls.JIObjectFactory;

public class DWebBrowserEvents2
{

    public DWebBrowserEvents2 ()
    {

    }

    //	[id(0x00000070), helpstring("Fired when the PutProperty method has been called.")]
    //	 void PropertyChange([in] BSTR szProperty);
    public void PropertyChange ( final JIString szProperty )
    {
        System.out.println ( "PropertyChange -> " + szProperty.getString () );
    }

    //	[id(0x000000fa), helpstring("Fired before navigate occurs in the given WebBrowser (window or frameset element). The processing of this navigation may be modified.")]
    //	 void BeforeNavigate2(
    //	                 [in] IDispatch* pDisp, 
    //	                 [in] VARIANT* URL, 
    //	                 [in] VARIANT* Flags, 
    //	                 [in] VARIANT* TargetFrameName, 
    //	                 [in] VARIANT* PostData, 
    //	                 [in] VARIANT* Headers, 
    //	                 [in, out] VARIANT_BOOL* Cancel);

    public JIVariant BeforeNavigate2 ( IJIComObject dispatch, final JIVariant URL, final JIVariant Flags, final JIVariant TargetFrameName, final JIVariant PostData, final JIVariant Headers, final JIVariant Cancel ) throws JIException
    {
        dispatch = JIObjectFactory.narrowObject ( dispatch );
        JIVariant realURL = URL;
        while ( realURL.isByRefFlagSet () )
        {
            realURL = realURL.getObjectAsVariant ();
        }

        System.out.println ( "BeforeNavigate2  -> " + realURL.getObjectAsString ().getString () );

        //uncomment and return this to stop loading the page
        //JIVariant variant = new JIVariant(true,true);

        return Cancel;
    }

    //[id(0x00000066), helpstring("Statusbar text changed.")]
    public void StatusTextChange ( final JIString text )
    {
        System.out.println ( "StatusTextChange -> " + text.getString () );
    }

    //[id(0x0000006c), helpstring("Fired when download progress is updated.")]
    public void ProgressChange ( final int Progress, final int ProgressMax )
    {
        System.out.println ( "ProgressChange -> " + Progress + " , " + ProgressMax );
    }

    //[id(0x00000069), helpstring("The enabled state of a command changed.")]
    public void CommandStateChange ( final int Command, final boolean Enable )
    {
        System.out.println ( "CommandStateChange -> " + Command + " , " + Enable );
    }

    //    [id(0x0000006a), helpstring("Download of a page started.")]
    public void DownloadBegin ()
    {
        System.out.println ( "DownloadBegin" );
    }

    //    [id(0x00000068), helpstring("Download of page complete.")]
    public void DownloadComplete ()
    {
        System.out.println ( "DownloadComplete" );
    }

    //[id(0x00000071), helpstring("Document title changed.")]
    public void TitleChange ( final JIString Text )
    {
        System.out.println ( "TitleChange -> " + Text.getString () );
    }

    //[id(0x000000fb), helpstring("A new, hidden, non-navigated WebBrowser window is needed.")]
    public JIVariant NewWindow2 ( final JIVariant ppDisp, final JIVariant Cancel ) throws JIException
    {
        System.out.println ( "NewWindow2 -> " + Cancel.getObjectAsBoolean () );
        return Cancel;
    }

    //[id(0x000000fc), helpstring("Fired when the document being navigated to becomes visible and enters the navigation stack.")]
    public void NavigateComplete2 ( IJIComObject pDisp, final JIVariant URL ) throws JIException
    {
        pDisp = JIObjectFactory.narrowObject ( pDisp );
        JIVariant realURL = URL;
        while ( realURL.isByRefFlagSet () )
        {
            realURL = realURL.getObjectAsVariant ();
        }

        System.out.println ( "NavigateComplete2 -> " + pDisp.getInterfaceIdentifier () + " , " + realURL.getObjectAsString ().getString () );
    }

    //[id(0x00000103), helpstring("Fired when the document being navigated to reaches ReadyState_Complete.")]
    public void DocumentComplete ( final IJIComObject pDisp, final JIVariant URL ) throws JIException
    {
        System.out.println ( "DocumentComplete -> " + pDisp.getInterfaceIdentifier () + " , " + URL );
    }

    //[id(0x000000fd), helpstring("Fired when application is quiting.")]
    public void OnQuit ()
    {
        System.out.println ( "OnQuit -> " );
    }

    //[id(0x000000fe), helpstring("Fired when the window should be shown/hidden")]
    public void OnVisible ( final boolean Visible )
    {
        System.out.println ( "OnVisible -> " + Visible );
    }

    //[id(0x000000ff), helpstring("Fired when the toolbar  should be shown/hidden")]
    public void OnToolBar ( final boolean ToolBar )
    {
        System.out.println ( "OnToolBar -> " + ToolBar );
    }

    //[id(0x00000100), helpstring("Fired when the menubar should be shown/hidden")]
    public void OnMenuBar ( final boolean MenuBar )
    {
        System.out.println ( "OnMenuBar -> " + MenuBar );
    }

    //[id(0x00000101), helpstring("Fired when the statusbar should be shown/hidden")]
    public void OnStatusBar ( final boolean StatusBar )
    {
        System.out.println ( "OnStatusBar -> " + StatusBar );
    }

    //[id(0x00000102), helpstring("Fired when fullscreen mode should be on/off")]
    public void OnFullScreen ( final boolean FullScreen )
    {
        System.out.println ( "OnFullScreen -> " + FullScreen );
    }

    //[id(0x00000104), helpstring("Fired when theater mode should be on/off")]
    public void OnTheaterMode ( final boolean TheaterMode )
    {
        System.out.println ( "OnTheaterMode -> " + TheaterMode );
    }

    //[id(0x00000106), helpstring("Fired when the host window should allow/disallow resizing")]
    public void WindowSetResizable ( final boolean Resizable )
    {
        System.out.println ( "OnResizable -> " + Resizable );
    }

    //[id(0x00000108), helpstring("Fired when the host window should change its Left coordinate")]
    public void WindowSetLeft ( final int Left )
    {
        System.out.println ( "WindowSetLeft - > " + Left );
    }

    //[id(0x00000109), helpstring("Fired when the host window should change its Top coordinate")]
    public void WindowSetTop ( final int Top )
    {
        System.out.println ( "WindowSetTop - > " + Top );
    }

    //[id(0x0000010a), helpstring("Fired when the host window should change its width")]
    public void WindowSetWidth ( final int Width )
    {
        System.out.println ( "WindowSetWidth - > " + Width );
    }

    //[id(0x0000010b), helpstring("Fired when the host window should change its height")]
    public void WindowSetHeight ( final int Height )
    {
        System.out.println ( "WindowSetHeight - > " + Height );
    }

    //[id(0x00000107), helpstring("Fired when the WebBrowser is about to be closed by script")]
    public JIVariant WindowClosing ( final boolean IsChildWindow, final JIVariant Cancel ) throws JIException
    {
        System.out.println ( "WindowClosing -> " + IsChildWindow + " , " + Cancel.getObjectAsBoolean () );
        return Cancel;
    }

    //[id(0x0000010c), helpstring("Fired to request client sizes be converted to host window sizes")]
    public Integer[] ClientToHostWindow ( final int CX, final int CY )
    {
        System.out.println ( "ClientToHostWindow - > " + CX + " , " + CY );
        return new Integer[] { new Integer ( CX ), new Integer ( CY ) };
    }

    //    [id(0x0000010d), helpstring("Fired to indicate the security level of the current web page contents")]
    public void SetSecureLockIcon ( final int SecureLockIcon )
    {
        System.out.println ( "SetSecureLockIcon - > " + SecureLockIcon );
    }

    //[id(0x0000010e), helpstring("Fired to indicate the File Download dialog is opening")]
    public JIVariant FileDownload ( final boolean noIdeaWhat, final JIVariant Cancel ) throws JIException
    {
        System.out.println ( "FileDownload - > " + Cancel );
        return Cancel;
    }

    //[id(0x0000010f), helpstring("Fired when a binding error occurs (window or frameset element).")]
    public JIVariant NavigateError ( final IJIComObject pDisp, final JIVariant URL, final JIVariant Frame, final JIVariant StatusCode, final JIVariant Cancel ) throws JIException
    {
        System.out.println ( "NavigateError - > " + URL.getObjectAsString () );
        return Cancel;
    }

    //[id(0x000000e1), helpstring("Fired when a print template is instantiated.")]
    public void PrintTemplateInstantiation ( final IJIComObject pDisp )
    {
        System.out.println ( "PrintTemplateInstantiation - > " + pDisp.getInterfaceIdentifier () );
    }

    //[id(0x000000e2), helpstring("Fired when a print template destroyed.")]
    public void PrintTemplateTeardown ( final IJIComObject pDisp )
    {
        System.out.println ( "PrintTemplateTeardown - > " + pDisp.getInterfaceIdentifier () );
    }

    //[id(0x000000e3), helpstring("Fired when a page is spooled. When it is fired can be changed by a custom template.")]
    public void UpdatePageStatus ( final IJIComObject pDisp, final JIVariant nPage, final JIVariant fDone )
    {
        System.out.println ( "UpdatePageStatus - > " + pDisp.getInterfaceIdentifier () );
    }

    //[id(0x00000110), helpstring("Fired when the global privacy impacted state changes")]
    public void PrivacyImpactedStateChange ( final boolean bImpacted )
    {
        System.out.println ( "PrivacyImpactedStateChange - > " + bImpacted );
    }

    //[id(0x00000111), helpstring("A new, hidden, non-navigated WebBrowser window is needed.")]
    public JIVariant NewWindow3 ( final JIVariant ppDisp, final JIVariant Cancel, final int dwFlags, final JIString bstrUrlContext, final JIString bstrUrl ) throws JIException
    {
        System.out.println ( "NewWindow3 - > " + ppDisp + " , " + Cancel.getObjectAsBoolean () + " , " + bstrUrl.getString () );
        return Cancel;
    }

    public void SetPhishingFilterStatus ( final int PhishingFilterStatus ) throws JIException
    {
        System.out.println ( "SetPhishingFilterStatus - > " + PhishingFilterStatus );
        //return Cancel;
    }

    public void WindowStateChanged ( final int dwWindowStateFlags, final int dwValidFlagsMask )
    {
        System.out.println ( "WindowStateChanged - > " + dwWindowStateFlags + " , " + dwValidFlagsMask );
    }

}
