package com.ncs.client;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class SwingClientApplication {
	public static void main(String[] args) {
		// Set system look and feel
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			// ignore, fallback to default
		}

		SwingUtilities.invokeLater(() -> {
			GameClient gameClient = new GameClient();
			LoginFrame loginFrame = new LoginFrame(gameClient);
			loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			loginFrame.setLocationRelativeTo(null);
			loginFrame.setVisible(true);
		});
	}
}
