package edu.rutgers.MOST.presentation;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class NotFoundYesNoDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public JButton yesButton = new JButton(" Yes ");
	public JButton noButton = new JButton(" No ");
	
	public NotFoundYesNoDialog() {
		// need to set up box layout
		setTitle("Item Not Found");
		
		getRootPane().setDefaultButton(yesButton);
		
		//box layout
		Box vb = Box.createVerticalBox();

		Box hbLabel = Box.createHorizontalBox();
		Box hbButton = Box.createHorizontalBox();
		
		JLabel label = new JLabel("<html><p>MOST has not found no more occurrences of the item you are searching for.<p>Do you want to start over from the beginning?");
		
		JPanel labelPanel = new JPanel();
		labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
		labelPanel.add(label);
		labelPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 0, 10));
		
		hbLabel.add(labelPanel);
		
		JLabel blankLabel = new JLabel("   ");
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.add(yesButton);
		buttonPanel.add(blankLabel);
		buttonPanel.add(noButton);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(10,20,20,20));

		hbButton.add(buttonPanel);
		
		vb.add(hbLabel);
		vb.add(hbButton);

		add(vb);
		
//		ActionListener yesButtonActionListener = new ActionListener() {
//			public void actionPerformed(ActionEvent prodActionEvent) {
//				setVisible(false);
//				dispose();
//			}
//		};
//		
//		yesButton.addActionListener(yesButtonActionListener);
//		
//		ActionListener noButtonActionListener = new ActionListener() {
//			public void actionPerformed(ActionEvent prodActionEvent) {
//				setVisible(false);
//				//dispose();
//			}
//		};
//		
//		noButton.addActionListener(noButtonActionListener);
		
	}
	
	public static void main(String[] args) {
		NotFoundYesNoDialog d = new NotFoundYesNoDialog();
		d.pack();
		d.setVisible(true);
	}
	
}


