import com.cycling74.max.*;
import com.cycling74.msp.*;
import java.util.*;

public class GenOsc extends MSPPerformer {

	// Variables for Oscillator
	private float mSineBuffer[];
	private float mSquareBuffer[];
	private float mSawBuffer[];
	private int mBufferSize;
	private float mPhase;
	private float mSampleRate;
	private float mFrequency;
	private float mSineVol, mSquareVol, mSawVol;
	private float mRamp;

	// Variables for Euclidian
	private ArrayList<Integer> mECounts;
	private ArrayList<Integer> mERemainders;
	private String mEPattern;
	private int mEStep;

	// Variables for Johnsonizer
	private HashMap<String, String> mJRules;
	private String mJPattern;
	private int mJLongestKey;
	private int mJStep;

	private int mOctave;
	private int mScaleNum;
	private String[] mScaleNames = { "C", "D", "E", "F", "G", "A", "B" };

	private int[][] mMajorScales = { { 48, 50, 52, 53, 55, 57, 59, 60 }, { 50, 52, 54, 55, 57, 59, 61, 62 },
			{ 52, 54, 56, 57, 59, 61, 63, 64 }, { 53, 55, 57, 58, 60, 62, 64, 65 }, { 55, 57, 59, 60, 62, 64, 66, 67 },
			{ 57, 59, 61, 62, 64, 66, 68, 69 }, { 59, 61, 63, 64, 66, 68, 70, 71 } };

	private int[][] mMinorScales = { { 48, 50, 52, 53, 55, 57, 59, 60 }, { 50, 52, 54, 55, 57, 59, 61, 62 },
			{ 52, 54, 56, 57, 59, 61, 63, 64 }, { 53, 55, 57, 58, 60, 62, 64, 65 }, { 55, 57, 59, 60, 62, 64, 66, 67 },
			{ 57, 59, 61, 62, 64, 66, 68, 69 }, { 59, 61, 63, 64, 66, 68, 70, 71 } };

	private int mInput1, mInput2;

