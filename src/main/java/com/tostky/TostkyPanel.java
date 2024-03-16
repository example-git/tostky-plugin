package com.tostky;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import javax.inject.Inject;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;


public class TostkyPanel extends PluginPanel {
    private JLabel cannonBallCountLabel;


    private TostkyPlugin plugin;
    private TostkyConfig config; // Ensured config is injected

    @Inject
    public TostkyPanel(TostkyPlugin plugin, TostkyConfig config) {
        this.plugin = plugin;
        this.config = config;
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        cannonBallCountLabel = new JLabel("Cannonballs: 0"); // Initialize the label here

        rebuild();
    }



    void rebuild() {
        removeAll();

        add(new PluginErrorPanel());

        JPanel contentPanel = new JPanel();
        JPanel cannonBallPanel = new JPanel();
        cannonBallPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        cannonBallPanel.setLayout(new BorderLayout());
        cannonBallPanel.add(cannonBallCountLabel, BorderLayout.WEST);

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> plugin.resetCannonBallCount());
        cannonBallPanel.add(resetButton, BorderLayout.EAST);

        add(cannonBallPanel);

        contentPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // Add labels and buttons for each ammunition type
        addAmmoRow(contentPanel, "Arrows", plugin.getArrowCount(), e -> plugin.resetPersistArrow());
        addAmmoRow(contentPanel, "Bolts", plugin.getBoltCount(), e -> plugin.resetPersistBolt());
        addAmmoRow(contentPanel, "Darts", plugin.getDartCount(), e -> plugin.resetPersistDart());
        addAmmoRow(contentPanel, "Javelins", plugin.getJavelinCount(), e -> plugin.resetPersistJavelin());
        addAmmoRow(contentPanel, "Throwing Axes", plugin.getThrowingAxeCount(), e -> plugin.resetPersistThrowingAxe());
        addAmmoRow(contentPanel, "Throwing Knives", plugin.getThrowingKnifeCount(), e -> plugin.resetPersistThrowingKnife());

        // Add toggle for counting picked items; pass contentPanel to meet the method's parameter requirement
        addToggleForCountingPickedItems(contentPanel);

        add(contentPanel);
        revalidate();
        repaint();
    }

    void updateCounts() {
        ammoLabels.forEach((name, label) -> {
            switch (name) {
//                case "Cannonballs":
//                    label.setText("Cannonballs: " + Math.abs(plugin.getCannonBallCount()));
//                    break;
                case "Arrows":
                    label.setText("Arrows: " + Math.abs(plugin.getArrowCount()));
                    break;
                case "Bolts":
                    label.setText("Bolts: " + Math.abs(plugin.getBoltCount()));
                    break;
                case "Darts":
                    label.setText("Darts: " + Math.abs(plugin.getDartCount()));
                    break;
                case "Javelins":
                    label.setText("Javelins: " + Math.abs(plugin.getJavelinCount()));
                    break;
                case "Throwing Axes":
                    label.setText("Throwing Axes: " + Math.abs(plugin.getThrowingAxeCount()));
                    break;
                case "Throwing Knives":
                    label.setText("Throwing Knives: " + Math.abs(plugin.getThrowingKnifeCount()));
                    break;
            }
        });
    }




    private Map<String, JLabel> ammoLabels = new HashMap<>();

    private JLabel addAmmoRow(JPanel panel, String ammoName, int count, ActionListener resetAction) {
        JPanel ammoRow = new JPanel();
        ammoRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        ammoRow.setLayout(new BorderLayout());

        JLabel ammoLabel = new JLabel(ammoName + ": " + count);
        ammoRow.add(ammoLabel, BorderLayout.WEST);

        ammoLabels.put(ammoName, ammoLabel);

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(resetAction);
        ammoRow.add(resetButton, BorderLayout.EAST);

        panel.add(ammoRow);
        return ammoLabel;
    }

    private void addToggleForCountingPickedItems(JPanel panel) {
        // Determine the initial state
        boolean isCountingEnabled = config.countPickedUpItems();

        // Initialize the button with dynamic text based on the initial state
        JToggleButton countPickedToggle = new JToggleButton(isCountingEnabled ? "Disable Counting Pickups" : "Enable Counting Pickups", isCountingEnabled);
        countPickedToggle.addItemListener(e -> {
            boolean isSelected = countPickedToggle.isSelected();
            // Toggle the configuration setting

            // Update the button text based on the new state
            if(isSelected) {
                countPickedToggle.setText("Disable Counting Pickups");
            } else {
                countPickedToggle.setText("Enable Counting Pickups");
            }
            // Optionally notify other parts of the plugin to reflect this configuration change
        });

        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        togglePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JLabel label = new JLabel("Count Picked Up Items:");
        //togglePanel.add(label);
        togglePanel.add(countPickedToggle);

        panel.add(togglePanel);
    }

    public void updateCannonBallCount(int count) {
        cannonBallCountLabel.setText("Cannonballs: " + count);
    }



}
