/*
 * 	Authors: 	Dylan Tobia, Abigail Dodd, Sydney Komro, Jewell Finder
 * 	File:		riskGUI.java
 * 	Purpose:	GUI for visual implementation of RISK
 */

package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import controller.TheGame;
import Model.*;
import songplayer.SoundClipPlayer;

//just a simple GUI to start, with a drawingPanel for map stuff
public class riskGUI extends JFrame {

	public static void main(String[] args) throws UnknownHostException,
			IOException {
		new riskGUI().setVisible(true);
	}

	private static BoardPanel drawingPanel;
	private static AnimationPanel animationPanel;
	private JMenuBar menu;
	private int width = java.awt.GraphicsEnvironment
			.getLocalGraphicsEnvironment().getMaximumWindowBounds().width;
	private int height = java.awt.GraphicsEnvironment
			.getLocalGraphicsEnvironment().getMaximumWindowBounds().height;
	private int xWidth = 0;
	private int yHeight = 0;
	private TheGame theGame;
	private ImageIcon gameBoard, stark, targaryen, lannister, whiteWalkers,
			dothraki, wildlings;
	private CountryPanel currCountryPanel;
	private boolean splash, gameOver = false;
	private ImageIcon splashScreen;
	private JPanel splashInfo = new JPanel();
	private Font gotFontHeader = new Font("Garamond", Font.BOLD, 11);
	private Font gotFontBody = new Font("Garamond", Font.BOLD, 11);
	private String gameType;
	private int humans;
	private int ai = -1;
	private ArrayList<String> houses;
	private ArrayList<String> playerNames;
	private ArrayList<String> possHouses;
	private ArrayList<Card> selectedCards;
	private ArrayList<AIStrategy> strat = new ArrayList<>();
	private boolean musicOn = true;
	private boolean animationsOn = true;
	private StatPanel currentStatsPanel;
	private Border blueline, raisedetched, loweredetched, raisedWithColor;
	private SoundClipPlayer player = new SoundClipPlayer();
	private boolean allAIS = false;
	private Faction attacker;
	private Faction defender;

