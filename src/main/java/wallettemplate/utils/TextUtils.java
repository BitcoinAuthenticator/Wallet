package wallettemplate.utils;

import java.math.BigInteger;

import authenticator.protobuf.ProtoSettings;
import authenticator.protobuf.ProtoSettings.BitcoinUnit;

import com.google.bitcoin.core.Coin;

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
	 * Coin is the amount in sathosies
	 * 
	 * @param coin
	 * @param unit
	 * @return
	 */
	public static String coinAmountTextDisplay(Coin coin, BitcoinUnit unit) {
		double i = satoshiesToBitcoinUnit(coin.value, unit);
		return String.format( "%.4f", i ) + " " + unit.getValueDescriptor().getOptions().getExtension(ProtoSettings.bitcoinUnitName);
	}
	
	/**
	 * convert an amount represented by a unit to satoshies, e.g. 100 mBits will become 10,000,000 satoshies, 
	 * 
	 * @param in
	 * @param unit
	 * @return
	 */
	public static double bitcoinUnitToSatoshies(double in, BitcoinUnit unit) {
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
		
		return in;
	}
	
	/**
	 * convert an amount in satoshies to unit amount, e.g. 10,000,000 satoshies to 100 mBits.
	 * 
	 * @param in
	 * @param unit
	 * @return
	 */
	public static double satoshiesToBitcoinUnit(double in, BitcoinUnit unit) {
		switch(unit) {
			case BTC:
				in /= 100000000;
				break;
			case Millibits:
				in /= 100000;
				break;
			case Microbits:
				in /= 100;
				break;
		}
		
		return in;
	}
}