package org.doubango.ngn.sip;

import java.nio.ByteBuffer;

import org.doubango.ngn.utils.NgnContentType;
import org.doubango.ngn.utils.NgnObservableHashMap;
import org.doubango.ngn.utils.NgnUriUtils;
import org.doubango.tinyWRAP.MessagingSession;
import org.doubango.tinyWRAP.RPMessage;
import org.doubango.tinyWRAP.SMSEncoder;
import org.doubango.tinyWRAP.SipMessage;
import org.doubango.tinyWRAP.SipSession;

import android.util.Log;

public class NgnMessagingSession extends NgnSipSession {
	private static String TAG = NgnMessagingSession.class.getCanonicalName();
	
	private final MessagingSession mSession;
	private static int SMS_MR = 0;
	
	private final static NgnObservableHashMap<Long, NgnMessagingSession> sessions = new NgnObservableHashMap<Long, NgnMessagingSession>(true);
	
	public static NgnMessagingSession takeIncomingSession(NgnSipStack sipStack, MessagingSession session, SipMessage sipMessage){
		final String toUri = sipMessage==null ? null: sipMessage.getSipHeaderValue("f");
		NgnMessagingSession imSession = new NgnMessagingSession(sipStack, session, toUri);
		NgnMessagingSession.sessions.put(imSession.getId(), imSession);
        return imSession;
    }

    public static NgnMessagingSession createOutgoingSession(NgnSipStack sipStack, String toUri){
        synchronized (NgnMessagingSession.sessions){
            final NgnMessagingSession imSession = new NgnMessagingSession(sipStack, null, toUri);
            NgnMessagingSession.sessions.put(imSession.getId(), imSession);
            return imSession;
        }
    }
    
    public static void releaseSession(NgnMessagingSession session){
		synchronized (NgnMessagingSession.sessions){
            if (session != null && NgnMessagingSession.sessions.containsKey(session.getId())){
                long id = session.getId();
                session.decRef();
                NgnMessagingSession.sessions.remove(id);
            }
        }
    }

    public static void releaseSession(long id){
		synchronized (NgnMessagingSession.sessions){
			NgnMessagingSession session = NgnMessagingSession.getSession(id);
            if (session != null){
                session.decRef();
                NgnMessagingSession.sessions.remove(id);
            }
        }
    }
    
	public static NgnMessagingSession getSession(long id) {
		synchronized (NgnMessagingSession.sessions) {
			if (NgnMessagingSession.sessions.containsKey(id))
				return NgnMessagingSession.sessions.get(id);
			else
				return null;
		}
	}

	public static int getSize(){
        synchronized (NgnMessagingSession.sessions){
            return NgnMessagingSession.sessions.size();
        }
    }
	
    public static boolean hasSession(long id){
        synchronized (NgnMessagingSession.sessions){
            return NgnMessagingSession.sessions.containsKey(id);
        }
    }
    
	protected NgnMessagingSession(NgnSipStack sipStack, MessagingSession session, String toUri) {
		super(sipStack);
        mSession = session == null ? new MessagingSession(sipStack) : session;

        super.init();
        super.setSigCompId(sipStack.getSigCompId());
        super.setToUri(toUri);
	}

	@Override
	protected SipSession getSession() {
		return mSession;
	}
	
	public boolean SendBinaryMessage(String text, String SMSC){
        String SMSCPhoneNumber;
        String dstPhoneNumber;
        String dstSipUri = super.getToUri();

        if ((SMSCPhoneNumber = NgnUriUtils.getValidPhoneNumber(SMSC)) != null && (dstPhoneNumber = NgnUriUtils.getValidPhoneNumber(dstSipUri)) != null){
            super.setToUri(SMSC);
            super.addHeader("Content-Type", NgnContentType.SMS_3GPP);
            super.addHeader("Content-Transfer-Encoding", "binary");
            super.addCaps("+g.3gpp.smsip");       
                    
            RPMessage rpMessage;
            //if(ServiceManager.getConfigurationService().getBoolean(CONFIGURATION_SECTION.RCS, CONFIGURATION_ENTRY.HACK_SMS, false)){
                //    rpMessage = SMSEncoder.encodeDeliver(++ScreenSMSCompose.SMS_MR, SMSCPhoneNumber, dstPhoneNumber, new String(content));
                //    session.addHeader("P-Asserted-Identity", SMSC);
            //}
            //else{
                    rpMessage = SMSEncoder.encodeSubmit(++NgnMessagingSession.SMS_MR, SMSCPhoneNumber, dstPhoneNumber, text);
            //}
        
            long rpMessageLen = rpMessage.getPayloadLength();
            ByteBuffer payload = ByteBuffer.allocateDirect((int)rpMessageLen);
            long payloadLength = rpMessage.getPayload(payload, (long)payload.capacity());
            boolean ret = mSession.send(payload, payloadLength);
            rpMessage.delete();
            if(NgnMessagingSession.SMS_MR >= 255){
                 NgnMessagingSession.SMS_MR = 0;
            }

            return ret;
        }
        else{
            Log.e(TAG, String.format("SMSC=%s or RemoteUri=%s is invalid", SMSC, dstSipUri));
            return sendTextMessage(text);
        }
    }

    public boolean sendTextMessage(String text){
        super.addHeader("Content-Type", NgnContentType.TEXT_PLAIN);
        byte[] bytes = text.getBytes();
        ByteBuffer payload = ByteBuffer.allocateDirect(bytes.length);
        payload.put(bytes);
        return mSession.send(payload, payload.capacity());
    }
    
    public boolean accept() {
        return mSession.accept();
      }

      public boolean reject() {
        return mSession.reject();
      }
}
