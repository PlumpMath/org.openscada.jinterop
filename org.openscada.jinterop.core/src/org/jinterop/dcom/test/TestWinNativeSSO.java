package org.jinterop.dcom.test;

import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIClsid;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JISession;

public class TestWinNativeSSO
{

    public static void main ( final String[] args )
    {

        try
        {

            final JISession session = JISession.createSession ();
            final JIComServer comServer = new JIComServer ( JIClsid.valueOf ( "00024500-0000-0000-C000-000000000046" ), session );
            final IJIComObject comObject = comServer.createInstance ();
            final int h = 0;

            //			SSPIJNIClient jniClient = SSPIJNIClient.getInstance();
            //			byte[] type1Message = jniClient.invokePrepareSSORequest();
            //			jcifs.util.Hexdump.hexdump(System.out, type1Message, 0, type1Message.length);
            //			int h = 0;
            //			
            //			jniClient.invokeUnInitialize();
            //			
            //			type1Message = new Type1Message().toByteArray();
            //			jcifs.util.Hexdump.hexdump(System.out, type1Message, 0, type1Message.length);
        }
        catch ( final Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace ();
        }

    }
}
