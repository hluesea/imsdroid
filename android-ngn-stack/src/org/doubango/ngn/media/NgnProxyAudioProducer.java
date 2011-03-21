package org.doubango.ngn.media;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import org.doubango.ngn.NgnApplication;
import org.doubango.ngn.NgnEngine;
import org.doubango.ngn.utils.NgnConfigurationEntry;
import org.doubango.tinyWRAP.ProxyAudioProducer;
import org.doubango.tinyWRAP.ProxyAudioProducerCallback;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;


/**
 * MyProxyAudioProducer
 */
public class NgnProxyAudioProducer extends NgnProxyPlugin{
	private static final String TAG = NgnProxyAudioProducer.class.getCanonicalName();
	private final static int AUDIO_BUFFER_FACTOR = 3;
	private final static int AUDIO_MIN_VALID_BUFFER_SIZE = 4096;
	private final static int AUDIO_DEFAULT_BUFFER_SIZE = 6200;
	
	private final MyProxyAudioProducerCallback mCallback;
	private final ProxyAudioProducer mProducer;
	private boolean mRoutingChanged;
	
	private AudioRecord mAudioRecorder;
	private ByteBuffer mAudioFrame;
	private int mPtime, mRate, mChannels;
	
	public NgnProxyAudioProducer(BigInteger id, ProxyAudioProducer producer){
		super(id, producer);
		mProducer = producer;
        mCallback = new MyProxyAudioProducerCallback(this);
        mProducer.setCallback(mCallback);
	}
	
	public void setOnPause(boolean pause){
		if(super.mPaused == pause){
			return;
		}
		try {
			if(this.mStarted){
				
			}
		} catch(Exception e){
			Log.e(TAG, e.toString());
		}
		
		super.mPaused = pause;
	}
	
	public void setSpeakerphoneOn(boolean speakerOn){
		Log.d(TAG, "setSpeakerphoneOn("+speakerOn+")");
		if (NgnApplication.isAudioRecreateRequired()){
			if(super.mPrepared){
				mRoutingChanged = true;
			}
		}
	}
	
	public void toggleSpeakerphone(){
		setSpeakerphoneOn(!NgnApplication.getAudioManager().isSpeakerphoneOn());
	}
	
	private int prepareCallback(int ptime, int rate, int channels){
    	Log.d(NgnProxyAudioProducer.TAG, "prepareCallback("+ptime+","+rate+","+channels+")");
    	return prepare(ptime, rate, channels);
    }

	private int startCallback(){
    	Log.d(NgnProxyAudioProducer.TAG, "startCallback");
    	if(mPrepared && mAudioRecorder != null){
			super.mStarted = true;
			final Thread t = new Thread(mRunnableRecorder, "AudioProducerThread");
			// t.setPriority(Thread.MAX_PRIORITY);
			t.start();
			return 0;
		}
        return -1;
    }

	private int pauseCallback(){
    	Log.d(NgnProxyAudioProducer.TAG, "pauseCallback");
    	setOnPause(true);
        return 0;
    }

	private int stopCallback(){
    	Log.d(NgnProxyAudioProducer.TAG, "stopCallback");
    	super.mStarted = false;
		if(mAudioRecorder != null){
			return 0;
		}
		return -1;
    }
	
	private synchronized int prepare(int ptime, int rate, int channels){
		if(super.mPrepared){
			Log.e(TAG,"already prepared");
			return -1;
		}
		
		final int minBufferSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		final int shortsPerNotif = (rate * ptime)/1000;
		int bufferSize = (minBufferSize + (shortsPerNotif - (minBufferSize % shortsPerNotif))) * NgnProxyAudioProducer.AUDIO_BUFFER_FACTOR;
	    if(bufferSize <= AUDIO_MIN_VALID_BUFFER_SIZE){
	    	bufferSize = AUDIO_DEFAULT_BUFFER_SIZE;
	     }
		
		mAudioFrame = ByteBuffer.allocateDirect(shortsPerNotif*2);
		mPtime = ptime; mRate = rate; mChannels = channels;
		
		mAudioRecorder = new AudioRecord(
				// WiPhone.isAGCSupported() ? MediaRecorder.AudioSource.VOICE_RECOGNITION : MediaRecorder.AudioSource.MIC,
				MediaRecorder.AudioSource.MIC,
				rate, 
				AudioFormat.CHANNEL_IN_MONO, 
				AudioFormat.ENCODING_PCM_16BIT,
				bufferSize);
		if(mAudioRecorder.getState() == AudioRecord.STATE_INITIALIZED){
			super.mPrepared = true;
			return 0;
		}
		else{
			Log.e(TAG, "prepare("+mAudioRecorder.getState()+") failed");
			super.mPrepared = false;
			return -1;
		}
	}
	
	private synchronized void unprepare(){
		if(mAudioRecorder != null){
			if(super.mPrepared){ // only call stop() is the AudioRecord is in initialized state
				mAudioRecorder.stop();
			}
			mAudioRecorder.release();
			mAudioRecorder = null;
		}
		super.mPrepared = false;
	}
	
	private Runnable mRunnableRecorder = new Runnable(){
		@Override
		public void run() {
			Log.d(TAG, "===== Audio Recorder (Start) ===== ");
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
			
			mAudioRecorder.startRecording();
			final int nSize = mAudioFrame.capacity();
			int nRead;
			
			if(NgnProxyAudioProducer.super.mValid){
				mProducer.setPushBuffer(mAudioFrame, mAudioFrame.capacity());
				mProducer.setGain(NgnEngine.getInstance().getConfigurationService().getInt(NgnConfigurationEntry.MEDIA_AUDIO_PRODUCER_GAIN, 
						NgnConfigurationEntry.DEFAULT_MEDIA_AUDIO_PRODUCER_GAIN));
			}
			
			while(NgnProxyAudioProducer.super.mValid && mStarted){
				if(mAudioRecorder == null){
					break;
				}
				
				if(mRoutingChanged){
					Log.d(TAG, "Routing changed: restart() recorder");
					mRoutingChanged = false;
					unprepare();
					if(prepare(mPtime, mRate, mChannels) != 0){
						break;
					}
					if(!NgnProxyAudioProducer.super.mPaused){
						mAudioRecorder.startRecording();
					}
				}
				
				// To avoid overrun read data even if on pause
				if((nRead = mAudioRecorder.read(mAudioFrame, nSize)) > 0){
					if(!NgnProxyAudioProducer.super.mPaused){
						if(nRead != nSize){
							mProducer.push(mAudioFrame, nRead);
							Log.w(TAG, "BufferOverflow?");
						}
						else{
							mProducer.push();
						}
					}
				}
			}
			
			unprepare();
			
			Log.d(TAG, "===== Audio Recorder (Stop) ===== ");
		}
	};
	
	
	/**
	 * MyProxyAudioProducerCallback
	 */
	static class MyProxyAudioProducerCallback extends ProxyAudioProducerCallback
    {
        final NgnProxyAudioProducer myProducer;

        public MyProxyAudioProducerCallback(NgnProxyAudioProducer producer){
        	super();
            myProducer = producer;
        }

        @Override
        public int prepare(int ptime, int rate, int channels){
            return myProducer.prepareCallback(ptime, rate, channels);
        }

        @Override
        public int start(){
            return myProducer.startCallback();
        }

        @Override
        public int pause(){
            return myProducer.pauseCallback();
        }

        @Override
        public int stop(){
            return myProducer.stopCallback();
        }
    }
}
