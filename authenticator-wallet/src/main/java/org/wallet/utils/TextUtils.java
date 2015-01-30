package org.wallet.utils;

import org.authenticator.Authenticator;
import org.authenticator.protobuf.ProtoSettings;
import org.authenticator.protobuf.ProtoSettings.BitcoinUnit;

import org.bitcoinj.core.Coin;

import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;

public class TextUtils {

	static final Text helper;
	static final double DEFAULT_WRAPPING_WIDTH;
	static final double DEFAULT_LINE_SPACING;
	static final String DEFAULT_TEXT;
	static final TextBoundsType DEFAULT_BOUNDS_TYPE;
	static {
	    helper = new Text();
	    DEFAULT_WRAPPING_WIDTH = helper.getWrappingWidth();
	    DEFAULT_LINE_SPACING = helper.getLineSpacing();
	    DEFAULT_TEXT = helper.getText();
	    DEFAULT_BOUNDS_TYPE = helper.getBoundsType();
	}

	public static double computeTextWidth(Font font, String text, double help0) {
	    // Toolkit.getToolkit().getFontLoader().computeStringWidth(field.getText(),
	    // field.getFont());
	
	    helper.setText(text);
	    helper.setFont(font);
	
	    helper.setWrappingWidth(0.0D);
	    helper.setLineSpacing(0.0D);
	    double d = Math.min(helper.prefWidth(-1.0D), help0);
	    helper.setWrappingWidth((int) Math.ceil(d));
	    d = Math.ceil(helper.getLayoutBounds().getWidth());
	
	    helper.setWrappingWidth(DEFAULT_WRAPPING_WIDTH);
	    helper.setLineSpacing(DEFAULT_LINE_SPACING);
	    helper.setText(DEFAULT_TEXT);
	    return d;
	}
	
	/**
	 * convert satoshies (extracted from {@link org.bitcoinj.core.Coin Coin}) to unit and stringify it
	 * 
	 * @param coin
	 * @param unit
	 * @return
	 */
	public static String coinToUnitString(Coin coin, BitcoinUnit unit) {
		double i = satoshiesToUnit(coin.value, unit);
		String dp = Integer.toString(Authenticator.getWalletOperation().getDecimalPointFromSettings());
		
		return String.format( "%." + dp +"f", i ) + " " + unit.getValueDescriptor().getOptions().getExtension(ProtoSettings.bitcoinUnitName);
	}
	
	/**
	 * convert an amount represented by a unit to satoshies, e.g. 100 mBits will become 10,000,000 satoshies, 
	 * 
	 * @param in
	 * @param unit
	 * @return
	 */
	public static long unitToSatoshies(float in, BitcoinUnit unit) {
		switch(unit) {
			case BTC:
				in *= 100000000;
				break;
			case Millibits:
				in *= 100000;
				break;
			case Microbits:
				in *= 100;
				break;
		}
		
		return (long)in;
	}
	
	/**
	 * convert an amount in satoshies to unit amount, e.g. 10,000,000 satoshies to 100 mBits.
	 * 
	 * @param in
	 * @param unit
	 * @return
	 */
	public static float satoshiesToUnit(long in, BitcoinUnit unit) {
		float ret = 0;
		switch(unit) {
			case BTC:
				ret = (float)in / (float)100000000;
				break;
			case Millibits:
				ret = (float)in / (float)100000;
				break;
			case Microbits:
				ret = (float)in / (float)100;
				break;
		}
		
		return ret;
	}
	
	public static String getAbbreviatedUnit (BitcoinUnit unit){
		if (unit == BitcoinUnit.BTC){return "BTC";}
		else if (unit == BitcoinUnit.Millibits){return "mBTC";}
		else if (unit == BitcoinUnit.Microbits){return "µBTC";}
		return null;
	}
}