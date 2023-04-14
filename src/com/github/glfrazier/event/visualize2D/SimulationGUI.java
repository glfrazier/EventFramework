package com.github.glfrazier.event.visualize2D;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

import com.github.glfrazier.event.Event;
import com.github.glfrazier.event.EventProcessor;
import com.github.glfrazier.event.EventingSystem;

@SuppressWarnings("serial")
public class SimulationGUI extends JFrame implements EventProcessor {

	private static Event TIMESTEP_EVENT = new Event() {
		public String toString() {
			return "TIMESTEP";
		}
	};

	private JButton startButton;
	private JButton stopButton;
	private JButton resetButton;
	private JPanel display;
	private JTextField messagePane;
	private JButton clearMsgButton;
	private JLabel timeLabel;
	private Simulation sim;
	private JTextField scaler;

	private double scale = 1.0;

	private Map<String, JComponent> components;

	public SimulationGUI(Simulation sim) {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.sim = sim;
		sim.setGUI(this);
		components = new HashMap<String, JComponent>();
		timeLabel = new JLabel("0");
		timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		// timeLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
		Box timeBox = Box.createHorizontalBox();
		timeBox.setBorder(new BevelBorder(BevelBorder.LOWERED));
		timeBox.add(Box.createHorizontalGlue());
		timeBox.add(new JLabel("Time:"));
		timeBox.add(Box.createHorizontalStrut(4));
		timeBox.add(timeLabel);
		timeBox.add(Box.createHorizontalGlue());
		components.put("timeBox", timeBox);
		scaler = new JFormattedTextField(NumberFormat.getNumberInstance());
		components.put("scaler", scaler);
		scaler.setColumns(6);
		scaler.setText("1.00");
		scaler.setHorizontalAlignment(JTextField.RIGHT);
		scaler.setAction(new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String dStr = scaler.getText();
				if (dStr.indexOf('.') < 0) {
					dStr += '.';
				}
				double d = scale;
				do {
					if (dStr.indexOf('.') < dStr.length() - 2) {
						errorNotice("The scale's greatest granularity is hundredths.");
						break;
					}
					try {
						d = Double.parseDouble(dStr);
					} catch (NumberFormatException ex) {
						d = scale;
					}
				} while (false);
				scale = d;
				scaler.setText(String.format("%.2f", scale));
			}

		});
		System.out.println("scaler: size=" + scaler.getSize() + ", preferred=" + scaler.getPreferredSize());
		startButton = new JButton("Play/Resume");
		components.put("startButton", startButton);
		startButton.addActionListener(onStart());
		stopButton = new JButton("Stop/Pause");
		components.put("stopButton", stopButton);
		stopButton.addActionListener(onStop());
		resetButton = new JButton("Reset");
		components.put("resetButton", resetButton);
		resetButton.addActionListener(onReset());
		display = new JPanel() {

			@Override
			public void paint(Graphics g) {
				sim.paint(g, display);
			}
		};
		display.setDoubleBuffered(true);
		display.setBackground(Color.WHITE);
		messagePane = new JTextField(40);
		messagePane.setEditable(false);
		clearMsgButton = new JButton("Clear");
		clearMsgButton.setAction(new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) {
				messagePane.setText("");
			}
			
		});
		Box messageBox = Box.createHorizontalBox();
		messageBox.add(Box.createGlue());
		messageBox.add(messagePane);
		messageBox.add(Box.createHorizontalStrut(6));
		messageBox.add(clearMsgButton);
		messageBox.add(Box.createHorizontalStrut(6));
		Container pane = this.getContentPane();
		pane.setLayout(new BorderLayout());

		scaler.setMaximumSize(scaler.getPreferredSize());
		Box sBox = Box.createHorizontalBox();
		sBox.add(Box.createGlue());
		sBox.add(new JLabel("Scale:"));
		sBox.add(Box.createHorizontalStrut(4));
		sBox.add(scaler);
		sBox.add(Box.createGlue());

		Box box = Box.createVerticalBox();

		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		GridLayout gridLayout = new GridLayout(5, 1);
		gridLayout.setVgap(5);
		panel.setLayout(gridLayout);
		panel.add(timeBox);
		panel.add(sBox);
		panel.add(startButton);
		panel.add(stopButton);
		panel.add(resetButton);
		panel.setMaximumSize(panel.getPreferredSize());
		box.add(Box.createVerticalStrut(10));
		box.add(panel);
		box.add(Box.createGlue());

		pane.add(box, BorderLayout.WEST);
		pane.add(display, BorderLayout.CENTER);
		pane.add(messageBox, BorderLayout.SOUTH);
//		pane.add(messagePane, BorderLayout.SOUTH);
//		showOnScreen(1, this);
		Dimension d = getPreferredSize();
		setSize((int) (1.5 * d.width), 2 * d.height);
		this.setVisible(true);
		// update the time every 10th of a second
		sim.getEventingSystem().scheduleEventRelative(this, TIMESTEP_EVENT, 100);
	}

	public void errorNotice(String msg) {
		messagePane.setText(msg);
	}

	public void elementChanged() {
		if (display != null) {
			display.repaint();
		}
	}

	private ActionListener onReset() {
		return new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Reset pressed");
			}

		};
	}

	private ActionListener onStop() {
		return new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Stop pressed");
			}

		};
	}

	private ActionListener onStart() {
		return new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Start pressed");
			}

		};
	}

	@Override
	public void process(Event e, EventingSystem eventingSystem, long t) {
		timeLabel.setText(String.format("%3.1f", eventingSystem.getElapsedTime() / 1000.0));
		display.repaint();
		eventingSystem.scheduleEventRelative(this, e, 100);
	}

	public double getScale() {
		return scale;
	}

}
