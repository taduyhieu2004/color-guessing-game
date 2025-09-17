package com.ncs.client;

import com.ncs.model.GameColor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GamePlayFrame extends JFrame implements MessageHandler.GameEventListener {
	private final GameClient gameClient;
	private final MainFrame mainFrame;

	// Top area
	private final JLabel infoLabel = new JLabel("Chuẩn bị...");
	private final JLabel scoreLabelP1 = new JLabel("Bạn: 0.0");
	private final JLabel scoreLabelP2 = new JLabel("Đối thủ: 0.0");
	private final JButton leaveButton = new JButton("Rời trận");
	private final JButton rematchButton = new JButton("Yêu cầu chơi lại");
	private final JButton backButton = new JButton("Về màn chính");

	// Content
	private final JPanel colorPanel = new JPanel(new GridLayout(1, 3, 8, 8));
	private final JPanel palettePanel = new JPanel(new GridLayout(3, 6, 6, 6));
	private final JButton submitButton = new JButton("Nộp bài");

	private final List<GameColor> currentTargets = new ArrayList<>();
	private final List<GameColor> currentGuess = new ArrayList<>();
	private int roundNumber = 1;
	private boolean gameEnded = false;

	private double totalScoreSelf = 0.0;
	private double totalScoreOpponent = 0.0;

	public GamePlayFrame(GameClient gameClient, MainFrame mainFrame) {
		super("Color Guessing Game - Play");
		this.gameClient = gameClient;
		this.mainFrame = mainFrame;
		this.gameClient.getMessageHandler().setGameEventListener(this);

		setSize(900, 640);
		setLayout(new BorderLayout(10, 10));

		// Header with GBL for đẹp mắt
		JPanel header = new JPanel(new GridBagLayout());
		header.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(2, 6, 2, 6);
		gbc.anchor = GridBagConstraints.WEST;

		gbc.gridx = 0; gbc.gridy = 0; header.add(new JLabel("Trạng thái:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; header.add(infoLabel, gbc);
		gbc.gridx = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; header.add(new JLabel("Điểm:"), gbc);
		gbc.gridx = 3; header.add(scoreLabelP1, gbc);
		gbc.gridx = 4; header.add(new JLabel("|"), gbc);
		gbc.gridx = 5; header.add(scoreLabelP2, gbc);

		gbc.gridx = 6; gbc.anchor = GridBagConstraints.EAST; header.add(leaveButton, gbc);
		gbc.gridx = 7; header.add(rematchButton, gbc);
		gbc.gridx = 8; header.add(backButton, gbc);
		add(header, BorderLayout.NORTH);

		// Panels
		colorPanel.setBorder(BorderFactory.createTitledBorder("Ghi nhớ 3 màu"));
		add(colorPanel, BorderLayout.CENTER);

		JPanel bottom = new JPanel(new BorderLayout());
		bottom.setBorder(BorderFactory.createTitledBorder("Chọn 3 màu đã thấy"));
		bottom.add(palettePanel, BorderLayout.CENTER);
		bottom.add(submitButton, BorderLayout.SOUTH);
		add(bottom, BorderLayout.SOUTH);

		submitButton.setEnabled(false);
		submitButton.addActionListener(e -> submitGuess());

		leaveButton.addActionListener(e -> {
			gameClient.leaveGame();
			dispose();
			if (mainFrame != null) mainFrame.returnFromGame();
		});
		rematchButton.addActionListener(e -> gameClient.requestRematch());
		backButton.addActionListener(e -> {
			if (gameEnded) {
				dispose();
				if (mainFrame != null) mainFrame.returnFromGame();
			} else {
				JOptionPane.showMessageDialog(this, "Bạn đang trong trận. Hãy dùng 'Rời trận'.");
			}
		});
	}

	private JPanel buildColorBox(Color c) {
		JPanel p = new JPanel();
		p.setBackground(c);
		p.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
		return p;
	}

	private void setupPalette(List<GameColor> palette) {
		palettePanel.removeAll();
		currentGuess.clear();
		for (GameColor gc : palette) {
			Color c = new Color(gc.getRed(), gc.getGreen(), gc.getBlue());
			JPanel box = buildColorBox(c);
			box.addMouseListener(new java.awt.event.MouseAdapter() {
				@Override public void mouseClicked(java.awt.event.MouseEvent e) {
					if (currentGuess.size() < 3) {
						currentGuess.add(gc);
						infoLabel.setText("Đã chọn " + currentGuess.size() + "/3");
						if (currentGuess.size() == 3) submitButton.setEnabled(true);
					}
				}
			});
			palettePanel.add(box);
		}
		palettePanel.revalidate();
		palettePanel.repaint();
	}

	private void submitGuess() {
		gameClient.submitGuess(new ArrayList<>(currentGuess), 0);
		submitButton.setEnabled(false);
	}

	private void updateScoreBar() {
		scoreLabelP1.setText("Bạn: " + String.format("%.1f", totalScoreSelf));
		scoreLabelP2.setText("Đối thủ: " + String.format("%.1f", totalScoreOpponent));
	}

	// GameEventListener
	@Override public void onGameStarted(Map<String, Object> gameData) { gameEnded = false; }
	@Override public void onGameEnded(String reason, Integer winnerId) {
		gameEnded = true;
		String msg = "Trận đấu kết thúc" + (winnerId != null ? (", winnerId=" + winnerId) : ", hòa");
		JOptionPane.showMessageDialog(this, msg + "\nBạn có thể yêu cầu 'Yêu cầu chơi lại' hoặc bấm 'Về màn chính'.");
	}
	@Override public void onShowColors(List<GameColor> colors, int duration, int roundNumber) {
		this.roundNumber = roundNumber;
		colorPanel.removeAll();
		for (GameColor gc : colors) {
			colorPanel.add(buildColorBox(new Color(gc.getRed(), gc.getGreen(), gc.getBlue())));
		}
		colorPanel.revalidate();
		colorPanel.repaint();
		infoLabel.setText("Vòng " + roundNumber + ": Ghi nhớ màu (" + (duration/1000) + "s)");
		submitButton.setEnabled(false);
	}
	@Override public void onStartGuessing(List<GameColor> colorPalette, int duration, int roundNumber) {
		setupPalette(colorPalette);
		infoLabel.setText("Chọn 3 màu đã thấy (" + (duration/1000) + "s)");
		submitButton.setEnabled(false);
	}
	@Override public void onRoundResult(Integer roundNumber, Boolean player1Correct, Boolean player2Correct, Integer winnerId, Double totalScore1, Double totalScore2) {
		// Server gửi tổng điểm hai phía sau mỗi round – hiển thị cố định
		if (totalScore1 != null) totalScoreSelf = totalScore1;
		if (totalScore2 != null) totalScoreOpponent = totalScore2;
		updateScoreBar();
		infoLabel.setText("Kết quả vòng " + roundNumber + ": tổng = " + String.format("%.1f", totalScoreSelf) + " - " + String.format("%.1f", totalScoreOpponent));
	}

	// Rematch
	@Override public void onRematchRequested(com.ncs.model.User fromUser, Integer timeoutMs) {
		javax.swing.SwingUtilities.invokeLater(() -> {
			int opt = JOptionPane.showConfirmDialog(this, (fromUser != null ? fromUser.getDisplayName() : "Đối thủ") + " mời chơi lại?", "Rematch", JOptionPane.OK_CANCEL_OPTION);
			if (opt == JOptionPane.OK_OPTION) {
				gameClient.acceptRematch();
			} else {
				gameClient.rejectRematch();
			}
		});
	}
	@Override public void onRematchResponse(String response, com.ncs.model.User responder) {
		javax.swing.SwingUtilities.invokeLater(() -> {
			String name = responder != null ? responder.getDisplayName() : "Đối thủ";
			JOptionPane.showMessageDialog(this, name + ("accept".equalsIgnoreCase(response) ? " đã đồng ý" : " đã từ chối") + " chơi lại.");
		});
	}
}

