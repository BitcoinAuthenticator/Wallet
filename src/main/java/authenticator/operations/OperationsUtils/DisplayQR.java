package authenticator.operations.OperationsUtils;

import java.awt.EventQueue;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;

/**Class creates a simple JFrame to display the QR code to the user*/
@SuppressWarnings("serial")
public class DisplayQR extends JFrame {


	private JPanel contentPane;
	static DisplayQR frame = new DisplayQR();
	

	/**Launches the Jframe*/
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
				frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**Loads the QR code image from file and creates the frame.*/
	public DisplayQR() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 310, 310);
		contentPane = new JPanel();
		contentPane.setFocusable(false);
		setContentPane(contentPane);
		String path = "";
		try {
			path = new java.io.File( "." ).getCanonicalPath() + "/PairingQRCode.png";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			ImageIcon image = new ImageIcon(path);
			JLabel lblQRCode = new JLabel(image, JLabel.CENTER);
			lblQRCode.setBounds(0, 0, 350, 350);
			contentPane.add(lblQRCode);
		}
	}
	
	/**Closes the window displaying the QR code*/
	public void CloseWindow(){
		frame.setVisible(false);
	}

}