	public GenOsc() {

		declareInlets(new int[] { DataTypes.ALL, DataTypes.ALL, DataTypes.ALL, DataTypes.ALL, DataTypes.ALL,
				DataTypes.ALL, DataTypes.ALL, DataTypes.ALL, DataTypes.ALL, DataTypes.ALL });
		declareOutlets(new int[] { SIGNAL, DataTypes.ALL, DataTypes.ALL, DataTypes.ALL });

		setInletAssist(new String[] { "Frequency", "Sine Vol", "Square Vol", "Saw Vol", "Octave & Transform", "Beats",
				"Pulses", "Initialise", "Add Rules" });
		setOutletAssist(new String[] { "Output", "Euclidian Rhythm", "Johnsonizer Rhythm", "Scale" });

		// Euclidian Variables
		mECounts = new ArrayList<Integer>();
		mERemainders = new ArrayList<Integer>();
		mEPattern = "";
		mEStep = 0;

		// Johnsonizer Variables
		mJRules = new HashMap<String, String>();
		mJPattern = "";
		mJLongestKey = 0;
		mJStep = 0;

		// Look up tables
		mBufferSize = 8136;
		mSineBuffer = new float[mBufferSize];
		mSquareBuffer = new float[mBufferSize];
		mSawBuffer = new float[mBufferSize];

		// Oscillators Variables
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

	// ------------------------------------------------------------
	public void dspsetup(MSPSignal[] in, MSPSignal[] out) {

	}

	// ------------------------------------------------------------
	public void perform(MSPSignal[] in, MSPSignal[] out) {

		for (int i = 0; i < out[0].n; i++) {

			float remainder = 0;
			mPhase += 8135.0f / (mSampleRate / (mFrequency));
			if (mPhase >= 8134) {
				mPhase -= 8135.0f;
			}
			remainder = (float) (mPhase - Math.floor(mPhase));

			// interpolates signal from look-up tables
			out[0].vec[i] = ((float) ((1 - remainder) * mSineBuffer[(int) (1 + mPhase)]
					+ remainder * mSineBuffer[(int) (2 + mPhase)])) * mSineVol;
//			out[1].vec[i] = ((float) ((1 - remainder) * mSquareBuffer[(int) (1 + mPhase)]
//					+ remainder * mSquareBuffer[(int) (2 + mPhase)])) * mSquareVol;
//			out[2].vec[i] = ((float) ((1 - remainder) * mSawBuffer[(int) (1 + mPhase)]
//					+ remainder * mSawBuffer[(int) (2 + mPhase)])) * mSawVol;

			mPhase = mPhase % mBufferSize;

		}
	}

	// ------------------------------------------------------------
	public void inlet(int i) {
		int inletNum = getInlet();

		if (inletNum == 0) {
			mFrequency = i;
		} else if (inletNum == 5) {
			mInput1 = i;
		} else if (inletNum == 6) {
			mInput2 = i;
		}

		if (inletNum == 5 || inletNum == 6) {
			calcPattern(mInput1, mInput2);
		}
	}

	// ------------------------------------------------------------
	public void inlet(float f) {
		int inletNum = getInlet();

		if (inletNum == 1) {
			mSineVol = f;
		} else if (inletNum == 2) {
			mSquareVol = f;
		} else if (inletNum == 3) {
			mSawVol = f;
		}
	}

	// ------------------------------------------------------------
	public void bang() {
		// Output pattern
		if (mEPattern == "") {
			return;
		} else {
			if (mEPattern.charAt(mEStep) == '1') {
				if (mJPattern.length() > 0) {
					int midiIn = mJPattern.charAt(mJStep);
//					outlet(0, toMidi(midiIn));
					mJStep = (mJStep + 1) % mJPattern.length();
				}
			}
			mEStep += 1;
			mEStep = mEStep % mEPattern.length();

			getPattern();
		}
	}

	// ------------------------------------------------------------
	private void calcPattern(int steps, int beats) {
		mECounts.clear();
		mERemainders.clear();
		mEPattern = "";
		mEStep = 0;

		int divisor = steps - beats;
		mERemainders.add(beats);
		int level = 0;

		while (mERemainders.get(level) > 1) {
			mECounts.add(divisor / mERemainders.get(level));
			mERemainders.add(divisor % mERemainders.get(level));
			divisor = mERemainders.get(level);
			level = level + 1;
		}

		mECounts.add(divisor);
		buildString(level);
	}

	// ------------------------------------------------------------
	private void buildString(int level) {
		if (level == -1) {
			mEPattern = mEPattern + "0";
		} else if (level == -2) {
			mEPattern = mEPattern + "1";
		} else {
			for (int i = 0; i < mECounts.get(level); i++) {
				buildString(level - 1);
			}

			if (mERemainders.get(level) != 0) {
				buildString(level - 2);
			}
		}
	}

	// ------------------------------------------------------------
	private void getPattern() {
		outlet(1, "Euclidian rhythm: " + mEPattern);
		outlet(2, "Johnizer current pattern: " + mJPattern);
		outlet(3, "Scale: " + mScaleNames[mScaleNum] + "major");
	}

	// ------------------------------------------------------------
	private void clearRules() {
		mJRules.clear();
		mJLongestKey = 0;
	}

	// ------------------------------------------------------------
	public void addRules(Atom atoms[]) {
		if (atoms.length % 2 != 0) {
			post("Johnsonizer: pass in pairs for rules");
			return;
		}

		clearRules();

		for (int i = 0; i < atoms.length; i += 2) {
			// adds rules with keys and pairs
			String key = atoms[i].toString();
			mJRules.put(key, atoms[i + 1].toString());

			if (key.length() > mJLongestKey) {
				mJLongestKey = key.length();
			}

			post(atoms[i].toString() + "-->" + atoms[i + 1].toString());
		}

		validateRules();

	}

	// ------------------------------------------------------------
	private void validateRules() {

		// check that the rules work

		Collection<String> values = mJRules.values();

		for (String v : values) {

			String working_v = v;

			while (working_v.length() > 0) {
				String s = searchForKey(working_v);
				if (s.length() == 0) {
					post("Johsonizer: invalid rule set, " + v + "has no solution.");
					return;
				}
				working_v = working_v.substring(s.length());
			}

		}

		post("Johnsonizer: confirm valid rules");

	}

	// ------------------------------------------------------------
	public void init(Atom[] arg) {
		// set a new initial value
		mJPattern = arg[0].toString();
		mJStep = 0;
		post("init working");
	}

	// ------------------------------------------------------------
	public void transform() {
		// transform the pattern using the rules
		String workingPattern = mJPattern;
		String newPattern = "";

		while (workingPattern.length() > 0) {

			String k = searchForKey(workingPattern);

			if (k.length() == 0) {
				break;
			}

			newPattern += mJRules.get(k);
			workingPattern = workingPattern.substring(k.length());

		}

		mJPattern = newPattern;
	}

	// ------------------------------------------------------------
	public int toMidi(int patternIn) {
		int midiIn = patternIn % 49;

		if (patternIn > 0) {
			return mMajorScales[mScaleNum][midiIn] + mOctave;
		}
		return 0;
	}

	public int toFreq(int midiIn) {

		return 0;
	}

	// ------------------------------------------------------------
	public void scale(int scaleNum) {
		mScaleNum = scaleNum;
	}

	// ------------------------------------------------------------
	public void octave(int octNum) {
		mOctave = octNum;
		mOctave = mOctave * 12;
	}

	// HELPERS
	// ------------------------------------------------------------
	private String searchForKey(String k) {
		// find and return the longest possible key for a given string
		for (int i = mJLongestKey; i > 0; i--) {

			String s = k.substring(0, i);

			String o = mJRules.get(s);
			if (o != null) {
				return s;
			}

		}
		post("Johnsonizer: no output for pattern " + k);
		return "";
	}

}
