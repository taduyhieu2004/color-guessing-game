package com.ncs.client;

import com.ncs.model.User;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class LoginFrame extends JFrame implements MessageHandler.LoginEventListener {
	private final GameClient gameClient;

	private final JTextField hostField = new JTextField("localhost", 16);
	private final JTextField portField = new JTextField("8888", 6);
	private final JTextField usernameField = new JTextField(16);
	private final JPasswordField passwordField = new JPasswordField(16);
	private final JButton loginButton = new JButton("Đăng nhập");
	private final JButton registerButton = new JButton("Đăng ký");

	public LoginFrame(GameClient gameClient) {
		super("Color Guessing Game - Login");
		this.gameClient = gameClient;
		this.gameClient.getMessageHandler().setLoginEventListener(this);
		this.gameClient.setOnConnectionLost(() -> SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(this, "Mất kết nối tới server", "Lỗi", JOptionPane.ERROR_MESSAGE);
			setVisible(true);
		}));
		this.gameClient.setOnError(msg -> SwingUtilities.invokeLater(() ->
			JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE))
		);

		// dùng pack() để layout tự tính kích thước chuẩn cho các ô nhập
		setLayout(new BorderLayout());
		add(buildFormPanel(), BorderLayout.CENTER);
		add(buildButtonPanel(), BorderLayout.SOUTH);

		loginButton.addActionListener(e -> doLogin());
		registerButton.addActionListener(e -> doRegister());
		pack();
		setMinimumSize(getSize());
	}

	private JPanel buildFormPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(6, 8, 6, 8);
		gbc.anchor = GridBagConstraints.WEST;

		int row = 0;
		// Server row
		gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; panel.add(new JLabel("Server:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; panel.add(hostField, gbc);
		gbc.gridx = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; panel.add(new JLabel(":"), gbc);
		gbc.gridx = 3; gbc.weightx = 0; gbc.fill = GridBagConstraints.HORIZONTAL; portField.setColumns(6); panel.add(portField, gbc);

		row++;
		gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; panel.add(new JLabel("Tên đăng nhập:"), gbc);
		gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; panel.add(usernameField, gbc); gbc.gridwidth = 1;

		row++;
		gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; panel.add(new JLabel("Mật khẩu:"), gbc);
		gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; panel.add(passwordField, gbc); gbc.gridwidth = 1;

		return panel;
	}

	private JPanel buildButtonPanel() {
		JPanel panel = new JPanel();
		panel.add(loginButton);
		panel.add(registerButton);
		return panel;
	}

	private void doLogin() {
		String host = hostField.getText().trim();
		int port;
		try { port = Integer.parseInt(portField.getText().trim()); }
		catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Port không hợp lệ"); return; }

		gameClient.setServerHost(host.isEmpty() ? "localhost" : host);
		gameClient.setServerPort(port);
		loginButton.setEnabled(false);
		registerButton.setEnabled(false);
		new Thread(() -> gameClient.login(usernameField.getText().trim(), new String(passwordField.getPassword()))).start();
	}

	private void doRegister() {
		String host = hostField.getText().trim();
		int port;
		try { port = Integer.parseInt(portField.getText().trim()); }
		catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Port không hợp lệ"); return; }

		gameClient.setServerHost(host.isEmpty() ? "localhost" : host);
		gameClient.setServerPort(port);
		loginButton.setEnabled(false);
		registerButton.setEnabled(false);
		new Thread(() -> gameClient.register(usernameField.getText().trim(), new String(passwordField.getPassword()), null, null)).start();
	}

	// LoginEventListener
	@Override public void onLoginSuccess(User user) {
		SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(this, "Đăng nhập thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
			MainFrame main = new MainFrame(gameClient, user);
			main.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			main.setLocationRelativeTo(null);
			main.setVisible(true);
			dispose();
		});
	}
	@Override public void onLoginFailure(String message) {
		SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(this, "Đăng nhập thất bại: " + message, "Lỗi", JOptionPane.ERROR_MESSAGE);
			loginButton.setEnabled(true);
			registerButton.setEnabled(true);
		});
	}
	@Override public void onRegisterSuccess(User user) {
		SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(this, "Đăng ký thành công! Hãy đăng nhập.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
			usernameField.setText(user.getUsername());
			loginButton.setEnabled(true);
			registerButton.setEnabled(true);
		});
	}
	@Override public void onRegisterFailure(String message) {
		SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(this, "Đăng ký thất bại: " + message, "Lỗi", JOptionPane.ERROR_MESSAGE);
			loginButton.setEnabled(true);
			registerButton.setEnabled(true);
		});
	}
	@Override public void onLogout() { }
}
