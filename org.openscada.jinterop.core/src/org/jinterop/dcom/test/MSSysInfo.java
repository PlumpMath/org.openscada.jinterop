package org.jinterop.dcom.test;

import java.net.UnknownHostException;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JILocalCoClass;
import org.jinterop.dcom.core.JILocalInterfaceDefinition;
import org.jinterop.dcom.core.JILocalMethodDescriptor;
import org.jinterop.dcom.core.JIProgId;
import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;

public class MSSysInfo
{

    JISession session = null;

    IJIComObject sysInfoObject = null;

    IJIComObject sysInfoServer = null;

    IJIDispatch dispatch = null;

    String identifier = null;

    MSSysInfo ( final String[] args ) throws JIException, UnknownHostException
    {
        this.session = JISession.createSession ( args[1], args[2], args[3] );
        this.session.useSessionSecurity ( true );
        final JIComServer comServer = new JIComServer ( JIProgId.valueOf ( "SYSINFO.SysInfo" ), args[0], this.session );
        this.sysInfoServer = comServer.createInstance ();
        this.sysInfoObject = this.sysInfoServer.queryInterface ( "6FBA474C-43AC-11CE-9A0E-00AA0062BB4C" );
        this.dispatch = (IJIDispatch)JIObjectFactory.narrowObject ( this.sysInfoObject.queryInterface ( IJIDispatch.IID ) );

    }

    void displayValues () throws JIException
    {
        System.out.println ( "ACStatus: " + this.dispatch.get ( "ACStatus" ).getObjectAsShort () );
        System.out.println ( "BatteryFullTime: " + this.dispatch.get ( "BatteryFullTime" ).getObjectAsInt () );
        System.out.println ( "BatteryLifePercent: " + this.dispatch.get ( "BatteryLifePercent" ).getObjectAsShort () );
        System.out.println ( "BatteryLifeTime: " + this.dispatch.get ( "BatteryLifeTime" ).getObjectAsInt () );
        System.out.println ( "BatteryStatus: " + this.dispatch.get ( "BatteryStatus" ).getObjectAsShort () );
        System.out.println ( "OSVersion: " + this.dispatch.get ( "OSVersion" ).getObjectAsFloat () );
        //dispatch.callMethod("AboutBox");

    }

    void AttachEventListener () throws JIException
    {
        //6FBA474D-43AC-11CE-9A0E-00AA0062BB4C

        final JILocalCoClass javaComponent = new JILocalCoClass ( new JILocalInterfaceDefinition ( "6FBA474D-43AC-11CE-9A0E-00AA0062BB4C" ), SysInfoEvents.class );
        javaComponent.getInterfaceDefinition ().addMethodDescriptor ( new JILocalMethodDescriptor ( "PowerStatusChanged", 8, null ) );
        javaComponent.getInterfaceDefinition ().addMethodDescriptor ( new JILocalMethodDescriptor ( "TimeChanged", 3, null ) );
        this.identifier = JIObjectFactory.attachEventHandler ( this.sysInfoServer, "6FBA474D-43AC-11CE-9A0E-00AA0062BB4C", JIObjectFactory.buildObject ( this.session, javaComponent ) );
        try
        {
            Thread.sleep ( 3000 );
        }
        catch ( final InterruptedException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        } //for call backs
    }

    void DetachEventListener () throws JIException
    {
        JIObjectFactory.detachEventHandler ( this.sysInfoServer, this.identifier );
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
            // JR: JISystem.getLogger ().setLevel ( Level.OFF );
            // JR: configure using slf4j now
            JISystem.setAutoRegisteration ( true );
            final MSSysInfo sysInfo = new MSSysInfo ( args );
            sysInfo.displayValues ();
            sysInfo.AttachEventListener ();
            Thread.sleep ( 20000 );//now play around with power settings
            sysInfo.DetachEventListener ();
        }
        catch ( final Exception e )
        {
            e.printStackTrace ();
        }

    }

}
