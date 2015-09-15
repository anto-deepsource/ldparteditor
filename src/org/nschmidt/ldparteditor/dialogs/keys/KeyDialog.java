/* MIT - License

Copyright (c) 2012 - this year, Nils Schmidt

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. */
package org.nschmidt.ldparteditor.dialogs.keys;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.nschmidt.ldparteditor.helpers.KeyBoardHelper;
import org.nschmidt.ldparteditor.state.KeyStateManager;

public class KeyDialog extends KeyDesign {

    public KeyDialog(Shell parentShell) {
        super(parentShell);
    }

    @Override
    public int open() {
        super.create();
        // TODO Add listeners here
        this.dialogArea.addKeyListener(new KeyListener() {

            @Override
            public void keyReleased(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                final int stateMask = e.stateMask;
                final int keyCode = e.keyCode;
                final boolean ctrlPressed = (stateMask & SWT.CTRL) != 0;
                final boolean altPressed = (stateMask & SWT.ALT) != 0;
                final boolean shiftPressed = (stateMask & SWT.SHIFT) != 0;
                final Event event = new Event();
                event.keyCode = keyCode;
                if (ctrlPressed) event.stateMask = event.stateMask | SWT.CTRL;
                if (altPressed) event.stateMask = event.stateMask | SWT.ALT;
                if (shiftPressed) event.stateMask = event.stateMask | SWT.SHIFT;
                lbl_PressKey[0].setText(KeyBoardHelper.getKeyString(event));
                lbl_PressKey[0].update();
                KeyStateManager.tmp_keyCode = keyCode;
                KeyStateManager.tmp_stateMask = stateMask;
                KeyStateManager.tmp_keyString = lbl_PressKey[0].getText();
            }
        });

        return super.open();
    }
}
