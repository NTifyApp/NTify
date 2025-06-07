/*
 * Copyright [2023-2025] [Gianluca Beil]
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
package com.spotifyxp.dialogs;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.spotifyxp.PublicValues;
import com.spotifyxp.configuration.ConfigValues;
import com.spotifyxp.events.EventSubscriber;
import com.spotifyxp.guielements.Settings;
import com.spotifyxp.swingextension.JFrame;
import com.spotifyxp.utils.ApplicationUtils;
import com.spotifyxp.utils.ClipboardUtil;
import com.spotifyxp.utils.ConnectionUtils;
import com.spotifyxp.utils.GraphicalMessage;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Objects;

public class LoginDialog {
    public JPanel contentPanel;
    public JPanel mainView;
    public JPanel zeroconfView;
    public JPanel oauthView;
    public JLabel chooseloginmethodlabel;
    public JButton zeroconfloginmethodbutton;
    public JButton oauthloginmethodbutton;
    public JLabel spotifyxplogo;

    //zeroconfView
    public JButton zeroconfbackbutton;
    public JTextArea zeroconfhowto;
    public JLabel ifnobrowserlabel;
    public JButton oauthbackbutton;
    public JButton oauthcopybutton;
    public JPanel oauthViewSubPanel;
    public JButton proxysettings;

    private static JFrame frame;
    private String oAuthCallbackURL;

    private class CustomSettingsDialog extends Settings {
        @Override
        public void addSetting(ConfigValues value, int i) {
            super.addSetting(value, i);
        }
    }

    private enum Views {
        MAIN,
        ZEROCONF,
        OAUTH
    }

    private LoginDialog(
            EventSubscriber onZeroconfCancel,
            EventSubscriber onZeroconfExecute,
            EventSubscriber onOauthCancel,
            EventSubscriber onOauthExecute
    ) {
        ImageIcon icon = new ImageIcon(Objects.requireNonNull(LoginDialog.class.getResource("/ntify.png")));
        Dimension defaultDimension = new Dimension(296, 384);
        spotifyxplogo.setIcon(
                new ImageIcon(icon.getImage().getScaledInstance((int) defaultDimension.getWidth() / 3, (int) defaultDimension.getWidth() / 3, Image.SCALE_FAST))
        );

        proxysettings.setForeground(PublicValues.globalFontColor);
        proxysettings.setText(PublicValues.language.translate("ui.login.openproxysettings"));
        proxysettings.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CustomSettingsDialog dialog = new CustomSettingsDialog();
                int counter = 0;
                for (ConfigValues values : ConfigValues.values()) {
                    if (values.category.equalsIgnoreCase("ui.settings.proxy")) {
                        dialog.addSetting(values, counter);
                        counter++;
                    }
                }
                dialog.pack();
                dialog.setVisible(true);
            }
        });

        chooseloginmethodlabel.setForeground(PublicValues.globalFontColor);
        zeroconfloginmethodbutton.setForeground(PublicValues.globalFontColor);
        oauthloginmethodbutton.setForeground(PublicValues.globalFontColor);
        oauthcopybutton.setForeground(PublicValues.globalFontColor);
        oauthbackbutton.setForeground(PublicValues.globalFontColor);
        ifnobrowserlabel.setForeground(PublicValues.globalFontColor);
        zeroconfhowto.setForeground(PublicValues.globalFontColor);
        zeroconfbackbutton.setForeground(PublicValues.globalFontColor);

        oauthloginmethodbutton.addActionListener(e -> {
            onOauthExecute.run(new EventSubscriber() {
                @Override
                public void run(Object... data) {
                    oAuthCallbackURL = (String) data[0];
                    try {
                        ConnectionUtils.openBrowser(oAuthCallbackURL);
                    } catch (URISyntaxException | IOException ex) {
                        GraphicalMessage.sorryError("Could not open OAuth URL! Press 'Copy' and open it manually");
                        throw new RuntimeException(ex);
                    }
                }
            });
            switchTo(Views.OAUTH);
        });
        oauthcopybutton.addActionListener(e -> {
            ClipboardUtil.set(oAuthCallbackURL);
        });
        oauthbackbutton.addActionListener(e -> {
            onOauthCancel.run();
            back();
        });

        zeroconfhowto.setText(PublicValues.language.translate("ui.login.message"));
        zeroconfhowto.setLineWrap(true);
        zeroconfhowto.setWrapStyleWord(true);
        zeroconfbackbutton.addActionListener(e -> {
            onZeroconfCancel.run();
            back();
        });
        zeroconfloginmethodbutton.addActionListener(e -> {
            onZeroconfExecute.run();
            switchTo(Views.ZEROCONF);
        });

        oauthView.setVisible(false);
        oauthViewSubPanel.setVisible(false);
        zeroconfView.setVisible(false);
    }

    private void switchTo(Views view) {
        switch (view) {
            case MAIN:
                mainView.setVisible(true);
                zeroconfView.setVisible(false);
                oauthViewSubPanel.setVisible(false);
                oauthView.setVisible(false);
                break;
            case ZEROCONF:
                zeroconfView.setVisible(true);
                mainView.setVisible(false);
                oauthView.setVisible(false);
                oauthViewSubPanel.setVisible(false);
                break;
            case OAUTH:
                oauthView.setVisible(true);
                zeroconfView.setVisible(false);
                oauthViewSubPanel.setVisible(true);
                mainView.setVisible(false);
        }
    }

    private void back() {
        switchTo(Views.MAIN);
    }

    public static void open(
            EventSubscriber onZeroconfCancel,
            EventSubscriber onZeroconfExecute,
            EventSubscriber onOauthCancel,
            EventSubscriber onOauthExecute
    ) {
        if (frame != null) {
            return;
        }
        frame = new JFrame(PublicValues.language.translate("ui.login.title").replace("%APPNAME%", ApplicationUtils.getName()));
        frame.setIconImage(Toolkit.getDefaultToolkit().getImage(LoginDialog.class.getResource("/ntify.png")));
        frame.setContentPane(new LoginDialog(onZeroconfCancel, onZeroconfExecute, onOauthCancel, onOauthExecute).contentPanel);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.pack();
        frame.setVisible(true);
    }

    public static void close() {
        frame.dispose();
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPanel = new JPanel();
        contentPanel.setLayout(new CardLayout(0, 0));
        contentPanel.setMinimumSize(new Dimension(296, 384));
        contentPanel.setPreferredSize(new Dimension(296, 384));
        mainView = new JPanel();
        mainView.setLayout(new GridLayoutManager(8, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainView.setVisible(true);
        contentPanel.add(mainView, "Card1");
        spotifyxplogo = new JLabel();
        spotifyxplogo.setAlignmentY(1.0f);
        spotifyxplogo.setText("");
        spotifyxplogo.setVerticalAlignment(0);
        mainView.add(spotifyxplogo, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        zeroconfloginmethodbutton = new JButton();
        Font zeroconfloginmethodbuttonFont = this.$$$getFont$$$(null, -1, 14, zeroconfloginmethodbutton.getFont());
        if (zeroconfloginmethodbuttonFont != null) zeroconfloginmethodbutton.setFont(zeroconfloginmethodbuttonFont);
        zeroconfloginmethodbutton.setText("Log in via zeroconf");
        mainView.add(zeroconfloginmethodbutton, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(166, -1), new Dimension(166, -1), new Dimension(166, -1), 0, false));
        oauthloginmethodbutton = new JButton();
        Font oauthloginmethodbuttonFont = this.$$$getFont$$$(null, -1, 14, oauthloginmethodbutton.getFont());
        if (oauthloginmethodbuttonFont != null) oauthloginmethodbutton.setFont(oauthloginmethodbuttonFont);
        oauthloginmethodbutton.setText("Log in via OAuth");
        mainView.add(oauthloginmethodbutton, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(166, -1), new Dimension(166, -1), new Dimension(166, -1), 0, false));
        chooseloginmethodlabel = new JLabel();
        Font chooseloginmethodlabelFont = this.$$$getFont$$$(null, -1, 16, chooseloginmethodlabel.getFont());
        if (chooseloginmethodlabelFont != null) chooseloginmethodlabel.setFont(chooseloginmethodlabelFont);
        chooseloginmethodlabel.setText("Choose login method");
        chooseloginmethodlabel.setVerticalAlignment(0);
        mainView.add(chooseloginmethodlabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        mainView.add(spacer1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 30), new Dimension(-1, 30), new Dimension(-1, 40), 0, false));
        final Spacer spacer2 = new Spacer();
        mainView.add(spacer2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10), 0, false));
        proxysettings = new JButton();
        proxysettings.setText("Button");
        mainView.add(proxysettings, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        mainView.add(spacer3, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 20), new Dimension(-1, 20), new Dimension(20, -1), 0, false));
        zeroconfView = new JPanel();
        zeroconfView.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        zeroconfView.setVisible(false);
        contentPanel.add(zeroconfView, "Card2");
        zeroconfbackbutton = new JButton();
        zeroconfbackbutton.setText("Back");
        zeroconfView.add(zeroconfbackbutton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        zeroconfhowto = new JTextArea();
        zeroconfhowto.setText("");
        zeroconfView.add(zeroconfhowto, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        final Spacer spacer4 = new Spacer();
        zeroconfView.add(spacer4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        oauthView = new JPanel();
        oauthView.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        oauthView.setVisible(false);
        contentPanel.add(oauthView, "Card3");
        oauthbackbutton = new JButton();
        oauthbackbutton.setText("Back");
        oauthView.add(oauthbackbutton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        oauthView.add(spacer5, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        oauthViewSubPanel = new JPanel();
        oauthViewSubPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        oauthView.add(oauthViewSubPanel, new GridConstraints(1, 0, 2, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        ifnobrowserlabel = new JLabel();
        ifnobrowserlabel.setText("If no browser opens, click 'Copy' to copy the URL");
        oauthViewSubPanel.add(ifnobrowserlabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_SOUTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        oauthcopybutton = new JButton();
        oauthcopybutton.setText("Copy");
        oauthViewSubPanel.add(oauthcopybutton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPanel;
    }

}
