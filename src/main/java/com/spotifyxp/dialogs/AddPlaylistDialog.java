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
package com.spotifyxp.dialogs;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.spotifyxp.PublicValues;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.swingextension.JDialog;
import com.spotifyxp.swingextension.JImagePanel;
import com.spotifyxp.utils.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;

public class AddPlaylistDialog extends JDialog {
    public JPanel contentPane;
    public JButton okbutton;
    public JButton cancelbutton;
    public JTextField playlistName;
    public JCheckBox playlistVisibility;
    public JLabel playlistNameLabel;
    public JTextArea playlistDescriptionText;
    public JLabel playlistDescriptionLabel;
    public JCheckBox playlistCollaborative;
    public JButton playlistChangeImageButton;
    public JImagePanel playlistImage;

    private byte[] imageBytes;
    private final ContextMenu fileSelectMenu;
    private final JDialog thisDialog = this;

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
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(4, 4, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(3, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        okbutton = new JButton();
        okbutton.setText("");
        panel2.add(okbutton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cancelbutton = new JButton();
        cancelbutton.setText("");
        panel2.add(cancelbutton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(5, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(0, 1, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        playlistName = new JTextField();
        panel3.add(playlistName, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        playlistNameLabel = new JLabel();
        playlistNameLabel.setText("");
        panel3.add(playlistNameLabel, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        playlistDescriptionLabel = new JLabel();
        playlistDescriptionLabel.setText("");
        panel3.add(playlistDescriptionLabel, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        panel3.add(separator1, new GridConstraints(2, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10), 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel3.add(scrollPane1, new GridConstraints(4, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        playlistDescriptionText = new JTextArea();
        scrollPane1.setViewportView(playlistDescriptionText);
        final JSeparator separator2 = new JSeparator();
        contentPane.add(separator2, new GridConstraints(2, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10), 0, false));
        playlistVisibility = new JCheckBox();
        playlistVisibility.setSelected(false);
        playlistVisibility.setText("");
        contentPane.add(playlistVisibility, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        playlistCollaborative = new JCheckBox();
        playlistCollaborative.setText("");
        contentPane.add(playlistCollaborative, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        playlistChangeImageButton = new JButton();
        playlistChangeImageButton.setText("");
        contentPane.add(playlistChangeImageButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        playlistImage = new JImagePanel();
        playlistImage.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(playlistImage, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

    public static class Playlist {
        /**
         * Can be empty
         */
        public final String imageBase64;
        public final String name;
        public final String description;
        public final boolean isPublic;
        public final boolean isCollaborative;

        Playlist(
                String imageBase64,
                String name,
                String description,
                boolean isPublic,
                boolean isCollaborative
        ) {
            this.imageBase64 = imageBase64;
            this.name = name;
            this.description = description;
            this.isPublic = isPublic;
            this.isCollaborative = isCollaborative;
        }
    }

    @FunctionalInterface
    public interface OkRunnable {
        void run(Playlist playlist);
    }

    public AddPlaylistDialog() throws IOException {
        super();
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(okbutton);

        Set<String> formats = Arrays.stream(ImageIO.getReaderFormatNames())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        fileSelectMenu = new ContextMenu();
        fileSelectMenu.addItem(PublicValues.language.translate("addplaylist.ctxmenu.fromurl"), new Runnable() {
            @Override
            public void run() {
                String[] url = new String[]{JOptionPane.showInputDialog(thisDialog, PublicValues.language.translate("addplaylist.image.dialog.description"), PublicValues.language.translate("addplaylist.image.dialog.title"), JOptionPane.PLAIN_MESSAGE)};
                if (url[0] == null || url[0].isEmpty()) {
                    return;
                }
                playlistImage.setImage(() -> {
                    try {
                        imageBytes = convertToJPEG(new URL(url[0]).openStream());
                        return imageBytes;
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(thisDialog, PublicValues.language.translate("addplaylist.image.error") + " " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        ConsoleLogging.Throwable(e);
                        return new byte[0];
                    }
                });
            }
        });
        fileSelectMenu.addItem(PublicValues.language.translate("addplaylist.ctxmenu.fromfile"), new Runnable() {
            @Override
            public void run() {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.setFileFilter(new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        if (f.isDirectory()) return true;
                        String name = f.getName().toLowerCase();
                        return formats.stream().anyMatch(fmt -> name.endsWith("." + fmt));
                    }

                    @Override
                    public String getDescription() {
                        return PublicValues.language.translate("addplaylist.fileselect.description") + " (*." + String.join(", *.", formats) + ")";
                    }
                });
                if (chooser.showOpenDialog(thisDialog) == JFileChooser.APPROVE_OPTION) {
                    try {
                        imageBytes = convertToJPEG(Files.newInputStream(Paths.get(chooser.getSelectedFile().getAbsolutePath())));
                        playlistImage.setImage(imageBytes);
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(thisDialog, PublicValues.language.translate("addplaylist.image.error") + " " + e.getMessage(), PublicValues.language.translate("addplaylist.image.error.title"), JOptionPane.ERROR_MESSAGE);
                        ConsoleLogging.Throwable(e);
                    }
                }
            }
        });

        playlistNameLabel.setForeground(PublicValues.globalFontColor);
        playlistNameLabel.setText(PublicValues.language.translate("addplaylist.name"));

        playlistChangeImageButton.setForeground(PublicValues.globalFontColor);
        playlistChangeImageButton.setText(PublicValues.language.translate("addplaylist.image.button"));
        playlistChangeImageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileSelectMenu.showAt(thisDialog.getRootPane(),
                        playlistChangeImageButton.getX(),
                        playlistChangeImageButton.getY() + playlistChangeImageButton.getHeight(),
                        playlistChangeImageButton.getWidth(),
                        null);
            }
        });

        playlistVisibility.setForeground(PublicValues.globalFontColor);
        playlistVisibility.setText(PublicValues.language.translate("addplaylist.visibility"));
        playlistVisibility.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (playlistVisibility.isSelected()) {
                    playlistCollaborative.setSelected(false);
                }
            }
        });

        playlistCollaborative.setForeground(PublicValues.globalFontColor);
        playlistCollaborative.setText(PublicValues.language.translate("addplaylist.collaborative"));
        playlistCollaborative.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (playlistCollaborative.isSelected()) {
                    playlistVisibility.setSelected(false);
                }
            }
        });

        playlistDescriptionLabel.setForeground(PublicValues.globalFontColor);
        playlistDescriptionLabel.setText(PublicValues.language.translate("addplaylist.description"));

        okbutton.setText(PublicValues.language.translate("addplaylist.ok"));

        cancelbutton.setText(PublicValues.language.translate("addplaylist.cancel"));
    }

    public byte[] convertToJPEG(InputStream source) throws IOException {
        BufferedImage image = ImageIO.read(source);
        BufferedImage convertedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        convertedImage.createGraphics().drawImage(image, 0, 0, Color.WHITE, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(convertedImage, "jpg", baos);
        return baos.toByteArray();
    }

    public void show(OkRunnable ok, Runnable cancel, Runnable onClose) {
        setModal(true);
        setLocation(ContentPanel.frame.getCenter());
        setTitle(PublicValues.language.translate("addplaylist.title"));
        okbutton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String base64EncodedImageData = "";
                if (imageBytes != null) {
                    base64EncodedImageData = Base64.getEncoder().encodeToString(imageBytes);
                    if (StringUtils.calculateStringSizeInKilobytes(base64EncodedImageData) > 255.0) {
                        JOptionPane.showMessageDialog(thisDialog, PublicValues.language.translate("addplaylist.dialog.toobig.description"), PublicValues.language.translate("addplaylist.dialog.toobig.title"), JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                if (playlistName.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(thisDialog, PublicValues.language.translate("addplaylist.dialog.noname.description"), PublicValues.language.translate("addplaylist.dialog.noname.title"), JOptionPane.ERROR_MESSAGE);
                    return;
                }
                ok.run(new Playlist(
                        base64EncodedImageData,
                        playlistName.getText(),
                        playlistDescriptionText.getText(),
                        playlistVisibility.isSelected(),
                        playlistCollaborative.isSelected()
                ));
                dispose();
                onClose.run();
            }
        });
        cancelbutton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
                cancel.run();
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose.run();
                dispose();
            }
        });
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        pack();
        setVisible(true);
    }
}
