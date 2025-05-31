/*
 * Copyright [2023-2024] [Gianluca Beil]
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
package com.spotifyxp.swingextension;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;

public class CustomLengthTextField extends JTextField {

    protected final boolean upper;
    protected int maxlength;

    public CustomLengthTextField() {
        this(-1);
    }

    public CustomLengthTextField(int length, boolean upper) {
        this(length, upper, null);
    }

    public CustomLengthTextField(int length, InputVerifier inpVer) {
        this(length, false, inpVer);
    }

    /**
     * @param length - maksimalan length
     * @param upper  - turn it to upercase
     * @param inpVer - InputVerifier
     */
    public CustomLengthTextField(int length, boolean upper, InputVerifier inpVer) {
        super();
        this.maxlength = length;
        this.upper = upper;
        if (length > 0) {
            AbstractDocument doc = (AbstractDocument) getDocument();
            doc.setDocumentFilter(new DocumentSizeFilter());
        }
        setInputVerifier(inpVer);
    }

    public CustomLengthTextField(int length) {
        this(length, false);
    }

    public void setMaxLength(int length) {
        this.maxlength = length;
    }

    class DocumentSizeFilter extends DocumentFilter {

        public void insertString(FilterBypass fb, int offs, String str, AttributeSet a)
                throws BadLocationException {

            //This rejects the entire insertion if it would make
            //the contents too long. Another option would be
            //to truncate the inserted string so the contents
            //would be exactly maxCharacters in length.
            if ((fb.getDocument().getLength() + str.length()) <= maxlength) {
                super.insertString(fb, offs, str, a);
            }
        }

        public void replace(FilterBypass fb, int offs,
                            int length,
                            String str, AttributeSet a)
                throws BadLocationException {

            if (upper) {
                str = str.toUpperCase();
            }

            //This rejects the entire replacement if it would make
            //the contents too long. Another option would be
            //to truncate the replacement string so the contents
            //would be exactly maxCharacters in length.
            int charLength = fb.getDocument().getLength() + str.length() - length;

            if (charLength <= maxlength) {
                super.replace(fb, offs, length, str, a);
            }
        }

        private void focusNextComponent() {
            if (CustomLengthTextField.this == KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent();
            }
        }
    }
}