	public riskGUI() {
		GraphicsEnvironment ge = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		try {
			gotFontHeader = Font.createFont(Font.TRUETYPE_FONT, this.getClass().getResourceAsStream("otherSources/TrajanusBricks.ttf"));//new File("otherSources/TrajanusBricks.ttf"));
					//"otherSources/TrajanusBricks.ttf"));
			gotFontHeader = gotFontHeader.deriveFont(36f);
			gotFontBody = Font.createFont(Font.TRUETYPE_FONT, this.getClass().getResourceAsStream("otherSources/LibreBaskerville-Regular.otf"));//new File("otherSources/LibreBaskerville-Regular.otf"));
			gotFontBody = gotFontBody.deriveFont(24f);
		} catch (FontFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		ge.registerFont(gotFontHeader);
		ge.registerFont(gotFontBody);

		// Making Borders
		// Creating border types to make the GUI prettier
		blueline = BorderFactory.createLineBorder(Color.BLUE);
		raisedetched = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		loweredetched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
		raisedWithColor = BorderFactory.createCompoundBorder(raisedetched,
				blueline);

		selectedCards = new ArrayList<Card>();
		attacker = Faction.STARK;
		defender = Faction.WILDLINGS;
		splash = true;
		setUpImages();
		setUpGui();
		setUpMenu();
		setUpHouseArray();
		setUpSplash();

	}// end riskGui constructor

	/*
	 * setupAnimationFrame
	 * 
	 * sets up the frame that will do the animations
	 */
	private void setUpAnimationFrame() {

		animationPanel = new AnimationPanel();
		animationPanel.setUpEverything(theGame);

		animationPanel.setSize(500, 500);
		animationPanel.setPreferredSize(new Dimension(500, 500));
		animationPanel.setLocation(100, 100);
	}

	/*
	 * setUpAIMenu sets up the menu for the AIs
	 */
	private void setUpAIMenu() {
		int i = 1;
		JMenu AIDiff = new JMenu("AI Difficulty");
		for (Player ai : theGame.getPlayers()) {
			if (ai instanceof AI) {
				((AI) ai).makeMenuItem(i, new AIDiffChangeListener());
				AIDiff.add(((AI) ai).getMenuItem());
				i++;
			}
		}
		menu.add(AIDiff);
	}// end setUpAiMenu

	/*
	 * loadGame Load a previously saved game
	 */
	public void loadGame() {
		boolean error = setUpLoad();
		if (error) {
			JOptionPane
					.showMessageDialog(
							null,
							"No game data has been succesfully loaded. Start a new game.",
							"Error", JOptionPane.ERROR_MESSAGE);
			splashNumPlayers();
		} else {
			splash = false;
			drawingPanel.removeAll();
			this.remove(drawingPanel);
			setUpDrawingPanel();
			setUpMenu();
			setUpAIMenu();
			setUpClearButton();
			setUpPassButton();
			drawingPanel.revalidate();
			drawingPanel.repaint();
		}

	}

	/*
	 * setUoLoad Sets up the apparatus for loading a game
	 */
	private boolean setUpLoad() {
		boolean error = false;
		JFileChooser choose = new JFileChooser();
		choose.setCurrentDirectory(new File("./SavedGames"));
		int get = choose.showOpenDialog(null);
		if (get == JFileChooser.APPROVE_OPTION) {
			try {
				ObjectInputStream inFile = new ObjectInputStream(
						new FileInputStream(choose.getSelectedFile()));
				theGame = (TheGame) inFile.readObject();
				inFile.close();
			} catch (Exception ex) {
				ex.printStackTrace();
				error = true;
			}
		} else {
			error = true;
		}
		return error;

	}

	/*
	 * turnOnStatPanel turns on the stats panel by removing the map pane and
	 * replacing it with a new stats panel
	 */
	private void turnOnStatPanel() {
		currentStatsPanel = new StatPanel();
		this.remove(drawingPanel);
		this.add(currentStatsPanel);
		this.revalidate();
		this.repaint();
	}// end turnOnStatPanel

	/*
	 * turnOffStatPanel turns off the stat panel by removing it and replacing
	 * the map panel
	 */
	private void turnOffStatPanel() {
		this.remove(currentStatsPanel);
		this.add(drawingPanel);
		this.revalidate();
		this.repaint();
	}// end turnOffStatPanel

	/*
	 * setUpHouseArray All of the possible houses are added to an arraylist
	 */
	private void setUpHouseArray() {
		possHouses = new ArrayList<String>();
		possHouses.add("Stark");
		possHouses.add("Lannister");
		possHouses.add("Targaryen");
		possHouses.add("White Walkers");
		possHouses.add("Dothraki");
		possHouses.add("Wildlings");

	}// end setUpHouseArray

	/*
	 * setUpSplash builds the first splash loading screen
	 */
	private void setUpSplash() {
		splashScreen = new ImageIcon("./images/SplashScreen.jpg");
		drawingPanel = new BoardPanel();
		drawingPanel.setLayout(null);
		drawingPanel.setSize(width - 40, height - 70);
		drawingPanel.setLocation(10, 10);
		drawingPanel.setBackground(Color.LIGHT_GRAY);
		this.add(drawingPanel);
		this.setVisible(true);
		drawingPanel.repaint();
		player.startTheme();
		splashLoading1();
	}// end setUpSplash

	/*
	 * setUpSplash builds the second splash loading screen
	 */
	private void splashLoading2() {

		splash = false;
		drawingPanel.removeAll();
		this.remove(drawingPanel);
		// creates or grabs an instance of the game, first variable is number of
		// human players, second is total number of players
		theGame = TheGame.getInstance(humans, ai, false);
		setUpDrawingPanel();
		setUpMenu();
		setUpClearButton();
		setUpPassButton();
		setUpAIMenu();
		setUpAnimationFrame();
		ArrayList<Player> players = theGame.getPlayers();
		for (int i = 0; i < humans; i++) {
			players.get(i).setFaction(houses.get(i));
			players.get(i).setName(playerNames.get(i));
		}
		for (String h : houses) {
			possHouses.remove(h);
		}
		int i = 0;
		for (int j = humans; j < (humans + ai); j++) {
			players.get(j).setFaction(possHouses.get(i));
			((AI) players.get(j)).setStrategy(strat.get(i));
			i++;
		}

		theGame.setPlayers(players);
		theGame.startGame();

	}// end splashLoading2

	/*
	 * splashNames Collects the names of the players
	 */
	private void splashNames() {
		boolean cancel = false;
		playerNames = new ArrayList<String>();
		for (int i = 0; i < humans; i++) {
			boolean nameFlag = false;

			while (!nameFlag) {
				String name = JOptionPane
						.showInputDialog("What will be Player " + (i + 1)
								+ "'s Name?");
				if (name == null) {
					cancel = true;
					nameFlag = true;
				} else {

					playerNames.add(name);
					nameFlag = true;
				}
			}
			if (cancel)
				break;
		}
		if (cancel)
			splashChooseGame();
		else
			splashLoading2();
	}// end splash names

	/*
	 * splashHouses collects the houses of the players and sets the others
	 */
	private void splashHouses() {
		drawingPanel.remove(splashInfo);
		boolean cancel = false;
		houses = new ArrayList<String>();
		for (int i = 0; i < humans; i++) {
			Boolean illegalName = true;
			String house = "";
			while (illegalName == true && cancel == false) {
				Boolean check = true;
				house = (String) JOptionPane.showInputDialog(null,
						"Please choose Player " + (i + 1) + "'s House",
						"Choose a House", JOptionPane.QUESTION_MESSAGE, null,
						new Object[] { "Stark", "Targaryen", "Lannister",
								"White Walkers", "Wildlings", "Dothraki" },
						"No");
				if (house != null) {
					for (int j = 0; j < houses.size(); j++) {
						if (houses.get(j).compareTo(house) == 0) {
							check = false;
							JOptionPane
									.showMessageDialog(
											null,
											"House has already been chosen. Please pick another.",
											"Error", JOptionPane.ERROR_MESSAGE);
						}
					}
				}

				if (house == null && check) {
					cancel = true;
					check = false;
				} else if (house != null && check)
					illegalName = false;

			}
			if (cancel) {
				break;
			} else
				houses.add(house);
		}
		if (!cancel) {
			for (int i = 0; i < ai; i++) {
				Boolean illegalName = true;
				String ais = "";

				while (illegalName == true) {

					ais = (String) JOptionPane.showInputDialog(null,
							"Please choose AI " + (i + 1) + "'s Strategy",
							"Choose a Strategy", JOptionPane.QUESTION_MESSAGE,
							null, new Object[] { "Easy", "Medium", "Hard" },
							"Easy");

					if (ais == null) {
						cancel = true;
						illegalName = false;
						break;
					} else {
						illegalName = false;
						switch (ais) {
						case "Easy":
							strat.add(new EasyAI());
							break;
						case "Hard":
							strat.add(new HardAI());
							break;
						case "Medium":
							strat.add(new MediumAI());
							break;
						default:
							break;
						}
					}
				}
				if (cancel)
					break;
			}
		}
		if (cancel) {
			splashChooseGame();
		} else
			splashNames();
	}// end splashHouses

	/*
	 * splashNumPlayers Gets the number of human and AIs
	 */
	private void splashNumPlayers() {

		String human = "", ais = "";
		boolean continueFlag = false, setFlag = false, aiFlag = false, cancel = false;

		while (!continueFlag) {
			human = JOptionPane.showInputDialog("How Many Human Players?");
			if (human == null) {
				continueFlag = true;
				cancel = true;
			} else {
				try {
					humans = Integer.parseInt(human);
					continueFlag = true;

				} catch (NumberFormatException e) {
					JOptionPane.showMessageDialog(null,
							"Must choose a valid number between 0 and 6.",
							"Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		if (cancel) {
			splashChooseGame();
		} else {
			while (!setFlag) {
				ais = JOptionPane.showInputDialog("How Many AI Players?");
				if (ais == null) {
					setFlag = true;
					cancel = true;
				} else {
					try {
						ai = Integer.parseInt(ais);
						aiFlag = true;

					} catch (NumberFormatException e) {
						JOptionPane.showMessageDialog(null,
								"Must choose a valid number between 0 and 6.",
								"Error", JOptionPane.ERROR_MESSAGE);
					}
					if (aiFlag) {
						if ((ai + humans) > 6 || ai + humans < 3) {
							JOptionPane
									.showMessageDialog(
											null,
											"Invalid. Total number of players must be between 3 and 6.",
											"Error", JOptionPane.ERROR_MESSAGE);
						} else
							setFlag = true;

					}
				}
			}
		}
		if (cancel) {
			splashChooseGame();
		} else
			splashHouses();
	}// end splashNumPlayers

	/*
	 * splashChooseGame lets user choose between new or saved game
	 */
	private void splashChooseGame() {
		drawingPanel.remove(splashInfo);
		splashInfo = new JPanel();
		splashInfo.setLayout(null);
		splashInfo.setSize(700, 400);
		splashInfo.setLocation(width / 2 - 350, height / 2 - 200);
		JLabel load = new JLabel("New Game or Load Game?");
		load.setFont(gotFontHeader.deriveFont(28f));
		load.setLocation(100, 5);
		load.setSize(600, 150);
		JButton newG = new JButton("New Game!");
		newG.setFont(gotFontHeader.deriveFont(24f));
		newG.setLocation(50, 200);
		newG.addActionListener(new GameTypeListener());
		newG.setSize(300, 100);
		JButton loadG = new JButton("Load Game!");
		loadG.setFont(gotFontHeader.deriveFont(24f));
		loadG.setLocation(375, 200);
		loadG.addActionListener(new GameTypeListener());
		loadG.setSize(300, 100);
		splashInfo.add(newG);
		splashInfo.add(loadG);
		splashInfo.add(load);
		drawingPanel.add(splashInfo);
		drawingPanel.repaint();
	}// end splashChooseGame

	/*
	 * SplashLoading1 is the first loading page. Sets up the background image
	 * and JPanel for information. Starts Theme Song, which will play until it
	 * ends. This screen is shown for 10 seconds.
	 */
	private void splashLoading1() {

		splashInfo = new JPanel();
		splashInfo.setLayout(null);
		splashInfo.setSize(500, 150);
		splashInfo.setLocation(width / 2 - 250, height / 2 - 75);
		JLabel load = new JLabel("LOADING...");
		load.setFont(gotFontHeader);
		load.setLocation(150, 5);
		load.setSize(300, 150);
		splashInfo.add(load);
		drawingPanel.add(splashInfo);
		drawingPanel.repaint();

		// pause on this screen for 10 seconds. Set to 5 seconds during testing.
		try {
			Thread.sleep(5000);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		// move on to splash screen #2, choosing game play
		splashChooseGame();
	}// end splashLoading1

	/*
	 * setUpGUI does all necessary information to build the GUI
	 */
	private void setUpGui() {
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		setLayout(new BorderLayout());
		setTitle("GoT Risk");
		setSize(width, height);
		this.setVisible(true);

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				int confirm = JOptionPane.showConfirmDialog(null, "Save Data?",
						"End", JOptionPane.YES_NO_CANCEL_OPTION);
				// If the user wants to save before quit, then save!
				if (confirm == JOptionPane.CANCEL_OPTION) {
				} else if (confirm == JOptionPane.OK_OPTION) {
					JFileChooser choose = new JFileChooser();
					choose.setCurrentDirectory(new File("./SavedGames"));
					int get = choose.showSaveDialog(null);
					if (get == JFileChooser.APPROVE_OPTION) {
						try {
							FileOutputStream gameToDisk = new FileOutputStream(
									choose.getSelectedFile() + ".ser");
							ObjectOutputStream outFile = new ObjectOutputStream(
									gameToDisk);
							outFile.writeObject(theGame);
							outFile.close();
						} catch (Exception ex) {
							ex.printStackTrace();
						}
						System.exit(0);
					}
				} else if (confirm == JOptionPane.NO_OPTION) {
					System.exit(0);
				}
			}
		});

	}// end setUpGui

	/*
	 * setUpMenu builds the menu
	 */
	private void setUpMenu() {
		JMenu file = new JMenu("File");
		JMenuItem newGame = new JMenuItem("New Game");
		newGame.addActionListener(new NewGameListener());
		file.add(newGame);
		JMenuItem saveGame = new JMenuItem("Save Game");
		saveGame.addActionListener(new saveGameListener());
		file.add(saveGame);
		JMenu settings = new JMenu("Settings");
		String music = "";
		if (musicOn)
			music = "Mute Music";
		else
			music = "Unmute Music";
		JMenuItem musicStatus = new JMenuItem(music);
		musicStatus.addActionListener(new musicListener());
		settings.add(musicStatus);
		String animations = "";
		if (animationsOn)
			animations = "Turn Off Animations";
		else
			animations = "Turn On Animations";
		JMenuItem animationStatus = new JMenuItem(animations);
		animationStatus.addActionListener(new animationListener());
		settings.add(animationStatus);

		JMenu maxDice = new JMenu("Defualt Dice");
		JMenu attackDice = new JMenu("Attack Dice");
		JMenu defendDice = new JMenu("Defend Dice");
		JMenuItem setMaxAttack = new JMenuItem("Max Attack Dice");
		setMaxAttack.addActionListener(new HelpListener());
		setMaxAttack.setActionCommand("attack max");
		maxDice.add(attackDice);
		maxDice.add(defendDice);

		JMenuItem setAttackTwo = new JMenuItem("2 Dice");
		setAttackTwo.addActionListener(new HelpListener());
		setAttackTwo.setActionCommand("attack 2");

		JMenuItem setAttackOne = new JMenuItem("1 Die");
		setAttackOne.addActionListener(new HelpListener());
		setAttackOne.setActionCommand("attack 1");
		JMenuItem setMaxDefend = new JMenuItem("Max Defend Dice");
		setMaxDefend.addActionListener(new HelpListener());
		setMaxDefend.setActionCommand("defend max");

		JMenuItem setDefendOne = new JMenuItem("1 Die");
		setDefendOne.addActionListener(new HelpListener());
		setDefendOne.setActionCommand("defend min");

		attackDice.add(setMaxAttack);
		attackDice.add(setAttackTwo);
		attackDice.add(setAttackOne);
		defendDice.add(setMaxDefend);
		defendDice.add(setDefendOne);

		settings.add(maxDice);
		JMenu help = new JMenu("Help");
		menu = new JMenuBar();
		menu.add(file);
		JMenuItem about = new JMenuItem("About");
		menu.add(help);
		if (!splash)
			menu.add(settings);

		JMenuItem howToPlay = new JMenuItem("How To Use this GUI");
		howToPlay.addActionListener(new HelpListener());
		howToPlay.setActionCommand("how to play");
		JMenuItem rules = new JMenuItem("Rules");
		help.add(rules);
		help.add(howToPlay);
		help.add(about);

		this.setJMenuBar(menu);

		rules.setActionCommand("rules");
		about.setActionCommand("about");
		rules.addActionListener(new HelpListener());
		about.addActionListener(new HelpListener());

	}// end setUpMenu

	/*
	 * setUpClearButton Creats the clear moves button
	 */
	private void setUpClearButton() {

		JButton clearButton = new JButton("Clear Move Selections");
		clearButton.addActionListener(new clearButtonListener());
		clearButton.setSize(4 * xWidth, 2 * yHeight);
		clearButton.setLocation(width - (int) (4.25 * xWidth),
				(int) (0.25 * yHeight));
		drawingPanel.add(clearButton);
	}

	/*
	 * setUpPassButton Creates the button that lets you skip phases
	 */
	private void setUpPassButton() {
		JButton passButton = new JButton("Skip to the Next Phase");
		passButton.addActionListener(new PassButtonListener());
		passButton.setSize(4 * xWidth, 2 * yHeight);
		passButton.setLocation(width - (int) (4.25 * xWidth),
				(int) (2.75 * yHeight));
		drawingPanel.add(passButton);
	}

	/*
	 * setUpStatButton Creats button that lets you select the stats pane
	 */
	private void setUpStatButton() {
		JButton statButton = new JButton("Check out our stats");
		statButton.addActionListener(new StatPanelTurnOnListener());
		statButton.setSize(4 * xWidth, 2 * yHeight);
		statButton.setLocation(width - (int) (4.25 * xWidth),
				(int) (5.25 * yHeight));
		drawingPanel.add(statButton);
	}

	/*
	 * animations Starts the animations! Takes the factions of the two players
	 * at war
	 */
	private void animations(Faction attacker, Faction defender) {
		setUpAnimationFrame();

		animationPanel.resetStart();
		drawingPanel.add(animationPanel);
		animationPanel.setLocation(width / 2 - 350, height / 2 - 350);

		animationPanel.setDefenseFaction(defender);
		animationPanel.setOffenseFaction(attacker);

		animationPanel.update(getGraphics());
		int i = 0;
		while (i < 65) {
			animationPanel.updateAnimations();
			try {
				Thread.sleep(50);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}

			animationPanel.update(getGraphics());
			i++;
		}

		drawingPanel.remove(animationPanel);
		this.repaint();
		drawingPanel.repaint();
	}

	/*
	 * setUpDrawingPanel Creates the panel where the map and all of the gameplay
	 * goes!
	 */
	private void setUpDrawingPanel() {
		drawingPanel = new BoardPanel();
		drawingPanel.setLayout(null);
		drawingPanel.setSize(width - 40, height - 70);
		drawingPanel.setLocation(10, 10);
		drawingPanel.setBackground(Color.BLACK);
		drawingPanel.repaint();

		// Prepare to draw the buttons!
		Dimension drawD = drawingPanel.getSize();
		xWidth = (int) (drawD.getWidth() / 40);
		yHeight = (int) (drawD.getHeight() / 40);
		drawCountryButtons();

		// Draw country panel
		currCountryPanel = new CountryPanel();
		if (theGame.getNumHumans() != 0 || theGame.isFinished())
			drawingPanel.add(currCountryPanel);
		this.add(drawingPanel, BorderLayout.CENTER);
		drawingPanel.repaint();
		this.repaint();

		if (!allAIS) {
			player.stopTheme();
			player.startPlay();
		}
		if (theGame.getNumHumans() != 0 || theGame.isFinished())
			setUpStatButton();
	}// end setUpDrawingPanel

	/*
	 * setUpImages Builds the images of the factions
	 */
	private void setUpImages() {

		gameBoard = new ImageIcon("./images/GoTMapRisk.jpg");
		stark = new ImageIcon("./images/stark.jpg");
		targaryen = new ImageIcon("./images/targaryen.jpg");
		lannister = new ImageIcon("./images/lannister.jpg");
		whiteWalkers = new ImageIcon("./images/whiteWalkers.jpg");
		dothraki = new ImageIcon("./images/dothraki.jpg");
		wildlings = new ImageIcon("./images/wildlings.jpg");
	}// end setUpImages

	/*
	 * drawCountryButtons draws buttons over the name of all of the countries
	 */
	private void drawCountryButtons() {
		for (Country country : theGame.getGameMap().getCountries()) {
			// The Make button method has the same logic that was previously
			// here
			country.makeButton(xWidth, yHeight, new CountryClickListener());
			drawingPanel.add(country.getButton());
		} // end for

	}// end drawCountryButtons

	/*
	 * updateCountryButton Updates those buttons if the size of the panel
	 * changes
	 */
	private void updateCountryButtons() {
		for (Country country : theGame.getGameMap().getCountries()) {
			country.updateButton(xWidth, yHeight);
		}
	}// end updateCountryButtons

	/*********************************
	 * Other JPanels Below
	 ********************************/
	/*
	 * BoardPanel The panel that displays country info, lets you trade, etc.
	 */
	private class BoardPanel extends JPanel implements Observer {

		@Override
		public void paintComponent(Graphics g) {
			if (theGame != null)
				gameOver = theGame.isGameOver();
			Graphics2D g2 = (Graphics2D) g;
			g2.setColor(Color.black);
			super.paintComponent(g2);

			Image tmp;
			if (splash)
				tmp = splashScreen.getImage();
			else
				tmp = gameBoard.getImage();
			g2.drawImage(tmp, 0, 0, drawingPanel.getWidth(),
					drawingPanel.getHeight(), null);

			Dimension drawD = drawingPanel.getSize();
			xWidth = (int) (drawD.getWidth() / 40);
			yHeight = (int) (drawD.getHeight() / 40);

			drawFactions(g2);

			if (!gameOver) {
				if (!splash && theGame.getNumHumans() != 0) {
					updateCountryButtons();
					currCountryPanel.updatePanel(g);
				}

				if (theGame != null) {
					drawCurrentPlayer(g2);
					drawUnits(g2);
					gameOver = theGame.isGameOver();
				}
				// drawGridAndNumbers(g2);
			} else {
				drawUnits(g2);
				drawingPanel.add(currCountryPanel);
				currCountryPanel.updatePanel(g);
				for (Country country : theGame.getGameMap().getCountries()) {
					country.getButton().setEnabled(false);
				}

			}
			if (theGame != null) {
				setUpMenu();
				setUpAIMenu();
			}
		}// end paintComponenet

		/*
		 * drawUnits draws the units that each country has place on it
		 */
		private void drawUnits(Graphics2D g2) {

			g2.setColor(Color.BLACK);
			g2.setFont(gotFontBody.deriveFont(Font.BOLD, 35f));
			for (Country country : theGame.getGameMap().getCountries()) {
				if (country.getForcesVal() > 0) {
					g2.drawString("" + country.getForcesVal(),
							((int) country.getX() * xWidth) + 22,
							((int) country.getY() * yHeight) + 17);
				}

			}

		}// end drawUnits

		/*
		 * draws information about the current player, including his house,
		 * image, etc.
		 */
		private void drawCurrentPlayer(Graphics2D g2) {
			Player currentPlayer = theGame.getCurrentPlayer();
			if (currentPlayer != null) {
				Faction playersFact = theGame.getCurrentPlayer().getFaction();
				switch (playersFact) {
				case STARK:
					g2.drawImage(stark.getImage(), 0, 0, 100, 100, null);
					break;
				case TARGARYEN:
					g2.drawImage(targaryen.getImage(), 0, 0, 100, 100, null);
					break;
				case LANNISTER:
					g2.drawImage(lannister.getImage(), 0, 0, 100, 100, null);
					break;
				case DOTHRAKI:
					g2.drawImage(dothraki.getImage(), 0, 0, 100, 100, null);
					break;
				case WHITEWALKERS:
					g2.drawImage(whiteWalkers.getImage(), 0, 0, 100, 100, null);
					break;
				case WILDLINGS:
					g2.drawImage(wildlings.getImage(), 0, 0, 100, 100, null);
					break;
				default:
					break;
				}
			}
			g2.setColor(Color.WHITE);
			g2.setFont(gotFontBody.deriveFont(20f));
			g2.drawString("Current Player: " + currentPlayer.getName(), 110, 25);
			g2.drawString("Current Phase: " + theGame.getPhase(), 110, 45);
			if (theGame.isPlayPhase() && theGame.isReinforcePhase()) {
			} else if (!theGame.isPlacePhase() && theGame.isReinforcePhase())
				g2.drawString("You have: "
						+ theGame.getCurrentPlayer().getAvailableTroops()
						+ " units left to place.", 110, 65);

			else if (theGame.isDeployPhase())
				g2.drawString("You have: "
						+ theGame.getCurrentPlayer().getAvailableTroops()
						+ " units to place.", 110, 65);

		}// end drawCurrentPlayer

		/*
		 * drawFactions draws factions if a country is occupied
		 */
		private void drawFactions(Graphics2D g2) {
			Map temp = Map.getInstance(0);
			Country[] allCountries = temp.getCountries();
			for (Country country : allCountries) {
				if (country.getOccupier() != null) {
					Faction ownerFaction = country.returnMyOwnersFaction();
					switch (ownerFaction) {
					case STARK:
						g2.drawImage(stark.getImage(),
								((int) country.getX() * xWidth) + 50,
								((int) country.getY() * yHeight) + 5, 30, 30,
								null);
						break;
					case TARGARYEN:
						g2.drawImage(targaryen.getImage(),
								((int) country.getX() * xWidth) + 50,
								((int) country.getY() * yHeight) + 5, 30, 30,
								null);
						break;
					case LANNISTER:
						g2.drawImage(lannister.getImage(),
								((int) country.getX() * xWidth) + 50,
								((int) country.getY() * yHeight) + 5, 30, 30,
								null);
						break;
					case DOTHRAKI:
						g2.drawImage(dothraki.getImage(),
								((int) country.getX() * xWidth) + 50,
								((int) country.getY() * yHeight) + 5, 30, 30,
								null);
						break;
					case WHITEWALKERS:
						g2.drawImage(whiteWalkers.getImage(),
								((int) country.getX() * xWidth) + 50,
								((int) country.getY() * yHeight) + 5, 30, 30,
								null);
						break;
					case WILDLINGS:
						g2.drawImage(wildlings.getImage(),
								((int) country.getX() * xWidth) + 50,
								((int) country.getY() * yHeight) + 5, 30, 30,
								null);

					}

				}
			}

		}// end drawFaction

		/*
		 * drawGridAndNumbers draws a 40X40 grid over the risk map. Used for
		 * determining where to place buttons. Used for measuring
		 */
		private void drawGridAndNumbers(Graphics2D g2) {
			for (int i = xWidth; i < width - 40; i += xWidth) {
				g2.drawLine(i, 0, i, height - 70);
			}

			for (int i = yHeight; i < height - 70; i += yHeight) {
				g2.drawLine(0, i, width - 40, i);
			}

			int xCount = xWidth / 2;
			int yCount = yHeight / 2;

			int startX = xCount;
			int startY = yCount;
			int y = 0;
			// int x = 0;

			for (int i = 1; i < 40; i++) {
				int x = 1;
				y++;
				startY = yCount;

				for (int j = 1; j < 40; j++) {
					g2.drawString(Integer.toString(x), startX, startY);
					startY += yHeight;

					x++;
				}
				startX += xWidth;
			}
		}// end drawGridAndNumbers

		/*
		 * update for drawing factions over occupied functions(non-Javadoc)
		 * 
		 * @see java.util.Observer#update(java.util.Observable,
		 * java.lang.Object)
		 */
		@Override
		public void update(Observable arg0, Object arg1) {

			if (theGame.isFinished() && theGame.getNumHumans() == 0) {
				Thread.currentThread().interrupt();
				// drawingPanel.removeAll();
				// this.remove(drawingPanel);
				setUpDrawingPanel();
				setUpMenu();
				this.revalidate();
				this.repaint();
				this.update(getGraphics());
			} else if (theGame.getNumHumans() == 0 && !theGame.isFinished()) {
				allAIS = true;
				try {
					Thread.sleep(1);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				drawingPanel.removeAll();
				this.remove(drawingPanel);
				this.revalidate();
				this.repaint();
				setUpDrawingPanel();
				setUpMenu();
				setUpAIMenu();
				this.revalidate();
				this.repaint();
				drawingPanel.update(drawingPanel.getGraphics());
				updateGM();
			}
			this.revalidate();
			this.repaint();

		}// end update

		/*
		 * updateGm revalidates and repaints (because null layouts are silly)
		 */
		private void updateGM() {
			if (theGame.isFinished()) {
				this.revalidate();
				this.repaint();
			}
		}

	}// end boardPanel

	public static BoardPanel getBoardPanel() {
		return drawingPanel;
	}// end getBoardPanel

	/*
	 * statPanel
	 * 		draws the statistics panel
	 */
	private class StatPanel extends JPanel {
		private Player currPlayer;
		private CurrentPlayerStatsPanel currPanel;
		private AllPlayerStatsPanel allPanel;
		private ArrayList<Player> allPlayers;

		public StatPanel() {
			super();
			currPanel = new CurrentPlayerStatsPanel();
			allPanel = new AllPlayerStatsPanel();
			// It's a border layout
			this.setLayout(new BorderLayout());

			JLabel phase = new JLabel(theGame.getPhase());
			phase.setHorizontalAlignment(SwingConstants.CENTER);
			phase.setBorder(raisedWithColor);
			phase.setFont(gotFontHeader);
			this.add(phase, BorderLayout.NORTH);

			JPanel holder = new JPanel();
			holder.setLayout(new GridLayout(0, 2));
			holder.add(currPanel);
			holder.add(allPanel);
			this.add(holder, BorderLayout.CENTER);

			// The go back button
			JButton returnButton = new JButton("Return to your game");
			returnButton.addActionListener(new StatPanelTurnOffListener());
			this.add(returnButton, BorderLayout.SOUTH);
			this.revalidate();
			this.repaint();
		}// end constructor

		/*
		 * Left half of the gui
		 */

		/*
		 * Anytime you make this view, a new currentplayerstats panel needs to
		 * be made to reflect the current player
		 */
		@SuppressWarnings("serial")
		private class CurrentPlayerStatsPanel extends JPanel {

			private CurrentPlayerStatsPanel() {
				super();
				currPlayer = theGame.getCurrentPlayer();
				this.setLayout(new GridLayout(2, 0));
				this.add(addNameAndCardsPanel());
				this.add(addPlayerCountriesPanel());
				this.revalidate();
				this.repaint();
			}// end constructor
		}// end currentplayerstats

		private JPanel addNameAndCardsPanel() {
			JPanel myStuff = new JPanel();
			myStuff.setLayout(new GridLayout(2, 0));

			// Drawing the name and factionr
			JPanel name = new JPanel();
			name.setLayout(new GridLayout(2, 0));
			name.setBorder(raisedWithColor);
			JLabel nameLabel = new JLabel(currPlayer.getName());
			nameLabel.setFont(gotFontHeader);
			JLabel factLabel = new JLabel(currPlayer.getFaction().getName());
			factLabel.setFont(gotFontHeader);
			name.add(nameLabel);
			name.add(factLabel);
			myStuff.add(name);

			// Drawing the cards
			JPanel cards = new JPanel();
			cards.setLayout(new GridLayout(0, 5));
			cards.setBorder(raisedWithColor);
			ArrayList<Card> currCards = currPlayer.getCards();

			for (int i = 0; i < currCards.size(); i++) {
				ImageIcon im = currCards.get(i).getMyImageIcon();
				CardPanel card = new CardPanel(im.getImage(), xWidth, yHeight);
				cards.add(card);
			}
			myStuff.add(cards);
			myStuff.revalidate();
			return myStuff;
		}// end nameAndCard

		private JScrollPane addPlayerCountriesPanel() {
			// build one panel for all countries
			ArrayList<Country> currCountries = currPlayer.getCountries();

			int numCols = 2;
			int numRows = currCountries.size() / numCols;
			// If there are leftovers, add one more column
			if (currCountries.size() % numCols != 0)
				numRows++;

			JPanel countPanel = new JPanel();
			countPanel.setLayout(new GridLayout(numRows, numCols));
			countPanel.setPreferredSize(new Dimension(width / 2, height / 2));

			// Make labels for all countries and add them
			for (int i = 0; i < currCountries.size(); i++) {
				JLabel countryLabel = new JLabel(currCountries.get(i).getName());
				countryLabel.setFont(gotFontBody);
				countPanel.add(countryLabel);
			}
			countPanel.revalidate();
			JScrollPane scroll = new JScrollPane(countPanel);
			scroll.setBorder(raisedWithColor);
			return scroll;
		}// end addPlayerCountriesPanel

		/*
		 * Begin Right Half of GUI
		 */
		private class AllPlayerStatsPanel extends JPanel {
			private AllPlayerStatsPanel() {
				super();
				allPlayers = theGame.getPlayers();
				this.setLayout(new GridLayout(2, 1));
				this.add(allPlayersList());
				this.add(contsAndLogPanel());
				this.revalidate();
				this.repaint();
			}// end constructor

			private JPanel allPlayersList() {
				JPanel playList = new JPanel();
				playList.setLayout(new GridLayout(allPlayers.size(), 1));

				for (int i = 0; i < allPlayers.size(); i++) {
					JLabel aPlayer = new JLabel();
					aPlayer.setFont(gotFontBody);

					// set up the name
					String player = allPlayers.get(i).getName();
					if (allPlayers.get(i) instanceof AI)
						player += " "
								+ ((AI) allPlayers.get(i)).getStrategy()
										.toString();

					player += " " + allPlayers.get(i).getFaction().getName();
					player += ": " + allPlayers.get(i).getCountries().size()
							+ " countries.";
					aPlayer.setText(player);

					// add it
					playList.add(aPlayer);
				} // end for
				playList.setBorder(raisedWithColor);
				playList.revalidate();
				playList.repaint();
				return playList;
			}// end allPlayersList

			/*
			 * draws the continents and log
			 */
			private JPanel contsAndLogPanel() {
				JPanel lastPanel = new JPanel();
				lastPanel.setLayout(new GridLayout(1, 2));

				JPanel contPanel = new JPanel();
				contPanel.setBorder(raisedWithColor);
				contPanel.setLayout(new GridLayout(7, 1));
				String[] allContinents = theGame.getMap()
						.getContinentOwnersAsStrings();

				for (int i = 0; i < allContinents.length; i++) {
					JLabel lab = new JLabel(allContinents[i]);
					lab.setFont(gotFontBody);
					contPanel.add(lab);
				}
				lastPanel.add(contPanel);

				JTextArea log = new JTextArea();
				log.setEditable(false);
				log.setText(theGame.getGameLog());
				JScrollPane scroll = new JScrollPane(log);
				lastPanel.add(scroll);

				return lastPanel;
			}

		}// end allplayerstats
	}

	/*
	 * Displays the country information and directions otherwise
	 */
	private class CountryPanel extends JPanel {

		private Country curr;

		//constructor
		public CountryPanel() {
			this.setLayout(new BorderLayout());
			this.setBorder(loweredetched);
			curr = theGame.getSelectedCountry();
			updatePanel();

		}// end constructor

		// Displays country's name and owner
		public void makeTopLabel() {
			JPanel top = new JPanel();
			top.setLayout(new BorderLayout());
			JLabel country = new JLabel(curr.getName());
			country.setFont(gotFontHeader);
			country.setHorizontalAlignment(JLabel.CENTER);
			top.add(country, BorderLayout.CENTER);
			JLabel owner = new JLabel();
			owner.setFont(gotFontHeader.deriveFont(28f));
			owner.setHorizontalAlignment(JLabel.CENTER);
			if (curr.getOccupier() != null)
				owner.setText(curr.getOccupier().getName() + " "
						+ curr.getOccupier().getFaction().getName());
			else
				owner.setText("None");
			top.add(owner, BorderLayout.SOUTH);

			// Eventually will be deleted!
			top.setBorder(raisedWithColor);

			this.add(top, BorderLayout.NORTH);
			top.revalidate();
		}// end makeTopLabel

		// will display a list of neighbors and the strength of the country's
		// armies
		public void makePlayingCenterPanel() {
			JPanel center = new JPanel();
			center.setLayout(new GridLayout(0, 2));
			JPanel neighbors = new JPanel();
			// Make neighbors panel
			ArrayList<Country> neighs = curr.getNeighbors();
			neighbors.setLayout(new GridLayout(neighs.size(), 0));

			// add all of the neighbors to that panel
			for (int i = 0; i < neighs.size(); i++) {
				JLabel lab = new JLabel();
				lab.setFont(gotFontBody);
				lab.setText(neighs.get(i).getName());
				lab.setHorizontalAlignment(JLabel.CENTER);
				neighbors.add(lab);
			} // end addneighbors loop
			neighbors.setBorder(raisedWithColor);
			center.add(neighbors);

			// Now, add the strength
			JLabel streng = new JLabel();
			streng.setHorizontalAlignment(JLabel.CENTER);
			streng.setFont(gotFontHeader.deriveFont(48f));
			streng.setText("" + curr.getForcesVal());
			streng.setBorder(raisedWithColor);
			center.add(streng);

			this.add(center, BorderLayout.CENTER);
			center.revalidate();
		}// end makeCenterPanel

		// displays the cards and has a button for trading in cards
		public void makePlayingCardPanel(Graphics g) {
			ArrayList<Card> myCards = theGame.getCurrentPlayer().getCards();

			// reset selected cards everytime we draw this panel
			selectedCards = new ArrayList<Card>();

			// The card panel is a new panel
			JPanel pane = new JPanel();
			pane.setLayout(new BorderLayout());
			JPanel showCards = new JPanel();
			int cols = myCards.size();
			if (cols < 1)
				cols = 1;
			showCards.setLayout(new GridLayout(0, 5));

			// Get the image for this card
			for (int i = 0; i < myCards.size(); i++) {
				JPanel both = new JPanel();
				both.setLayout(new BorderLayout());
				both.setPreferredSize(new Dimension((int) (1.5 * xWidth),
						(int) (3 * yHeight)));

				// add card
				ImageIcon im = myCards.get(i).getMyImageIcon();
				CardPanel card = new CardPanel(im.getImage(), xWidth, yHeight);
				card.setPreferredSize(new Dimension((int) (1.5 * xWidth),
						(int) (3 * yHeight)));
				both.add(card, BorderLayout.CENTER);

				// add checkboxes
				JCheckBox checkBox = new JCheckBox();
				checkBox = new JCheckBox();
				checkBox.setActionCommand("" + i);
				checkBox.setSelected(false);
				checkBox.addItemListener(new CardBoxListener());
				checkBox.setHorizontalAlignment(SwingConstants.CENTER);
				both.add(checkBox, BorderLayout.SOUTH);

				showCards.add(both);
			} // end for

			showCards.setBorder(raisedWithColor);

			pane.add(showCards, BorderLayout.CENTER);
			if (theGame.getCurrentPlayer().getCards().size() >= 3) {
				JButton trade = new JButton("Choose Cards to Trade.");
				trade.setFont(gotFontBody.deriveFont(6));
				trade.addActionListener(new TradeClickListener());
				pane.add(trade, BorderLayout.SOUTH);
			} else {
				JPanel hold = new JPanel();
				hold.setLayout(new GridLayout(2, 0));
				JLabel trade = new JLabel("\t\t View Your Cards Above.\n ");
				JLabel trade2 = new JLabel(
						"Click \'Skip to the Next Phase\' Button to the Right to Move to Deploy Phase");
				trade.setFont(gotFontBody.deriveFont(4));
				trade.setHorizontalAlignment(JLabel.CENTER);
				trade2.setFont(gotFontBody.deriveFont(4));
				trade2.setHorizontalAlignment(JLabel.CENTER);
				hold.add(trade);
				hold.add(trade2);
				hold.setBorder(raisedWithColor);
				pane.add(hold, BorderLayout.SOUTH);
			}

			this.add(pane, BorderLayout.CENTER);
			showCards.repaint();
			showCards.revalidate();
			pane.revalidate();
			this.revalidate();
		}// end makeCardPanel

		//adds war button to panel
		public void makePlayingMyCountryBottomLabel() {
			JButton butt = new JButton("Go to War!");
			butt.addActionListener(new AttackListener());
			this.add(butt, BorderLayout.SOUTH);
			this.revalidate();

		}// end makeBottomLabel

		//adds transfer troops button to panel
		public void makeTransferMyCountryBottomLabel() {
			JButton trans = new JButton("Transfer Troops");
			trans.addActionListener(new TransferTroopListener());
			this.add(trans, BorderLayout.SOUTH);
			this.revalidate();
		}// end makeTransferMyCountryBottomLabel

		//adds attack button to panel
		public void makePlayingYourCountryBottomLabel() {
			JButton attack = new JButton("Attack");
			attack.addActionListener(new AttackListener());
			this.add(attack, BorderLayout.SOUTH);
			this.revalidate();
		}// end makePlayingYourCountryBottomLabel

		//adds place troops button
		public void makePlacementBottomLabel() {
			JButton place = new JButton("Place Troops Here");
			place.addActionListener(new PlaceAndReinforceListener());
			place.addActionListener(new TransferTroopListener());
			this.add(place, BorderLayout.SOUTH);
			this.revalidate();
		}// end makeplacementbottom

		// Only displays neighbors
		public void makePlacementCenterPanel() {
			JPanel neighbors = new JPanel();
			ArrayList<Country> neighs = curr.getNeighbors();
			neighbors.setLayout(new GridLayout(neighs.size(), 0));

			// add all of the neighbors to that panel
			for (int i = 0; i < neighs.size(); i++) {
				JLabel lab = new JLabel();
				lab.setHorizontalAlignment(JLabel.CENTER);
				lab.setFont(gotFontBody);
				lab.setText(neighs.get(i).getName());
				neighbors.add(lab);
			} // end addneighbors loop
			neighbors.setBorder(raisedWithColor);
			this.add(neighbors, BorderLayout.CENTER);
			neighbors.revalidate();
		}
		
		//update panel
		public void updatePanel() {
			updatePanel(null);
		}

		//update center panel
		public void updatePanel(Graphics g) {
			curr = theGame.getSelectedCountry();
			this.removeAll();
			this.setLocation(12 * xWidth, 1 * yHeight);
			this.setSize(xWidth * 20, yHeight * 14);

			if (curr == null) {
				JLabel directions = new JLabel();
				directions.setHorizontalAlignment(JLabel.CENTER);
				if (theGame.isFinished()) {
					directions.setFont(gotFontHeader.deriveFont(Font.BOLD, 24));
					directions.setText(theGame.getCurrentPlayer().getName()
							+ " has achieved total victory.");
					this.add(directions, BorderLayout.CENTER);
				} else if (theGame.isRedeemCardPhase()) {
					directions.setFont(gotFontHeader.deriveFont(Font.BOLD, 28));
					directions.setText("Redeem your cards");
					directions.setBorder(raisedWithColor);
					this.add(directions, BorderLayout.NORTH);
					makePlayingCardPanel(g);
				} // end if
				else if (theGame.isPlacePhase()) {
					directions.setFont(gotFontHeader.deriveFont(Font.BOLD, 34));
					directions.setText("Choose a Country to Claim");
					this.add(directions, BorderLayout.CENTER);
				} // end if
				else if (theGame.isDeployPhase()) {
					directions.setFont(gotFontHeader.deriveFont(Font.BOLD, 20));
					directions.setText("Choose a country to Deploy new units");
					this.add(directions, BorderLayout.CENTER);
				} else if (theGame.isReinforcePhase()) {
					directions.setFont(gotFontHeader.deriveFont(Font.BOLD, 30));
					directions.setText("Choose a Country to Reinforce");
					this.add(directions, BorderLayout.CENTER);
				} // end else if
				else if (theGame.isAttackPhase()) {
					if (theGame.getMoveFrom() != null) {
						directions.setText("Choose a Country to Attack");
					} // end if
					else if (theGame.getMoveTo() != null) {
						directions.setText("Choose a Country to Attack From");
					} // end else if
					else {
						directions.setText("Choose a Country to Go to War!");
					} // end else

					directions.setFont(gotFontHeader.deriveFont(Font.BOLD, 30));

					this.add(directions, BorderLayout.CENTER);
				} // end else if
				else {

					JLabel dir2 = new JLabel();
					directions.setFont(gotFontHeader.deriveFont(Font.BOLD, 34));
					dir2.setFont(gotFontHeader.deriveFont(Font.BOLD, 34));
					directions.setText("Choose a Country to Attack"); // or

					this.add(directions, BorderLayout.CENTER);
				} // end else

				this.revalidate();
				this.repaint();
			} // end if
			else {
				// This never changes - always displayed if a country is
				// displayed!
				makeTopLabel();

				if (theGame.isPlacePhase()) {
					makePlacementCenterPanel();
					makePlacementBottomLabel();
				} // end if
				else if (!theGame.isPlayPhase() && theGame.isReinforcePhase()) {
					makePlayingCenterPanel();
					makePlacementBottomLabel();

				} else if (theGame.isDeployPhase()) {
					makePlayingCenterPanel();
					// Only give this option if the country is yours
					if (theGame.getCurrentPlayer().equals(curr.getOccupier()))
						makePlacementBottomLabel();
				} // end elseif
					// we should make a specific panel for if a transfer is in
					// progress.
				else if (theGame.isAttackPhase()) {
					makePlayingCenterPanel();

					if (theGame.getCurrentPlayer().equals(curr.getOccupier())) {
						makePlayingMyCountryBottomLabel();
					} // end if
					else {
						makePlayingYourCountryBottomLabel();
					}
				} else {
					makePlayingCenterPanel();
					// Only give this option if the country is yours
					if (theGame.getCurrentPlayer().equals(curr.getOccupier()))
						makeTransferMyCountryBottomLabel();
				} // end outer else

			} // end updatePanel

		}// end updatePanel
	}// end countryPanel

	/******************************
	 * Listeners are Below this line!!!
	 *****************************/

	private class CardBoxListener implements ItemListener {

		@Override
		public void itemStateChanged(ItemEvent itE) {

			ArrayList<Card> playCards = theGame.getCurrentPlayer().getCards();

			// the action command is the card's index in the player's arraylist
			int index = Integer.parseInt(((JCheckBox) itE.getItem())
					.getActionCommand());

			// if it's selected
			if (itE.getStateChange() == ItemEvent.SELECTED) {
				selectedCards.add(playCards.get(index));
			} // end if
			else {
				selectedCards.remove(playCards.get(index));
			} // end else
		}// end itemstatechanged
	}// end cardboxlistener

	/*
	 * Handles the logic for trading in cards!
	 */
	private class TradeClickListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {

			if (cardsAreRedeemable()) {
				theGame.setCardsToRedeem(selectedCards);
				int armiesToAdd = theGame.redeemCards();
				theGame.getCurrentPlayer().addAvailableTroops(armiesToAdd);
				theGame.nextPhase();
			} // end else if
			else {

				if (selectedCards.size() < 0 || selectedCards.size() > 3) {
					JOptionPane.showMessageDialog(riskGUI.this,
							"Illegal amount of cards chosen.",
							"Can't redeem cards.",
							JOptionPane.INFORMATION_MESSAGE);
				} else {
					JOptionPane.showMessageDialog(riskGUI.this,
							"Cannot redeem cards chosen. Try again.",
							"Can't redeem cards.",
							JOptionPane.INFORMATION_MESSAGE);
				}
				selectedCards.clear();
			}
			repaint();
		}// end actionPerformed
	}// end tradeClickListener
	
	/*
	 * determines if the players cards are redeemable
	 */
	private boolean cardsAreRedeemable() {
		if (selectedCards.size() != 3)
			return false;
		// determine if they're a valid combo
		else {
			if (cardsHaveOneOfEach())
				return true;
			else if (cardsHaveThreeOfOne())
				return true;
			else
				return false;
		} // end else
	}// end cardsAreRedeemable
	
	/*
	 * determines if the player has 3 of one type of card
	 */
	private boolean cardsHaveThreeOfOne() {
		String unit = selectedCards.get(0).getUnit();

		// check the second card
		if (!unit.equalsIgnoreCase(selectedCards.get(1).getUnit())
				&& !selectedCards.get(1).getUnit().equalsIgnoreCase("wild"))
			return false;
		// check the third card
		if (!unit.equalsIgnoreCase(selectedCards.get(2).getUnit())
				&& !selectedCards.get(2).getUnit().equalsIgnoreCase("wild"))
			return false;

		return true;
	}

	/*
	 *determines if the player has one card of each kind 
	 */
	private boolean cardsHaveOneOfEach() {
		int[] unitTypes = new int[4];

		for (int i = 0; i < selectedCards.size(); i++) {
			if (selectedCards.get(i).getUnit().equalsIgnoreCase("infantry"))
				unitTypes[0]++;
			else if (selectedCards.get(i).getUnit().equalsIgnoreCase("cavalry"))
				unitTypes[1]++;
			else if (selectedCards.get(i).getUnit()
					.equalsIgnoreCase("artillery"))
				unitTypes[2]++;
			else if (selectedCards.get(i).getUnit().equalsIgnoreCase("wild"))
				unitTypes[3]++;
		} // end for

		// Look through all types - if there's two of anything other than wild,
		// it's not a match
		for (int i = 0; i < 3; i++) {
			if (unitTypes[i] > 1)
				return false;
		} // end for
		return true;
	}// end cardsHaveOneOfEach

	
	/*
	 * Passes to the next phase for the user and notifies the game.
	 */
	private class PassButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (theGame.isAttackPhase()) {
				if (theGame.skipAttackPhase()) {
					JOptionPane.showMessageDialog(riskGUI.this,
							"You earned a new card!", "Card Earned",
							JOptionPane.INFORMATION_MESSAGE);
				} // end if
			} else if (theGame.isReinforcePhase() && theGame.isPlayPhase()) {
				theGame.passReinforcementPhase();
			} else if (theGame.isRedeemCardPhase()) {
				if (!theGame.skipCardRedemption())
					JOptionPane.showMessageDialog(riskGUI.this,
							"Because you have 5 cards, you must redeem 3.",
							"Card Redemption Warning",
							JOptionPane.INFORMATION_MESSAGE);
			} // end else if
			else {
				JOptionPane.showMessageDialog(riskGUI.this,
						"Sorry, you're not allowed to skip this phase.",
						"Pass Warning", JOptionPane.INFORMATION_MESSAGE);
			}

			drawingPanel.repaint();
		}// end actionperformed
	}// end passbuttonlistener

