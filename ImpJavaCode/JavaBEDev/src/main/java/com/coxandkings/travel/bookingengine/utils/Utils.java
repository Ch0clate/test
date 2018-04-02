package com.coxandkings.travel.bookingengine.utils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

public class Utils {
	
	private static MathContext mathCtx = new MathContext(0, RoundingMode.UP);
	
	
	public static int convertToInt(String strVal, int dftVal) {
		try {
			return Integer.valueOf(strVal);
		}
		catch (NumberFormatException nfx) {
			return dftVal;
		}
	}

	public static BigDecimal convertToBigDecimal(String strVal, int precision, int dftVal) {
		if (isStringNullOrEmpty(strVal)) {
			return new BigDecimal(dftVal);
		}
		
		try {
			return new BigDecimal(strVal, new MathContext(precision));
		}
		catch (NumberFormatException nfx) {
			return new BigDecimal(dftVal);
		}
	}

	
	public static BigDecimal convertToBigDecimal(String strVal, int dftVal) {
		return convertToBigDecimal(strVal, 6, dftVal);
	}
	
	public static int convertAndRoundToInt(String strVal, int dftVal) {
		try {
			BigDecimal bigDec = new BigDecimal(strVal);
			return bigDec.round(mathCtx).intValue();
		}
		catch (NumberFormatException nfx) {
			return dftVal;
		}
	}
	
	public static String concatSequence(List<String> seq, char sepChar) {
		if (seq == null) {
			return "";
		}
		
		StringBuilder strBldr = new StringBuilder();
		for (int i=0; i < seq.size(); i++) {
			strBldr.append(seq.get(i));
			strBldr.append(sepChar);
		}
		
		strBldr.setLength((strBldr.length() > 0) ? strBldr.length() - 1 : 0);
		return strBldr.toString();
	}

	public static boolean isStringNotNullAndNotEmpty(String str) {
		return ! isStringNullOrEmpty(str);
	}

	public static boolean isStringNullOrEmpty(String str) {
		return (str == null || str.isEmpty());
	}
}
