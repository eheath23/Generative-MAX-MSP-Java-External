import com.cycling74.max.*;
import com.cycling74.msp.*;
import java.util.*;

public class oscillator extends MSPPerformer {

	private float mSineBuffer[];
	private float mSquareBuffer[];
	private float mSawBuffer[];
	private int mBufferSize;
	private float mPhase;
	private float mSampleRate;
	private float mFrequency;
	private float mSineVol, mSquareVol, mSawVol;
	private float mRamp;

	public oscillator() {

		declareInlets(new int[] { DataTypes.ALL, DataTypes.ALL, DataTypes.ALL, DataTypes.ALL });
		declareOutlets(new int[] { SIGNAL, SIGNAL, SIGNAL });

		setInletAssist(new String[] { "Frequency", "Sine Vol", "Square Vol", "Saw Vol" });
		setOutletAssist(new String[] { "Sine Out", "Square Out", "Saw Out" });

		mBufferSize = 8136;
		mSineBuffer = new float[mBufferSize];
		mSquareBuffer = new float[mBufferSize];
		mSawBuffer = new float[mBufferSize];
		mFrequency = 0;
		mSampleRate = 44100;
		mRamp = 0;

		float unit = (float) (2.0 * Math.PI / mBufferSize);
		for (int i = 0; i < mBufferSize; i++) {
			// Populate Sine Buffer
			mSineBuffer[i] = (float) Math.sin((float) unit * i);

			// Populate Square Buffer
			if (mSineBuffer[i] <= 0) {
				mSquareBuffer[i] = -1.0f;
			} else if (mSineBuffer[i] > 0) {
				mSquareBuffer[i] = 1.0f;
			}

			// Populate Sawtooth Buffer
			if (i == 0) {
				mRamp = -1;
			} else if (i > 0) {
				mRamp += 0.01;
			}
			mSawBuffer[i] = mRamp;
			mRamp = mRamp % mBufferSize;
		}
	}

	public void dspsetup(MSPSignal[] in, MSPSignal[] out) {

	}

	public void perform(MSPSignal[] in, MSPSignal[] out) {

		for (int i = 0; i < out[0].n; i++) {

			float remainder = 0;
			mPhase += 8135.0f / (mSampleRate / (mFrequency));
			if (mPhase >= 8134) {
				mPhase -= 8135.0f;
			}
			remainder = (float) (mPhase - Math.floor(mPhase));
			
			//interpolates signal from lookuptabels
			out[0].vec[i] = ((float) ((1 - remainder) * mSineBuffer[(int) (1 + mPhase)]
					+ remainder * mSineBuffer[(int) (2 + mPhase)])) * mSineVol;
			out[1].vec[i] = ((float) ((1 - remainder) * mSquareBuffer[(int) (1 + mPhase)]
					+ remainder * mSquareBuffer[(int) (2 + mPhase)])) * mSquareVol;
			out[2].vec[i] = ((float) ((1 - remainder) * mSawBuffer[(int) (1 + mPhase)]
					+ remainder * mSawBuffer[(int) (2 + mPhase)])) * mSawVol;

			mPhase = mPhase % mBufferSize;

		}
	}

	public void inlet(int i) {
		int inletNum = getInlet();

		if (inletNum == 0) {
			mFrequency = i;
		}
	}

	public void inlet(float f) {
		int inletNum = getInlet();

		if (inletNum == 0) {
			mSineVol = f;
		} else if (inletNum == 1) {
			mSquareVol = f;
		} else if (inletNum == 2) {
			mSawVol = f;
		}
	}
}
