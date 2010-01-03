// JugglingLabWindow.java
//
// Copyright 2003 by Jack Boyce (jboyce@users.sourceforge.net) and others

/*
    This file is part of Juggling Lab.

    Juggling Lab is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    Juggling Lab is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Juggling Lab; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package jugglinglab.core;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import org.xml.sax.*;

import jugglinglab.jml.*;
import jugglinglab.notation.*;
import jugglinglab.util.*;
import jugglinglab.view.*;


public class JugglingLabWindow extends JFrame implements ActionListener {
    static ResourceBundle guistrings;
    // static ResourceBundle errorstrings;
    static {
        guistrings = ResourceBundle.getBundle("GUIStrings");
        // errorstrings = ResourceBundle.getBundle("ErrorStrings");
    }

	protected JugglingLabPanel jlp = null;
	protected String uploadscript = null;
	protected String patfile = null;
	protected Animator uploadcallback = null;
	
	
	public JugglingLabWindow(String title) throws JuggleExceptionUser, JuggleExceptionInternal {
		this(title, null, null, null);
	}
	
    public JugglingLabWindow(String title, String uploadscript, String patfile, Animator uc) throws JuggleExceptionUser, JuggleExceptionInternal {
        super(title);
		jlp = new JugglingLabPanel(this, Notation.NOTATION_SITESWAP, null, View.VIEW_EDIT);

		this.uploadscript = uploadscript;
		this.patfile = patfile;
		this.uploadcallback = uc;

        JMenuBar mb = new JMenuBar();
		JMenu filemenu = createFileMenu();
        mb.add(filemenu);
		JMenu notationmenu = jlp.getNotationGUI().createNotationMenu();
		mb.add(notationmenu);
        JMenu viewmenu = jlp.getView().createViewMenu();
		mb.add(viewmenu);
        JMenu helpmenu = jlp.getNotationGUI().createHelpMenu(true /*!PlatformSpecific.getPlatformSpecific().isMacOS()*/);
		if (helpmenu != null)
			mb.add(helpmenu);
        setJMenuBar(mb);

		if (uploadscript == null)
			filemenu.getItem(2).setEnabled(false);			// cannot save to server
		viewmenu.getItem(View.VIEW_EDIT-1).setSelected(true);				// starting in edit view
		if (notationmenu != null)
			notationmenu.getItem(0).setSelected(true);		// siteswap notation

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(jlp);
		pack();
		setResizable(true);
		
		JMLPattern initpat = null;
		if ((uc != null) && (uc.getPattern() != null))
			initpat = (JMLPattern)uc.getPattern().clone();
		jlp.getView().restartView(initpat, new AnimatorPrefs());
        setVisible(true);
    }
	
	
    protected static final String[] fileItems = new String[]
    { "Close", null, "Save to Server"};
    protected static final String[] fileCommands = new String[]
    { "close", null, "savetoserver" };
    protected static final char[] fileShortcuts =
    { 'W', ' ', 'S' };

	public JMenu createFileMenu() {
        JMenu filemenu = new JMenu(guistrings.getString("File"));
        for (int i = 0; i < fileItems.length; i++) {
            if (fileItems[i] == null)
                filemenu.addSeparator();
            else {
				JMenuItem fileitem = new JMenuItem(guistrings.getString(fileItems[i].replace(' ', '_')));
				
                if (fileShortcuts[i] != ' ')
                    fileitem.setAccelerator(KeyStroke.getKeyStroke(fileShortcuts[i],
											Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

                fileitem.setActionCommand(fileCommands[i]);
                fileitem.addActionListener(this);
                filemenu.add(fileitem);
            }
        }
		return filemenu;
	}

    public void actionPerformed(ActionEvent ae) {
        String command = ae.getActionCommand();

        try {
            if (command.equals("close"))
                doMenuCommand(FILE_CLOSE);
            else if (command.equals("savetoserver"))
                doMenuCommand(FILE_SAVETOSERVER);
        } catch (JuggleExceptionInternal jei) {
            if (uploadcallback != null) {
				String message = "Internal error";
				if (jei.getMessage() != null)
					message = "Internal error: " + jei.getMessage();
				uploadcallback.exception = new JuggleExceptionInternal(message);
			}
            jugglinglab.util.ErrorDialog.handleException(jei);
        } catch (Exception e) {
            if (uploadcallback != null) {
				uploadcallback.exception = new JuggleExceptionUser("Error writing pattern to server");
				uploadcallback.repaint();
			}
			String message = "Error writing pattern to server:\n" + e.getClass().getName();
			if (e.getMessage() != null)
				message = message + ": " + e.getMessage();
			new ErrorDialog(this, message);
		}
    }

	public static final int FILE_NONE = 0;
	public static final int FILE_CLOSE = 1;
	public static final int FILE_SAVETOSERVER = 2;
	
	public void doMenuCommand(int action) throws JuggleExceptionUser, JuggleExceptionInternal, IOException {
        switch (action) {

            case FILE_NONE:
                break;

            case FILE_CLOSE:
                dispose();
                break;

            case FILE_SAVETOSERVER:
				JMLPattern pat = jlp.getView().getPattern();
				if (pat != null) {
					FileUploader fu = new FileUploader(new URL(uploadscript));
					String result = fu.upload(patfile, pat.toString());
					// System.out.println(result);
					if (uploadcallback != null)
						uploadcallback.restartJuggle(pat, null);
				}
				dispose();
                break;
		}
	}
	
	
    public synchronized void dispose() {
        super.dispose();
        if (jlp.getView() != null)
            jlp.getView().dispose();
    }
}
