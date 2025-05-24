package com.spotifyxp.panels;

import com.spotifyxp.PublicValues;
import com.spotifyxp.api.UnofficialSpotifyAPI;
import com.spotifyxp.guielements.SpotifyBrowseSection;
import com.spotifyxp.utils.AsyncActionListener;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

public class SpotifySectionPanel extends JScrollPane implements View {
    public static JButton backButton;
    public static JLayeredPane contentPanel;
    public static JLabel title;

    public SpotifySectionPanel() {
        contentPanel = new JLayeredPane();

        javax.swing.SwingUtilities.invokeLater(() -> getVerticalScrollBar().setValue(0));

        setVisible(false);
        setViewportView(contentPanel);

        contentPanel.setLayout(null);
        contentPanel.setBackground(getBackground());

        backButton = new JButton(PublicValues.language.translate("ui.back"));
        backButton.setBounds(0, 0, 89, 23);
        backButton.setForeground(PublicValues.globalFontColor);
        backButton.addActionListener(new AsyncActionListener(e -> {
            for(Component component : contentPanel.getComponents()) {
                if(component.getName() != null && component.getName().equals("BackButton") && component instanceof JButton) {
                    continue;
                }
                contentPanel.remove(component);
            }
            contentPanel.revalidate();
            contentPanel.repaint();
            ContentPanel.switchView(ContentPanel.lastView);
            ContentPanel.enableTabSwitch();

            // Back button is still visible. Redrawing the entire window should fix it
            ContentPanel.frame.revalidate();
            ContentPanel.frame.repaint();
        }));
        backButton.setName("BackButton");
        contentPanel.add(backButton, JLayeredPane.PALETTE_LAYER);

        title = new JLabel();
        title.setBounds(0, 0, 784, 50);
        title.setBackground(contentPanel.getBackground());
        title.setForeground(PublicValues.globalFontColor);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setBorder(null);
        title.setFont(title.getFont().deriveFont(Font.BOLD).deriveFont(23f));
    }

    public static class ViewDescriptorBuilder {
        private String title = "";
        private ArrayList<ViewDescriptorComponent> components = new ArrayList<>();

        public ViewDescriptorBuilder setTitle(String title) {
            this.title = title;
            return this;
        }

        public ViewDescriptorBuilder addComponent(ViewDescriptorComponent component) {
            components.add(component);
            return this;
        }

        private ArrayList<ViewDescriptorComponent> getComponents() {
            return components;
        }
    }

    public static class ViewDescriptorComponent {
        private String title = "";
        private Component component;

        public ViewDescriptorComponent(String title, Component component) {
            this.title = title;
            this.component = component;
        }
    }

    public void fillWith(ViewDescriptorBuilder builder) {
        ArrayList<ViewDescriptorComponent> components = builder.getComponents();

        title.setText(builder.title);

        contentPanel.add(title);

        int yCache = 80;
        int xCache = 10;
        int width = getWidth() - 32;
        int height = 261;
        int spacing = 70;
        int titleHeight = getFontMetrics(title.getFont()).getHeight();
        int titleSpacing = 5;

        for(ViewDescriptorComponent component : components) {
            JLabel titleOfEntry = new JLabel(component.title);
            titleOfEntry.setForeground(PublicValues.globalFontColor);
            titleOfEntry.setBounds(xCache, yCache - titleHeight - titleSpacing, width, titleHeight);

            component.component.setBounds(xCache, yCache, width, height);

            contentPanel.add(titleOfEntry);
            contentPanel.add(component.component);

            yCache += height + spacing;
        }

        contentPanel.setPreferredSize(new Dimension(width, yCache + 10 - title.getHeight()));
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    @Override
    public void makeVisible() {
        setVisible(true);
    }

    @Override
    public void makeInvisible() {
        setVisible(false);
    }
}
