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
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GamePlayFrame extends JFrame implements MessageHandler.GameEventListener {
	private final GameClient gameClient;
	private final JPanel colorPanel = new JPanel(new GridLayout(1, 3, 8, 8));
	private final JPanel palettePanel = new JPanel(new GridLayout(3, 6, 6, 6));
	private final JLabel infoLabel = new JLabel("Chuẩn bị...");
	private final JButton submitButton = new JButton("Nộp bài");
	private final List<GameColor> currentTargets = new ArrayList<>();
	private final List<GameColor> currentGuess = new ArrayList<>();
	private int roundNumber = 1;
	private Timer phaseTimer;

	public GamePlayFrame(GameClient gameClient) {
		super("Color Guessing Game - Play");
		this.gameClient = gameClient;
		this.gameClient.getMessageHandler().setGameEventListener(this);
		setSize(800, 600);
		setLayout(new BorderLayout(8, 8));
		JPanel top = new JPanel(new BorderLayout());
		top.add(infoLabel, BorderLayout.WEST);
		add(top, BorderLayout.NORTH);
		colorPanel.setBorder(BorderFactory.createTitledBorder("Ghi nhớ 3 màu"));
		add(colorPanel, BorderLayout.CENTER);
		JPanel bottom = new JPanel(new BorderLayout());
		bottom.setBorder(BorderFactory.createTitledBorder("Chọn 3 màu đã thấy"));
		bottom.add(palettePanel, BorderLayout.CENTER);
		bottom.add(submitButton, BorderLayout.SOUTH);
		submitButton.setEnabled(false);
		submitButton.addActionListener(e -> submitGuess());
		add(bottom, BorderLayout.SOUTH);
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

	// GameEventListener
	@Override public void onGameStarted(Map<String, Object> gameData) { }
	@Override public void onGameEnded(String reason, Integer winnerId) {
		JOptionPane.showMessageDialog(this, "Trận đấu kết thúc" + (winnerId != null ? (", winnerId=" + winnerId) : ", hòa"));
	}
	@Override public void onShowColors(List<GameColor> colors, int duration, int roundNumber) {
		this.roundNumber = roundNumber;
		currentTargets.clear();
		currentTargets.addAll(colors);
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
		infoLabel.setText("Kết quả vòng " + roundNumber + ": tổng điểm = " + totalScore1 + " - " + totalScore2);
	}
}
