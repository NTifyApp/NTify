/*
 * Copyright [2024-2025] [Gianluca Beil]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.spotifyxp.panels;

import com.spotifyxp.PublicValues;
import com.spotifyxp.utils.ConnectionUtils;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;

public class Feedback extends JPanel implements View {
    public static JTextPane thankYouText;
    public static JPanel thankYouTextContainer;
    public static GridBagConstraints thankYouTextConstraints;

    public static JPanel contentContainer;

    public static JTextArea browserinfo;

    public static JPanel reportBugContainer;
    public static JLabel foundABugLabel;
    public static JButton reportBugButton;

    public static JPanel bugsContainer;
    public static JLabel whichBugsLabel;
    public static JButton viewBugsButton;

    public static JPanel visitProjectContainer;
    public static JLabel visitProjectLabel;
    public static JButton visitProjectButton;

    public static int contentSpacing;
    public static Dimension buttonSize;

    public static Font titleFont;

    public Feedback() {
        setVisible(false);
        setLayout(null);

        contentSpacing = PublicValues.applicationWidth / 100 * 30 + 50;
        buttonSize = new Dimension();
        buttonSize.height = 60;
        titleFont = getFont().deriveFont(18f);

        contentContainer = new JPanel();
        contentContainer.setLayout(new GridBagLayout());
        contentContainer.setBounds(contentSpacing, 0, PublicValues.applicationWidth - contentSpacing - 20, PublicValues.contentContainerHeight());
        add(contentContainer);

        thankYouTextContainer = new JPanel();
        thankYouTextContainer.setLayout(new GridBagLayout());
        thankYouTextContainer.setBounds(0, 0, PublicValues.applicationWidth / 100 * 30, PublicValues.contentContainerHeight());
        add(thankYouTextContainer);

        thankYouText = new JTextPane();
        thankYouText.setEditable(false);
        thankYouText.setForeground(PublicValues.globalFontColor);
        StyledDocument doc = thankYouText.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        try {
            doc.insertString(doc.getLength(), PublicValues.language.translate("ui.feedback.thankyoutext"), null);
            doc.setParagraphAttributes(0, doc.getLength(), center, false);
        } catch (BadLocationException ignored) {
        }
        thankYouTextConstraints = new GridBagConstraints();
        thankYouTextConstraints.anchor = GridBagConstraints.CENTER;
        thankYouTextConstraints.fill = GridBagConstraints.HORIZONTAL;
        thankYouTextConstraints.weightx = 1.0;
        thankYouTextConstraints.weighty = 1.0;
        thankYouText.setFont(thankYouText.getFont().deriveFont(17f));
        thankYouTextContainer.add(thankYouText, thankYouTextConstraints);

        browserinfo = new JTextArea(PublicValues.language.translate("ui.feedback.browserinfo"));
        browserinfo.setForeground(PublicValues.globalFontColor);
        browserinfo.setFont(getFont().deriveFont(14f));
        browserinfo.setWrapStyleWord(true);
        browserinfo.setLineWrap(true);
        browserinfo.setEditable(false);
        contentContainer.add(browserinfo, getConstraintsFor(0)); // Spacing

        reportBugContainer = new JPanel();
        reportBugContainer.setLayout(new BorderLayout());
        contentContainer.add(reportBugContainer, getConstraintsFor(1));

        foundABugLabel = new JLabel(PublicValues.language.translate("ui.feedback.foundabug"));
        foundABugLabel.setForeground(PublicValues.globalFontColor);
        foundABugLabel.setFont(titleFont);
        reportBugContainer.add(foundABugLabel, BorderLayout.NORTH);

        reportBugButton = new JButton(PublicValues.language.translate("ui.feedback.reportbug"));
        reportBugButton.setForeground(PublicValues.globalFontColor);
        reportBugButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    ConnectionUtils.openBrowser("https://github.com/NTifyApp/NTify/issues/new");
                } catch (URISyntaxException | IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        reportBugButton.setPreferredSize(buttonSize);
        reportBugContainer.add(reportBugButton, BorderLayout.CENTER);

        bugsContainer = new JPanel();
        bugsContainer.setLayout(new BorderLayout());
        contentContainer.add(bugsContainer, getConstraintsFor(2));

        whichBugsLabel = new JLabel(PublicValues.language.translate("ui.feedback.seeknownbugs"));
        whichBugsLabel.setFont(titleFont);
        whichBugsLabel.setForeground(PublicValues.globalFontColor);
        bugsContainer.add(whichBugsLabel, BorderLayout.NORTH);

        viewBugsButton = new JButton(PublicValues.language.translate("ui.feedback.viewbugs"));
        viewBugsButton.setForeground(PublicValues.globalFontColor);
        viewBugsButton.setPreferredSize(buttonSize);
        viewBugsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    ConnectionUtils.openBrowser("https://github.com/NTifyApp/NTify/issues");
                } catch (URISyntaxException | IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        bugsContainer.add(viewBugsButton, BorderLayout.CENTER);

        visitProjectContainer = new JPanel();
        visitProjectContainer.setLayout(new BorderLayout());
        contentContainer.add(visitProjectContainer, getConstraintsFor(3));

        visitProjectLabel = new JLabel(PublicValues.language.translate("ui.feedback.visitprojectlabel"));
        visitProjectLabel.setForeground(PublicValues.globalFontColor);
        visitProjectLabel.setFont(titleFont);
        visitProjectContainer.add(visitProjectLabel, BorderLayout.NORTH);

        visitProjectButton = new JButton(PublicValues.language.translate("ui.feedback.viewproject"));
        visitProjectButton.setForeground(PublicValues.globalFontColor);
        visitProjectButton.setPreferredSize(buttonSize);
        visitProjectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    ConnectionUtils.openBrowser("https://github.com/NTifyApp/NTify");
                } catch (URISyntaxException | IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        visitProjectContainer.add(visitProjectButton, BorderLayout.CENTER);

        contentContainer.add(new JPanel(), getConstraintsFor(5)); // Spacing
    }

    private GridBagConstraints getConstraintsFor(int number) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = number;
        gbc.weighty = 1;
        gbc.weightx = 1;
        return gbc;
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
