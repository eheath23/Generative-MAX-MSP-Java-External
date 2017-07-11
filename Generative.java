import com.cycling74.max.*;
import java.util.*;

public class Generative extends MaxObject {

	private ArrayList<Integer> mECounts;
	private ArrayList<Integer> mERemainders;
	private String mEPattern;
	private int mEStep;

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
	private int mRSize, mRDecay, mRDamp, mRDiff;
//	private Random mRndm;

	// ------------------------------------------------------------
	Generative() {
		declareIO(5, 8);

		mECounts = new ArrayList<Integer>();
		mERemainders = new ArrayList<Integer>();
		mEPattern = "";
		mEStep = 0;

		mJRules = new HashMap<String, String>();
		mJPattern = "";
		mJLongestKey = 0;
		mJStep = 0;

//		Random mRndm = new Random();

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
					outlet(0, toMidi(midiIn));
					mJStep = (mJStep + 1) % mJPattern.length();
				}
			}
			mEStep += 1;
			mEStep = mEStep % mEPattern.length();

			getPattern();
//			reverbControl();
		}

	}

	// ------------------------------------------------------------
	public void inlet(int i) {
		int inletNum = getInlet();

		if (inletNum == 1) {
			mInput1 = i;
		} else if (inletNum == 2) {
			mInput2 = i;
		}

		if (inletNum == 1 || inletNum == 2) {
			calcPattern(mInput1, mInput2);
		}
	}

	// ------------------------------------------------------------
	// public void list(Atom[] list) {
	// //
	// int inletNum = getInlet();
	//
	// String s = "List: ";
	//
	// for(int i = 0; i < list.length; i++){
	// s += list[i].toString();
	//
	// }
	// post(s);
	//
	//
	// if (inletNum == 3) {
	// init(list);
	//
	// } else if (inletNum == 4) {
	// addRules(list);
	//
	// }
	//
	// }

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

	// ------------------------------------------------------------
	public void scale(int scaleNum) {
		mScaleNum = scaleNum;
	}

	// ------------------------------------------------------------
	public void octave(int octNum) {
		mOctave = octNum;
		mOctave = mOctave * 12;
	}

//	public void reverbControl() {
//		mRSize = mRndm.nextInt(255);
//		mRDecay = mRndm.nextInt(255);
//		mRDamp = mRndm.nextInt(255);
//		mRDiff = mRndm.nextInt(255);
//		
//		post("" + mRSize);
//
//		outlet(5, mRSize);
//		outlet(6, mRDecay);
//		outlet(7, mRDamp);
//		outlet(8, mRDiff);
//	}

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
