package nl.tudelft.otsim.Utilities;

import java.util.ArrayList;
import java.util.Locale;

import nl.tudelft.otsim.FileIO.ParsedNode;
import nl.tudelft.otsim.FileIO.StaXWriter;
import nl.tudelft.otsim.FileIO.XML_IO;
import nl.tudelft.otsim.GUI.Storable;

/**
 * This class holds a list of time/factor pairs and allows retrieval of 
 * (interpolated) factor values for any time.
 * 
 * @author Peter Knoppers
 */
public class TimeScaleFunction implements XML_IO {
	/** Tag of a TimeScaleFunction when stored in XML */
	public static final String XMLTAG = "TimeFactorScaleFunction";
	private static final String XML_TIMEFACTORPAIR = "Pair";
	private static final String XML_TIME = "Time";
	private static final String XML_FACTOR = "Factor";
	private ArrayList<Double> times = new ArrayList<Double>();
	private ArrayList<Double> factors = new ArrayList<Double>();
	private final Storable storable;
	private final static String timeFormat = "%.3f"; 
	private final static String factorFormat = "%.6f";
	
	/**
	 * Create an empty instance of a TimeScaleFunction.
	 * @param storable {@link Storable}; the Storable that will be notified on changes to this TimeScaleFunction (may be null)
	 */
	public TimeScaleFunction(Storable storable) {
		this.storable = storable;
	}
	
	/**
	 * Create a TimeScaleFunction from an XML description
	 * @param storable {@link Storable}; the Storable that will be notified of changes in this TimeScaleFunction (may be null)
	 * @param pn {@link ParsedNode} XML node of the TimeScaleFunction object 
	 * @throws Exception
	 */
	public TimeScaleFunction(Storable storable, ParsedNode pn) throws Exception {
		this(storable);
		for (int index = 0; index < pn.size(XML_TIMEFACTORPAIR); index++) {
			ParsedNode subNode = pn.getSubNode(XML_TIMEFACTORPAIR, index);
			double time = Double.NaN;
			double factor = Double.NaN;
			ParsedNode valueNode = subNode.getSubNode(XML_TIME, 0);
			if (null != valueNode)
				time = Double.parseDouble(valueNode.getValue());
			valueNode = subNode.getSubNode(XML_FACTOR, 0);
			if (null != valueNode)
				factor = Double.parseDouble(valueNode.getValue());
			if (Double.isNaN(time) || Double.isNaN(factor))
				throw new Exception("incompletely defined time/factor pair near " + subNode.description());
			insertPair(time, factor);
		}
	}
	
	/**
	 * Create a TimeScaleFunction from a textual description with no Storable.
	 * <br /> This can be useful in traffic generators in the traffic simulators.
	 * @param description
	 */
	public TimeScaleFunction (String description) {
		storable = null;
		String pairs[] = description.split("\t");
		for(String pair : pairs) {
			String fields[] = pair.split("/");
			double time = Double.parseDouble(fields[0]);
			double factor = Double.parseDouble(fields[1]);
			insertPair(time, factor);
		}
	}
	
	/**
	 * Insert a time/factor pair.
	 * <br /> If several values are inserted with the exact same time, the 
	 * order of the stored time/factor pairs is undefined.
	 * @param time Double; the time (in s relative to simulation start time)
	 * @param factor Double; the factor
	 */
	public void insertPair (double time, double factor) {
		int position;
		for (position = 0; (position < times.size()) && (time > times.get(position)); position++)
			;
		times.add(position, time);
		factors.add(position, factor);
		if (null != storable)
			storable.setModified();
	}
	
	/**
	 * Retrieve the number of time/factor pairs stored.
	 * @return Integer; the number of time/factor pairs stored.
	 */
	public int size() {
		return times.size();
	}
	
	/**
	 * Retrieve the time of a particular time/factor pair.
	 * @param index Integer; the index of the time/factor pair
	 * @return Double; the time of the selected time/factor pair
	 */
	public double getTime(int index) {
		return times.get(index);
	}
	
	/**
	 * Retrieve the factor of a particular time/factor pair.
	 * @param index Integer; the index of the time/factor pair
	 * @return Double; the factor of the selected time/factor pair
	 */
	public double getFactor(int index) {
		return factors.get(index);
	}

	/**
	 * Remove a time/factor pair.
	 * @param index Integer; the index of the time/factor pair to remove
	 */
	public void deletePair (int index) {
		times.remove(index);
		factors.remove(index);
		if (null != storable)
			storable.setModified();
	}
	
	/**
	 * Retrieve the factor at a specified time.
	 * <br />Before the first specified time and after the last specified time
	 * the returned factor value remains constant.
	 * @param time Double; the time in s relative to simulation start time
	 * @return Double; the (interpolated) factor at the specified time 
	 */
	public double getFactor(double time) {
		if (factors.size() == 0)
			return 1.0;		// Trivial case
		double prevTime = 0;
		double prevFactor = factors.get(0);
		for (int i = 0; i < times.size(); i++) {
			Double thisTime = times.get(i);
			Double thisFactor = factors.get(i);
			if (thisTime <= time) {
				prevTime = thisTime;
				prevFactor = thisFactor;
			} else if (thisTime > prevTime )
				return prevFactor + (thisFactor - prevFactor) * (time - prevTime) / (thisTime - prevTime);
			else
				return thisFactor;
		}
		return prevFactor;
	}
	
	/**
	 * Determine if this TimeScaleFunction returns 1.0 for all time values.
	 * @return Boolean; true if this TimeScaleFunction always return 1.0
	 */
	public boolean isTrivial() {
		for (Double factor : factors)
			if (1.0d != factor)
				return false;
		return true;
	}

	private boolean writePairs(StaXWriter staXWriter) {
		for (int index = 0; index < size(); index++) {
			if (! staXWriter.writeNodeStart(XML_TIMEFACTORPAIR))
				return false;
			if (! staXWriter.writeNode(XML_TIME, String.format(Locale.US, timeFormat, getTime(index))))
				return false;
			if (! staXWriter.writeNode(XML_FACTOR, String.format(Locale.US, factorFormat, getFactor(index))))
				return false;
			if (! staXWriter.writeNodeEnd(XML_TIMEFACTORPAIR))
				return false;
		}
		return true;
	}
	
	@Override
	public boolean writeXML(StaXWriter staXWriter) {
		return staXWriter.writeNodeStart(XMLTAG)
				&& writePairs(staXWriter)
				&& staXWriter.writeNodeEnd(XMLTAG);
	}
	
	/**
	 * Export this TimeScaleFunction in a textual format.
	 * @return String; this TimeScaleFunction in a textual format
	 */
	public String export () {
		String result = "";
		final String formatPair = timeFormat + "/" + factorFormat;
		for (int i = 0; i < size(); i++) {
			double time = times.get(i);
			double factor = factors.get(i);
			result += String.format(Locale.US, "%s" + formatPair, result.length() > 0 ? "\t" : "", time, factor);
		}
		return result;
	}
	
}

