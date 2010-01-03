// JugglingLabAWT.java
//
// Copyright 2004 by Jack Boyce (jboyce@users.sourceforge.net) and others

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


import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import java.util.*;
import java.io.*;
import java.net.*;
import org.xml.sax.*;

import jugglinglab.core.*;
import jugglinglab.jml.*;
import jugglinglab.notation.*;
import jugglinglab.util.*;


public class JugglingLabAWT extends Applet {
    static ResourceBundle guistrings;
    static ResourceBundle errorstrings;
    static {
        guistrings = ResourceBundle.getBundle("GUIStrings");
        errorstrings = ResourceBundle.getBundle("ErrorStrings");
    }

	protected Animator ja = null;
	protected AnimatorPrefs jc = null;
	
	
    public JugglingLabAWT() {
        setBackground(Color.white);
		
		// For some reason initAudioClips() causes an InstantiationException that
		// cannot be caught, on Win2k/IE4.0/MS JVM
		/*
        try {
			initAudioClips();
		} catch (Exception e) {
			// Doesn't work on Win2K/IE4.0/MS JVM
			// No problem if they don't load; there just won't be sounds
		}
		*/
    }

    // applet version starts here
    public void init() {
        String uploadscript = getParameter("uploadscript");
        String jmldir = getParameter("jmldir");
        String jmlfile = getParameter("jmlfile");
        String prefs = getParameter("animprefs");
		
		try {
			jc = new AnimatorPrefs();
			if (prefs != null)
				jc.parseInput(prefs);

			JMLPattern pat = null;
			boolean readerror = false;
			
			if (jmlfile != null) {
				if (!jmlfile.endsWith(".jml"))
					throw new JuggleExceptionUser(errorstrings.getString("Error_JML_extension"));
				if (jmlfile.indexOf(".") != (jmlfile.length()-4))
					throw new JuggleExceptionUser(errorstrings.getString("Error_JML_filename"));
				
				try {
					URL jmlfileURL = null;
					if (jmldir != null)
						jmlfileURL = new URL(new URL(jmldir), jmlfile);
					else
						jmlfileURL = new URL(getDocumentBase(), jmlfile);

					JMLParser parse = new JMLParser();
					parse.parse(new InputStreamReader(jmlfileURL.openStream()));
					if (parse.getFileType() == JMLParser.JML_PATTERN)
						pat = new JMLPattern(parse.getTree());
					else
						throw new JuggleExceptionUser(errorstrings.getString("Error_JML_is_pattern_list"));
				} catch (MalformedURLException mue) {
					throw new JuggleExceptionUser(errorstrings.getString("Error_URL_syntax")+" '"+jmldir+"'");
				} catch (IOException ioe) {
					readerror = true;
					// System.out.println(ioe.getMessage());
					// throw new JuggleExceptionUser(errorstrings.getString("Error_reading_pattern"));
				} catch (SAXException se) {
					throw new JuggleExceptionUser(errorstrings.getString("Error_parsing_pattern"));
				}
			}

			if (pat == null) {
				String notation = getParameter("notation");
				String pattern = getParameter("pattern");
				if ((notation != null) && (pattern != null)) {
					Notation not = null;
					if (notation.equalsIgnoreCase("jml")) {
						try {
							pat = new JMLPattern(new StringReader(pattern));
						} catch (IOException ioe) {
							throw new JuggleExceptionUser(errorstrings.getString("Error_reading_JML"));
						} catch (SAXException se) {
							throw new JuggleExceptionUser(errorstrings.getString("Error_parsing_JML"));
						}
					} else {
						not = Notation.getNotation(notation);
						pat = not.getJMLPattern(pattern);
					}
				}
			}

			if (uploadscript != null) {
				if (jmlfile == null)
					throw new JuggleExceptionUser(errorstrings.getString("Error_no_JML_specified"));

				GridBagLayout gb = new GridBagLayout();
				setLayout(gb);
			
				ja = new Animator();
				add(ja);
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.anchor = GridBagConstraints.WEST;
				gbc.fill = GridBagConstraints.BOTH;
				gbc.gridwidth = gbc.gridheight = 1;
				gbc.gridx = 0;
				gbc.gridy = 0;
				gbc.insets = new Insets(0,0,0,0);
				gbc.weightx = 1.0;
				gbc.weighty = 1.0;
				gb.setConstraints(ja, gbc);

				Button but = new Button(guistrings.getString("Edit"));
				but.setBackground(Color.white);
				add(but);
				gbc = new GridBagConstraints();
				gbc.anchor = GridBagConstraints.EAST;
				gbc.fill = GridBagConstraints.NONE;
				gbc.gridwidth = gbc.gridheight = 1;
				gbc.gridx = 0;
				gbc.gridy = 1;
				gbc.insets = new Insets(0,0,0,0);
				gbc.weightx = 1.0;
				gbc.weighty = 0.0;
				gb.setConstraints(but, gbc);
							
				validate();
				
				if (pat != null)
					ja.restartJuggle(pat, jc);
				else if (readerror)
					ja.exception = new JuggleExceptionUser(guistrings.getString("Click_Edit"));		
		
				final String uploadscriptf = uploadscript;
				final String jmlfilef = jmlfile;
				final Animator jaf = ja;
				but.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						try {
							// Use reflection API since JugglingLabWindow isn't AWT-only
							Class jlwc = Class.forName("jugglinglab.core.JugglingLabWindow");
							Constructor con = jlwc.getDeclaredConstructor(new Class[] {String.class, String.class, String.class, Animator.class});
							String title = "Editing " + jmlfilef;
							con.newInstance(new Object[] {title, uploadscriptf, jmlfilef, jaf});
							// new JugglingLabWindow("Editing "+jmlfilef, uploadscriptf, jmlfilef, jaf);
						} catch (Exception e) {
							if (e instanceof JuggleExceptionUser)
								jaf.exception = (JuggleExceptionUser)e;
							else if (e instanceof JuggleExceptionInternal)
								jaf.exception = new JuggleExceptionInternal("Internal error: " + e.getMessage());
							else
								jaf.exception = new JuggleExceptionUser(errorstrings.getString("Error_could_not_launch_editor"));
							jaf.repaint();   // force redraw to display message
						}
					}
				});
			} else {
				if (readerror)
					throw new JuggleExceptionUser(errorstrings.getString("Error_reading_pattern"));
				if (pat == null)
					throw new JuggleExceptionUser(errorstrings.getString("Error_no_pattern_specified"));

				GridBagLayout gb = new GridBagLayout();
				setLayout(gb);
				
				ja = new Animator();
				add(ja);
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.anchor = GridBagConstraints.WEST;
				gbc.fill = GridBagConstraints.BOTH;
				gbc.gridwidth = gbc.gridheight = 1;
				gbc.gridx = 0;
				gbc.gridy = 0;
				gbc.insets = new Insets(0,0,0,0);
				gbc.weightx = 1.0;
				gbc.weighty = 1.0;
				gb.setConstraints(ja, gbc);

				validate();
				
				// NOTE: animprefs will only be applied when some kind of pattern is defined
				if (pat != null)
					ja.restartJuggle(pat, jc);
			}
		} catch (Exception e) {
			String message = "";
			if (e instanceof JuggleExceptionUser)
				message = e.getMessage();
			else {
				if (e instanceof JuggleExceptionInternal)
					message = "Internal error";
				else
					message = e.getClass().getName();
				if (e.getMessage() != null)
					message = message + ": " + e.getMessage();
			}
			
			GridBagLayout gb = new GridBagLayout();
			setLayout(gb);
		
			Label lab = new Label(message);
			lab.setAlignment(Label.CENTER);
			add(lab);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridwidth = gbc.gridheight = 1;
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.insets = new Insets(0,0,0,0);
			gbc.weightx = 1.0;
			gbc.weighty = 1.0;
			gb.setConstraints(lab, gbc);

			validate();
			
			if (!(e instanceof JuggleExceptionUser))
				e.printStackTrace();
		}
    }

    public void start() {
        if ((ja != null) && (ja.message == null) && !jc.mousePause)
            ja.setPaused(false);
    }

    public void stop() {
        if ((ja != null) && (ja.message == null) && !jc.mousePause)
            ja.setPaused(true);
    }

    public void destroy() {
        if (ja != null)
			ja.dispose();
    }

    public void update(Graphics g) {
        // Override default version that clears applet area
		if (ja != null)
			ja.paint(g);
    }

    // It's easiest to load the audioclips here and pass them along to Animator.
    // For some reason AudioClip is part of the Applet package.
    protected void initAudioClips() {
        AudioClip[] clips = new AudioClip[2];
        URL catchurl = JugglingLabAWT.class.getResource("/resources/catch.au");
		if (catchurl != null)
			clips[0] = newAudioClip(catchurl);
        URL bounceurl = JugglingLabAWT.class.getResource("/resources/bounce.au");
		if (bounceurl != null)
			clips[1] = newAudioClip(bounceurl);
        Animator.setAudioClips(clips);
    }
}
