package com.ncs.client;

import com.ncs.model.User;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class MainFrame extends JFrame implements MessageHandler.UserListEventListener, MessageHandler.InvitationEventListener, MessageHandler.GameEventListener {
	private final GameClient gameClient;
	private final User currentUser;

	private final JList<String> onlineList = new JList<>();
	private final DefaultTableModel leaderboardModel = new DefaultTableModel(new Object[]{"#", "Tên", "Điểm", "Tỷ lệ"}, 0);
	private final JTable leaderboardTable = new JTable(leaderboardModel);
	private final JTextField inviteMessageField = new JTextField(18);
	private final JButton inviteButton = new JButton("Gửi lời mời");
	private String selectedUsername;
	private GamePlayFrame currentGameFrame;

	public MainFrame(GameClient gameClient, User currentUser) {
		super("Color Guessing Game - Main");
		this.gameClient = gameClient;
		this.currentUser = currentUser;
		this.gameClient.getMessageHandler().setUserListEventListener(this);
		this.gameClient.getMessageHandler().setInvitationEventListener(this);
		// Lắng nghe sự kiện game để mở màn chơi khi server báo bắt đầu
		this.gameClient.getMessageHandler().setGameEventListener(this);

		setSize(900, 600);
		setLayout(new BorderLayout());

		// Top bar
		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
		top.add(new JLabel("Xin chào, " + (currentUser.getFullName() != null ? currentUser.getFullName() : currentUser.getUsername())));
		add(top, BorderLayout.NORTH);

		// Left: online users
		JPanel left = new JPanel(new BorderLayout());
		left.setBorder(BorderFactory.createTitledBorder("Người chơi online"));
		onlineList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		onlineList.addListSelectionListener(e -> {
			selectedUsername = onlineList.getSelectedValue();
			inviteButton.setEnabled(selectedUsername != null && !selectedUsername.equalsIgnoreCase(currentUser.getUsername()));
		});
		left.add(new JScrollPane(onlineList), BorderLayout.CENTER);

		// Context menu + double click để mời
		JPopupMenu popup = new JPopupMenu();
		JMenuItem inviteItem = new JMenuItem("Gửi lời mời");
		inviteItem.addActionListener(e -> doInvite());
		popup.add(inviteItem);
		onlineList.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				int idx = onlineList.locationToIndex(e.getPoint());
				if (idx >= 0) { onlineList.setSelectedIndex(idx); selectedUsername = onlineList.getSelectedValue(); }
				if (e.getClickCount() == 2 && selectedUsername != null && !selectedUsername.equalsIgnoreCase(currentUser.getUsername())) {
					doInvite();
				}
			}
			@Override public void mousePressed(MouseEvent e) { maybeShowPopup(e); }
			@Override public void mouseReleased(MouseEvent e) { maybeShowPopup(e); }
			private void maybeShowPopup(MouseEvent e) {
				if (e.isPopupTrigger()) {
					int idx = onlineList.locationToIndex(e.getPoint());
					if (idx >= 0) { onlineList.setSelectedIndex(idx); selectedUsername = onlineList.getSelectedValue(); }
					inviteItem.setEnabled(selectedUsername != null && !selectedUsername.equalsIgnoreCase(currentUser.getUsername()));
					popup.show(onlineList, e.getX(), e.getY());
				}
			}
		});

		JPanel invitePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		invitePanel.add(new JLabel("Lời nhắn:"));
		invitePanel.add(inviteMessageField);
		invitePanel.add(inviteButton);
		inviteButton.setEnabled(false);
		left.add(invitePanel, BorderLayout.SOUTH);

		// Right: leaderboard
		JPanel right = new JPanel(new BorderLayout());
		right.setBorder(BorderFactory.createTitledBorder("Bảng xếp hạng"));
		leaderboardTable.setFillsViewportHeight(true);
		right.add(new JScrollPane(leaderboardTable), BorderLayout.CENTER);

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
		split.setDividerLocation(350);
		add(split, BorderLayout.CENTER);

		// Actions
		inviteButton.addActionListener(e -> doInvite());

		// Load data
		SwingUtilities.invokeLater(() -> {
			gameClient.getOnlineUsers();
			gameClient.getLeaderboard(50);
		});
	}

	private void openGameIfNeeded() {
		if (currentGameFrame == null || !currentGameFrame.isDisplayable()) {
			currentGameFrame = new GamePlayFrame(gameClient, this);
			currentGameFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			currentGameFrame.setLocationRelativeTo(this);
			currentGameFrame.setVisible(true);
			this.setVisible(false);
		}
	}

	void returnFromGame() {
		this.setVisible(true);
		this.gameClient.getMessageHandler().setGameEventListener(this);
	}

	private void doInvite() {
		if (selectedUsername == null) return;
		gameClient.sendInvitation(selectedUsername, inviteMessageField.getText().trim());
		JOptionPane.showMessageDialog(this, "Đã gửi lời mời tới " + selectedUsername);
	}

	// UserListEventListener
	@Override public void onOnlineUsersReceived(List<User> users) {
		SwingUtilities.invokeLater(() -> {
			String[] names = users.stream().map(User::getUsername).toArray(String[]::new);
			onlineList.setListData(names);
		});
	}
	@Override public void onLeaderboardReceived(List<User> leaderboard) {
		SwingUtilities.invokeLater(() -> {
			leaderboardModel.setRowCount(0);
			int i = 1;
			for (User u : leaderboard) {
				leaderboardModel.addRow(new Object[]{i++, u.getUsername(), u.getRankingPoints(), String.format("%.0f%%", u.getWinRate())});
			}
		});
	}
	@Override public void onUserStatusChanged(User user, String action) {
		// refresh online list
		gameClient.getOnlineUsers();
	}

	// InvitationEventListener
	@Override public void onInvitationReceived(java.util.Map<String, Object> invitationData, User inviter) {
		SwingUtilities.invokeLater(() -> {
			int opt = JOptionPane.showConfirmDialog(this, inviter.getDisplayName() + " mời bạn thách đấu?", "Lời mời", JOptionPane.OK_CANCEL_OPTION);
			Object idObj = invitationData.get("id");
			if (opt == JOptionPane.OK_OPTION && idObj instanceof Number) {
				gameClient.acceptInvitation(((Number) idObj).intValue());
			}
		});
	}
	@Override public void onInvitationResponse(String response, User responder) { }
	@Override public void onInvitationSent(String message) { }
	@Override public void onInvitationAccepted() {
		SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Đối thủ đã chấp nhận. Đang chờ server bắt đầu trận..."));
	}
	@Override public void onInvitationRejected(String message) {
		SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, message == null ? "Lời mời đã bị từ chối" : message));
	}
	@Override public void onInvitationError(String message) {
		SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE));
	}

	// GameEventListener — mở màn chơi khi có sự kiện game đầu tiên
	@Override public void onGameStarted(java.util.Map<String, Object> gameData) { openGameIfNeeded(); }
	@Override public void onGameEnded(String reason, Integer winnerId) { }
	@Override public void onShowColors(java.util.List<com.ncs.model.GameColor> colors, int duration, int roundNumber) { openGameIfNeeded(); }
	@Override public void onStartGuessing(java.util.List<com.ncs.model.GameColor> colorPalette, int duration, int roundNumber) { openGameIfNeeded(); }
	@Override public void onRoundResult(Integer roundNumber, Boolean player1Correct, Boolean player2Correct, Integer winnerId, Double totalScore1, Double totalScore2) { }
}