	/*
	 * help button listener for opening the about
	 */
	private class HelpListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().compareTo("rules") == 0) {

				URL rules = null;
				try {
					rules = new URL(
							"http://www.cs.arizona.edu/~mercer/Projects/335/Final/RiskRules.pdf");
				} catch (MalformedURLException e1) {

					e1.printStackTrace();
				}
				JEditorPane ep = new JEditorPane("text/html", "<a href=\""
						+ rules.toString()
						+ "\">Rules given to us by Rick Mercer</a>" //
						+ "</body></html>");
				ep.setEditable(false);
				ep.addHyperlinkListener(new LinkClickListener(rules));
				JOptionPane.showMessageDialog(null, ep);

			} else if (e.getActionCommand().compareTo("about") == 0) {
				JOptionPane
						.showMessageDialog(
								riskGUI.this,
								"Dylan Tobia, Abigail Dodd, Sydney Komro, and Jewell Finder are members of the team Risky Business"
										+ "\n                                    This is a recreation of the popular board game, Risk.\n"
										+ "                                         Created for our CS335 class as our final project.",
								"About", JOptionPane.INFORMATION_MESSAGE);
			} else if (e.getActionCommand().compareTo("attack max") == 0) {

				theGame.changeAttackDice(3);
			} else if (e.getActionCommand().compareTo("attack 2") == 0) {

				theGame.changeAttackDice(2);
			} else if (e.getActionCommand().compareTo("attack 1") == 0) {

				theGame.changeAttackDice(1);
			} else if (e.getActionCommand().compareTo("defend max") == 0) {

				theGame.changeDefendDice(2);
			} else if (e.getActionCommand().compareTo("defend min") == 0) {

				theGame.changeDefendDice(1);
			} else if (e.getActionCommand().compareTo("how to play") == 0) {

				JOptionPane
						.showMessageDialog(
								riskGUI.this,
								"                                 Welcome to our implementation of Risk, Game of Thrones Edition!\n"
										+ "**********************************************DISCLAIMER******************************************************** \n"
										+ "IF A PLAYER LOSES AND IT IS JUST AI LEFT, OR IF YOU START A GAME WITH ONLY AI, THE GAME WILL PLAY UNTIL \n"
										+ "COMPLETION, BUT THEN SEEMS TO FREEZE. IT ISN'T ACTUALLY FROZEN, JUST BE PATIENT, IT WILL SHOW THE WINNER\n"
										+ "AND THEN THE MENU'S AND THING WILL BE ACCESSIBLE. WE ARE SORRY FOR THE INCONVENIECE.\n"
										+ "****************************************************************************************************************\n"
										+ "The game will first ask if you want to play a New Game, or load a previous save. If you would like to load\n"
										+ "the file types are .ser files. Next it will ask you the usual questions, how many humans, ai's, what \n"
										+ "faction you would like to play as, and what difficulty you would like the ai's to be. Then the game begins. \n"
										+ "To place an army, click on the NAME of a country, and then the \"Place Army\" button, if that country is not\n"
										+ "currently occupied, it will place a unit for you. Then it is the next players turn. Continue doing this until\n"
										+ "all countries are occupied. Then it will move on to the reinforcement phase. Just like with placing armies,\n"
										+ "to reinforce a country, select its NAME, and then the \"Place Army\" button. You can only reinforce countries\n"
										+ "you own. When all of your units have been placed, it will move on to the next phase, beginning with the redeem\n"
										+ "card phase. In this phase, the center panel will show you how many cards you currently have (If you have any).\n"
										+ "To redeem cards, click the radio boxes beneath the cards you wish to redeem, and click the \"Trade in Cards\"\n"
										+ "button. To continue to the next phase, the Deploy Phase, press the \"Skip Phase\" button located on the top\n"
										+ "right of the screen. You can never skip the Deploy phase, and just like before, you must reinforce countries\n"
										+ "that only you own. When you are finished you will automatically be moved to the Attack Phase. To attack a \n"
										+ "country, click either the country you want to attack's NAME, or the country you want to attack from's NAME\n"
										+ "and click either the \"Go To War\" button, or the \"Attack\" button (They are the same button, located at\n"
										+ "the bottom of the info pane). Then click the other country you wish to attack, or attack from, and click the\n"
										+ "attack button. You will be alerted to the outcome of the attack. If you have conquered a country, you will be\n"
										+ "prompted with how many units you wish to move there. If you have not conquered the country yet, you will need\n"
										+ "to reselect your attacking, and attack from countries. When you are finished attacking, press the \"Skip Phase\"\n"
										+ "button located at the top right of the screen. Now you are in the reinforce phase. Click a country you own that\n"
										+ "has more than 1 unit on it, and click the transfer troops button. Then click an attached, friendly country to place\n"
										+ "units upon, and click the transfer troops button. You will be prompted with how many units you wish to move from it.\n"
										+ "Keep in mind you MUST leave 1.When you are finished, press the \"Skip Phase\" button at the top right of the screen,\n "
										+ "to advance to the next players turn. Continue this until the game is over, or you wish to stop. To save, click \n"
										+ "File-Save Game, or close the window, which will then prompt you if you wish to save the game or not.\n"
										+ "                                                                               Happy conquering!");
			}
		}// actionPerformed
	}// end helpListener

	/*
	 * Handles what happens when you select a country
	 */
	private class CountryClickListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			
			Country lastSelected = theGame.getSelectedCountry();
			// step through all countries until the same name as the
			// actionCommand, then return that country
			// No country is selected if it's the card phase
			// because that's not allowed :)
			if (!theGame.isRedeemCardPhase()) {
				for (Country country : theGame.getGameMap().getCountries()) {
					if (country.getName().compareTo(e.getActionCommand()) == 0)
						theGame.setSelectedCountry(country);
				} // end for
			} // end if
			
			if (lastSelected != null && lastSelected.equals(theGame.getSelectedCountry())){
				doDoubleClickAction();
			}//end if

			drawingPanel.repaint();
		}// end actionperformed
	}// end countryclicklistener
	
	private void doDoubleClickAction(){
		//if we're in game setup
		if (!theGame.isPlayPhase() || theGame.isDeployPhase()){
			placeAndReinforceAction();
		}//end if
		//if we're in attack phase
		else if (theGame.isAttackPhase()){
			attackAction();
		}//end if
		else if (theGame.isReinforcePhase()){
			transferTroopAction();
		}//end else if
	}//end doDoubleClickAction

	/*
	 * Listener for transfer troops buttons. Handles moving troops from one
	 * country you own to another country that you own AND that is connected to
	 * the original country by a chain of 1 or more neighbors that you also own
	 */
	private class TransferTroopListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			transferTroopAction();
		}// end actionPerformed
	}// end transfertrooplistener
	
	public void transferTroopAction(){
		if (theGame.isPlayPhase() && theGame.isReinforcePhase()) {
			// Set the move from first
			if (theGame.getMoveFrom() == null) {
				if (theGame.playerIsOwner()) {
					theGame.setMoveFrom();
				} // end if
				else {
					JOptionPane
							.showMessageDialog(
									null,
									"You can only transfer troops from your own countries.",
									"Error", JOptionPane.ERROR_MESSAGE);
				}
			} // end if
				// Then set the move To
			else if (theGame.getMoveTo() == null) {
				if (theGame.playerIsOwner()) {
					theGame.setMoveTo();
					int numArmies = getArmiesToUse(theGame.getMoveFrom());// Get
																			// Armies
																			// to
																			// Move

					boolean success = theGame.transferTroops(numArmies);

					if (!success) {
						JOptionPane
								.showMessageDialog(
										null,
										"There must be a path of your own countries between your two choices.",
										"Error", JOptionPane.ERROR_MESSAGE);
					} // end if

					// Always clear the game, even if failure
					theGame.clearSelections();
				} // end if
				else {
					JOptionPane
							.showMessageDialog(
									null,
									"You can only transfer troops from your own countries.",
									"Error", JOptionPane.ERROR_MESSAGE);
				} // end else
			} // end else if
		} // end if

		drawingPanel.repaint();
	}
	
	// pops up a pane to ask how many units to move, which returns a string
	// it then tries to parse that string into an int, and if it does
	// compares
	// it to the total
	// number of units on a country. If it is larger, or equal to the total
	// number, throws an error, otherwise returns that number
	// to the gui
	public int getUnitsToMove(Country countryToRemoveUnits) {
		boolean moveFlag = false, continueFlag = false;
		int totalUnits = countryToRemoveUnits.getForcesVal(), unitsToReturn = 0;
		;
		String unitsToMove = "";

		while (!moveFlag) {
			unitsToMove = JOptionPane
					.showInputDialog("How Many armies? You must leave 1.");
			try {
				unitsToReturn = Integer.parseInt(unitsToMove);
				continueFlag = true;
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(null,
						"That was invalid number.", "Error",
						JOptionPane.ERROR_MESSAGE);
			}
			if (continueFlag) {
				if (unitsToReturn >= totalUnits) {
					JOptionPane.showMessageDialog(null,
							"You must leave 1 army.", "Error",
							JOptionPane.ERROR_MESSAGE);
				} else {
					moveFlag = true;
				}
			}
		}
		return unitsToReturn;

	}// end unitsToReturn

	/*
	 * Handles two teams going to war against each other!
	 */
	private class AttackListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			attackAction();
		}// end actionPerformed
	}// end AttackListener
	
	public void attackAction(){
		// One of the countries needs to be set
		if (theGame.getMoveFrom() == null && theGame.getMoveTo() == null) {
			if (theGame.playerIsOwner()) {
				if (theGame.getSelectedCountry().getForcesVal() > 1)
					theGame.setMoveFrom();
				else {
					JOptionPane
							.showMessageDialog(
									null,
									"Cannot attack from a country with only one army. Please choose another",
									"Error", JOptionPane.ERROR_MESSAGE);
				} // end else
			} // end if

			else {
				theGame.setMoveTo();
			} // end else
		} // end if

		// the other one needs to be set
		else if (theGame.getMoveFrom() == null) {
			if (theGame.playerIsOwner()) {
				// Only allow attack from a country with more than one army
				if (theGame.getSelectedCountry().getForcesVal() > 1) {
					theGame.setMoveFrom();

					if (theGame.getMoveTo().isMyNeighbor(
							theGame.getMoveFrom())) {
						attacker = theGame.getMoveFrom().getOccupier()
								.getFaction();
						defender = theGame.getMoveTo().getOccupier()
								.getFaction();
						theGame.attack();
						if (animationsOn)
							animations(attacker, defender);

						ArrayList<Dice> attack = theGame.getAttackDice();
						ArrayList<Dice> defense = theGame.getDefenseDice();

					} // end if
					else {
						JOptionPane.showMessageDialog(null,
								"You can only attack your neighbors.",
								"Error", JOptionPane.ERROR_MESSAGE);
						theGame.clearSelections();
					}
				} // end if
				else {
					JOptionPane
							.showMessageDialog(
									null,
									"Cannot attack from a country with only one army. Please choose another",
									"Error", JOptionPane.ERROR_MESSAGE);
				} // end else
			} // end if

			else {
				JOptionPane
						.showMessageDialog(
								null,
								"Cannot attack from someone else's country. Please choose another",
								"Error", JOptionPane.ERROR_MESSAGE);
			} // end else
		} // end else if

		// the other one needs to be set
		else if (theGame.getMoveTo() == null) {
			if (!theGame.playerIsOwner()) {
				theGame.setMoveTo();

				if (theGame.getMoveTo().isMyNeighbor(theGame.getMoveFrom())) {

					attacker = theGame.getMoveFrom().getOccupier()
							.getFaction();
					defender = theGame.getMoveTo().getOccupier()
							.getFaction();

					theGame.attack();
					if (animationsOn)
						animations(attacker, defender);

					// Saved so that they can be used for animations
					ArrayList<Dice> attack = theGame.getAttackDice();
					ArrayList<Dice> defense = theGame.getDefenseDice();
				} // end if

				else {
					JOptionPane.showMessageDialog(null,
							"You can only attack your neighbors.", "Error",
							JOptionPane.ERROR_MESSAGE);
					theGame.clearSelections();
				} // end else
			} // end if
			else {
				JOptionPane
						.showMessageDialog(
								null,
								"Cannot attack your own country. Please choose another",
								"Error", JOptionPane.ERROR_MESSAGE);
			} // end else
		} // end else if
		theGame.setSelectedCountry(null);
		drawingPanel.repaint();
	}//end attackAction

	/*
	 * sets up a Joptionpane to ask how many armies to move and determines if it is a valid value. 
	 */
	public int getArmiesToUse(Country countryToRemoveUnits) {
		boolean moveFlag = false, continueFlag = false;
		int totalUnits = countryToRemoveUnits.getForcesVal(), unitsToReturn = 0;
		String unitsToMove = "";

		while (!moveFlag) {
			unitsToMove = JOptionPane
					.showInputDialog("How many armies do you want to use? You must leave 1.");
			try {
				unitsToReturn = Integer.parseInt(unitsToMove);
				continueFlag = true;
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(null, "That was invalid number.",
						"Error", JOptionPane.ERROR_MESSAGE);
			}
			if (continueFlag) {
				if (unitsToReturn >= totalUnits) {
					JOptionPane.showMessageDialog(null, "Invalid number.",
							"Error", JOptionPane.ERROR_MESSAGE);
				} else {
					moveFlag = true;
				}
			}
		}
		return unitsToReturn;

	}// end getArmiesToAttack

	/*
	 * Listener for the place and reinforcement buttons. Lets users place troops
	 * on previously unclaimed countries or reinforce countries they already
	 * own.
	 */

	private class PlaceAndReinforceListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			placeAndReinforceAction();
		}// end actionPerformed
	}// end class
	
	public void placeAndReinforceAction(){
		if (theGame.isPlacePhase()) {
			if (theGame.getSelectedCountry().getOccupier() == null)
				theGame.play();
			else {
				JOptionPane.showMessageDialog(null,
						"You must pick an unoccupied country", "Error",
						JOptionPane.ERROR_MESSAGE);
			} // end else
		} // end if
		else if (theGame.isReinforcePhase() && !theGame.isPlayPhase()) {
			if (theGame.getSelectedCountry().getOccupier()
					.equals(theGame.getCurrentPlayer())) {
				theGame.play();
			} // end if
			else {
				JOptionPane.showMessageDialog(null,
						"You may only reinforce your own country", "Error",
						JOptionPane.ERROR_MESSAGE);
			} // end else
		} // end else if

		else if (theGame.isDeployPhase()) {
			if (theGame.getSelectedCountry().getOccupier()
					.equals(theGame.getCurrentPlayer())) {
				theGame.play();
			} // end if
			else {
				JOptionPane.showMessageDialog(null,
						"You may only reinforce your own country", "Error",
						JOptionPane.ERROR_MESSAGE);
			} // end else
		} // end else if

		theGame.setSelectedCountry(null);
		drawingPanel.repaint();
	}//end placeAndReinforceAction

	/*
	 * Creates a new Game
	 */
	private class NewGameListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (theGame != null)
				theGame.clear();
			splashNumPlayers();
		}// end action performed
	}// end game listener

	/*
	 * Lets the user choose between loading an old game and starting a new game
	 * 
	 */
	private class GameTypeListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			gameType = arg0.getActionCommand();
			if (gameType.compareTo("New Game!") == 0)
				splashNumPlayers();
			else
				loadGame();
		}// end actionperformed
	}// end GameTypeListener

	/*
	 * listener for the AI Difficulty menu
	 */
	private class AIDiffChangeListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {

			String newDifficulty = (String) JOptionPane.showInputDialog(null,
					"Please choose a Difficulty", "Set Difficulty",
					JOptionPane.QUESTION_MESSAGE, null, new Object[] { "Easy",
							"Medium", "Hard" }, "Easy");

			for (Player ai : theGame.getPlayers()) {
				if (ai instanceof AI) {
					if (((AI) ai).getMenuItem().getActionCommand()
							.compareTo(e.getActionCommand()) == 0) {
						switch (newDifficulty) {

						case "Easy":
							((AI) ai).setStrategy(new EasyAI((AI) ai));
							break;
						case "Medium":
							((AI) ai).setStrategy(new MediumAI((AI) ai));
							break;
						case "Hard":
							((AI) ai).setStrategy(new HardAI((AI) ai));
						default:
							break;
						}// end switch
					} // end if
				} // end if
			} // end for
		}// end actionperformed
	}// end aiDiffChangeListener

	/*
	 * clearButton calls theGame's method to clear all selections and repaints the drawingPanel
	 */
	private class clearButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {

			theGame.clearSelections();
			drawingPanel.repaint();
		}// end actionperformed
	}// end clearButtonListener

	/*
	 * music listener notifies sound player to pause the music and updates the menu.
	 */
	private class musicListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {

			musicOn = !musicOn;
			if (musicOn) {
				player.notifyPause();
			} else {
				player.pause();
			}
			setUpMenu();
			if (!splash)
				setUpAIMenu();
		}// end actionperformed
	}// end musicListener

	/*
	 * animationListener is called when menu option is pressed and resets the menu.
	 * turns off animations during attack.
	 */
	private class animationListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {

			animationsOn = !animationsOn;
			setUpMenu();
			if (!splash)
				setUpAIMenu();
		}// end actionperformed
	}// end musicListener

	/*
	 * LinkClickListener allows for the user to click on the Hyperlink when the rules menu listener is pressed
	 * and sends the user to the url specified on their desktop.
	 */
	private class LinkClickListener implements HyperlinkListener {
		private URL myUrl;

		//sets URL to rules
		public LinkClickListener(URL rules) {
			myUrl = rules;

		}

		@Override
		public void hyperlinkUpdate(HyperlinkEvent e) {
			if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
				Desktop myDesktop = Desktop.getDesktop();
				try {
					myDesktop.browse(myUrl.toURI());
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (URISyntaxException e1) {
					e1.printStackTrace();
				}

			}

		}
	}

	/*
	 * StatPanel on listener is called whenever stat panel button is pressed 
	 * This calls the method turnOnStatPanel()
	 */
	private class StatPanelTurnOnListener implements ActionListener {
		private boolean turnedOn = false;

		@Override
		public void actionPerformed(ActionEvent arg0) {
			turnOnStatPanel();
		}// end actionPerformed
	}// end StatPanelListener

	/*
	 * StatPanel off listener is called whenever button at the bottom of the statPanel is pressed. 
	 * This calls the method turnOffStatPanel()
	 */
	private class StatPanelTurnOffListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			turnOffStatPanel();
		}// end actionPerformed
	}// end turned off listener

	/*
	 * saveGameListener is called whenever saveGame menu item is pressed. 
	 * writes theGame object to outputStream and saves via JFileChooser
	 */
	private class saveGameListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			JFileChooser choose = new JFileChooser();
			choose.setCurrentDirectory(new File("./SavedGames"));
			int get = choose.showSaveDialog(null);
			if (get == JFileChooser.APPROVE_OPTION) {
				try {
					FileOutputStream gameToDisk = new FileOutputStream(
							choose.getSelectedFile() + ".ser");
					ObjectOutputStream outFile = new ObjectOutputStream(
							gameToDisk);
					outFile.writeObject(theGame);
					outFile.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}// end actionPerformed
	}// end saveGame listener

}// end GUI
