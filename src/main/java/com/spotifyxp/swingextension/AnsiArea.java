package com.spotifyxp.swingextension;

import com.spotifyxp.PublicValues;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.utils.ClipboardUtil;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnsiArea extends JTextPane {
    private static final Pattern ANSI_SGR_PATTERN = Pattern.compile("\\u001B\\[(\\d+(?:;\\d+)*)m");
    private static Color defaultBackgroundColor;
    private static Font defaultFont;
    private static Color defaultColor;
    private ContextMenu contextMenu;
    private JTextPane itself;

    public AnsiArea() {
        defaultBackgroundColor = new Color(0, 0, 0, 0);
        defaultFont = getFont();
        defaultColor = new Color(128, 128, 128);

        itself = this;

        setEditable(false);

        contextMenu = new ContextMenu();
        contextMenu.addItem(PublicValues.language.translate("ui.general.copy"), new Runnable() {
            @Override
            public void run() {
                ClipboardUtil.set(itself.getText());
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if(SwingUtilities.isRightMouseButton(e)) {
                    contextMenu.showAt(itself, e.getX(), e.getY());
                }
            }
        });
    }

    public void parse(String input) {
        setText("");
        Matcher matcher = ANSI_SGR_PATTERN.matcher(input);
        Color currentBackground;
        Color currentTextColor;
        Font currentFont = defaultFont;
        boolean underline;
        int previousEnd = 0;
        Style lastStyle = null;
        while (matcher.find()) {
            underline = false;
            currentBackground = defaultBackgroundColor;
            currentTextColor = defaultColor;
            String code = matcher.group(1);
            if(!code.equals("0")) {
                if (code.contains(";")) {
                    switch (Integer.parseInt(code.split(";")[0])) {
                        case 0:
                            currentFont = defaultFont;
                            switch (Integer.parseInt(code.split(";")[1])) {
                                case 30:
                                    currentTextColor = new Color(57, 147, 212);
                                    break;
                                case 31:
                                    currentTextColor = new Color(240, 82, 79);
                                    break;
                                case 32:
                                    currentTextColor = new Color(92, 150, 44);
                                    break;
                                case 33:
                                    currentTextColor = new Color(166, 138, 13);
                                    break;
                                case 34:
                                    currentTextColor = new Color(57, 147, 212);
                                    break;
                                case 35:
                                    currentTextColor = new Color(167, 113, 191);
                                    break;
                                case 36:
                                    currentTextColor = new Color(0, 163, 163);
                                    break;

                                case 90:
                                    currentTextColor = new Color(89, 89, 89);
                                    break;
                                case 91:
                                    currentTextColor = new Color(255, 64, 80);
                                    break;
                                case 92:
                                    currentTextColor = new Color(79, 196, 20);
                                    break;
                                case 93:
                                    currentTextColor = new Color(229, 191, 0);
                                    break;
                                case 94:
                                    currentTextColor = new Color(31, 176, 255);
                                    break;
                                case 95:
                                    currentTextColor = new Color(237, 126, 237);
                                    break;
                                case 96:
                                    currentTextColor = new Color(0, 229, 229);
                                    break;
                                case 97:
                                    currentTextColor = new Color(255, 255, 255);
                                    break;

                                case 100:
                                    currentBackground = new Color(89, 89, 89);;
                                    break;
                                case 101:
                                    currentBackground = new Color(255, 64, 80);
                                    break;
                                case 102:
                                    currentBackground = new Color(79, 196, 20);
                                    break;
                                case 103:
                                    currentBackground = new Color(229, 191, 0);
                                    break;
                                case 104:
                                    currentBackground = new Color(31, 176, 255);
                                    break;
                                case 105:
                                    currentBackground = new Color(255, 85, 255);
                                    break;
                                case 106:
                                    currentBackground = new Color(237, 126, 237);
                                    break;
                                case 107:
                                    currentBackground = new Color(255, 255, 255);
                                    break;
                            }
                            break;
                        case 1:
                            currentFont = currentFont.deriveFont(Font.BOLD);
                            switch (Integer.parseInt(code.split(";")[1])) {
                                case 30:
                                    currentTextColor = new Color(0, 0, 0);
                                    break;
                                case 31:
                                    currentTextColor = new Color(240, 82, 79);
                                    break;
                                case 32:
                                    currentTextColor =  new Color(92, 150, 44);
                                    break;
                                case 33:
                                    currentTextColor = new Color(166, 138, 13);
                                    break;
                                case 34:
                                    currentTextColor = new Color(57, 147, 212);
                                    break;
                                case 35:
                                    currentTextColor = new Color(167, 113, 191);
                                    break;
                                case 36:
                                    currentTextColor = new Color(0, 163, 163);
                                    break;
                                case 37:
                                    currentTextColor = defaultColor;
                                    break;

                                case 90:
                                    currentTextColor = Color.DARK_GRAY;
                                    break;
                                case 91:
                                    currentTextColor = new Color(255, 85, 85);
                                    break;
                                case 92:
                                    currentTextColor = new Color(85, 255, 85);
                                    break;
                                case 93:
                                    currentTextColor = new Color(255, 255, 85);
                                    break;
                                case 94:
                                    currentTextColor = new Color(85, 85, 255);
                                    break;
                                case 95:
                                    currentTextColor = new Color(255, 85, 255);
                                    break;
                                case 96:
                                    currentTextColor = new Color(85, 255, 255);
                                    break;
                                case 97:
                                    currentTextColor = new Color(220, 220, 220);
                                    break;
                            }
                            break;
                        case 4:
                            underline = true;
                            switch (Integer.parseInt(code.split(";")[1])) {
                                case 30:
                                    currentTextColor = new Color(0, 0, 0);
                                    break;
                                case 31:
                                    currentTextColor = new Color(240, 82, 79);
                                    break;
                                case 32:
                                    currentTextColor =  new Color(92, 150, 44);
                                    break;
                                case 33:
                                    currentTextColor = new Color(166, 138, 13);
                                    break;
                                case 34:
                                    currentTextColor = new Color(57, 147, 212);
                                    break;
                                case 35:
                                    currentTextColor = new Color(167, 113, 191);
                                    break;
                                case 36:
                                    currentTextColor = new Color(0, 163, 163);
                                    break;
                                case 37:
                                    currentTextColor = defaultColor;
                                    break;
                            }
                            break;
                    }
                } else {
                    switch (Integer.parseInt(code.split(";")[0])) {
                        case 0:
                            currentFont = defaultFont;
                            break;
                        case 40:
                            currentBackground = defaultColor;
                            break;
                        case 41:
                            currentBackground = new Color(240, 82, 79);
                            break;
                        case 42:
                            currentBackground = new Color(92, 150, 44);
                            break;
                        case 43:
                            currentBackground = new Color(166, 138, 13);
                            break;
                        case 44:
                            currentBackground = new Color(57, 147, 212);
                            break;
                        case 45:
                            currentBackground = new Color(167, 113, 191);
                            break;
                        case 46:
                            currentBackground = new Color(0, 163, 163);
                            break;
                        case 47:
                            currentBackground = Color.WHITE;
                            break;
                    }
                }
            }else{
                currentTextColor = new Color(128, 128, 128);
            }

            int currentStart = matcher.start();
            if (currentStart > previousEnd) {
                StyledDocument doc = getStyledDocument();
                Style style = addStyle("", null);
                StyleConstants.setBold(style, true);
                StyleConstants.setForeground(style, currentTextColor);
                StyleConstants.setBackground(style, currentBackground);
                StyleConstants.setBold(style, currentFont.isBold());
                StyleConstants.setUnderline(style, underline);
                try {
                    doc.insertString(doc.getLength(), input.substring(previousEnd, currentStart), lastStyle);
                    lastStyle = style;
                } catch (BadLocationException e) {
                    throw new RuntimeException(e);
                }
            }else{
                Style style = addStyle("", null);
                StyleConstants.setBold(style, true);
                StyleConstants.setForeground(style, currentTextColor);
                StyleConstants.setBackground(style, currentBackground);
                StyleConstants.setBold(style, currentFont.isBold());
                StyleConstants.setUnderline(style, underline);
                lastStyle = style;
            }
            previousEnd = matcher.end();
        }

        if (previousEnd < input.length()) {
            StyledDocument doc = getStyledDocument();
            try {
                doc.insertString(doc.getLength(), input.substring(previousEnd), lastStyle);
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
