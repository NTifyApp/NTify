package com.spotifyxp.dev;

import com.spotifyxp.swingextension.JFrame;
import com.spotifyxp.utils.ApplicationUtils;

import javax.swing.*;
import java.awt.*;

public class ErrorSimulator extends JFrame {
    private static JTextField exceptionDescriptionOptional;
    @SuppressWarnings("all")
    private static JButton submit;

    private class ContentPanel extends JPanel {
        public ContentPanel() {
            setLayout(null);
            setPreferredSize(new Dimension(400, 25));
            setTitle(ApplicationUtils.getName() + " - Error Generator (Developer Tools)");

            submit = new JButton("Submit");
            exceptionDescriptionOptional = new JTextField();

            submit.addActionListener(e -> {
                throw new RuntimeException(exceptionDescriptionOptional.getText());
            });

            exceptionDescriptionOptional.setBounds(5, 1, 318, 23);
            submit.setBounds(328, 1, 72, 23);

            add(submit, BorderLayout.EAST);
            add(exceptionDescriptionOptional, BorderLayout.CENTER);
        }
    }

    public void open() {
        if (isVisible()) return;
        setResizable(false);
        getContentPane().add(new ContentPanel());
        super.open();
    }
}
