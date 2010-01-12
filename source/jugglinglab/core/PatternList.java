// PatternList.java
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

package jugglinglab.core;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import jugglinglab.jml.*;
import jugglinglab.notation.*;
import jugglinglab.util.*;
import jugglinglab.view.*;


public class PatternList extends JPanel {
    static ResourceBundle guistrings;
    static ResourceBundle errorstrings;
    static {
        guistrings = ResourceBundle.getBundle("GUIStrings");
        errorstrings = ResourceBundle.getBundle("ErrorStrings");
    }

	View animtarget = null;
    String title = null;
    JList list = null;
    Vector patterns = null;
    DefaultListModel model = null;
    // JLabel status = null;


	public PatternList() {
		makePanel();
	}
	
    public PatternList(View target) {
        makePanel();
		setTargetView(target);
    }

    protected void makePanel() {
        /*
         this.status = new JLabel("");
         this.add(status);
        	GridBagConstraints gbc = new GridBagConstraints();
         gbc.anchor = GridBagConstraints.WEST;
         gbc.fill = GridBagConstraints.HORIZONTAL;
         gbc.gridwidth = gbc.gridheight = 1;
         gbc.gridx = 0;
         gbc.gridy = 0;
         gbc.insets = new Insets(5,5,5,5);
         gbc.weightx = 1.0;
         gbc.weighty = 0.0;
         gb.setConstraints(status, gbc);
         */
        patterns = new Vector();
        model = new DefaultListModel();
        list = new JList(model);
        list.setFont(Font.getFont("Monospaced"));
        list.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent lse) {
                PatternWindow jaw2 = null;
                try {
                    if (lse.getValueIsAdjusting()) {
                        PatternRecord rec = (PatternRecord)patterns.elementAt(list.getSelectedIndex());

                        JMLPattern pat = null;

                        if ((rec.notation != null) && rec.notation.equalsIgnoreCase("JML") && (rec.anim != null)) {
                            JMLParser p = new JMLParser();
                            p.parse(new StringReader(rec.anim));
                            pat = new JMLPattern(p.getTree(), PatternList.this.loadingversion);
                        } else if ((rec.notation != null) && (rec.anim != null)) {
                            Notation not = Notation.getNotation(rec.notation);
                            pat = not.getJMLPattern(rec.anim);
                        } else
                            return;

                        if (pat != null) {
                            pat.setTitle(rec.display);

                            AnimatorPrefs ap = new AnimatorPrefs();
                            if (rec.animprefs != null)
                                ap.parseInput(rec.animprefs);

                            if (animtarget != null)
                                animtarget.restartView(pat, ap);
                            else
                                jaw2 = new PatternWindow(pat.getTitle(), pat, ap);
                        }
                    }
                } catch (JuggleExceptionUser je) {
                    if (jaw2 != null)
                        jaw2.dispose();
                    new ErrorDialog(PatternList.this, errorstrings.getString("Error_creating_pattern")+": "+
                                    je.getMessage());
                } catch (Exception e) {
                    if (jaw2 != null)
                        jaw2.dispose();
                    jugglinglab.util.ErrorDialog.handleException(e);
                }
            }
        });

        JScrollPane pane = new JScrollPane(list);

        this.setLayout(new BorderLayout());
        this.add(pane, BorderLayout.CENTER);
    }

	public void setTargetView(View target) {
		animtarget = target;
	}
	
    public void addPattern(String display, String animprefs, String notation, String anim) {
        // display = display.trim();
        if (notation != null)
            notation = notation.trim();
        if (animprefs != null)
            animprefs = animprefs.trim();
        if (anim != null)
            anim = anim.trim();

        PatternRecord rec = new PatternRecord(display, animprefs, notation, anim);

        patterns.addElement(rec);
        model.addElement(display);
    }

    public void clearList() {
        patterns.clear();
        model.clear();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return this.title;
    }

    String loadingversion = null;

    public void readJML(JMLNode root) throws JuggleExceptionUser {
        if (!root.getNodeType().equalsIgnoreCase("jml"))
            throw new JuggleExceptionUser(errorstrings.getString("Error_missing_JML_tag"));
        loadingversion = root.getAttributes().getAttribute("version");
        if (loadingversion == null)
            loadingversion = "1.0";

        JMLNode listnode = root.getChildNode(0);
        if (!listnode.getNodeType().equalsIgnoreCase("patternlist"))
            throw new JuggleExceptionUser(errorstrings.getString("Error_missing_patternlist_tag"));

        for (int i = 0; i < listnode.getNumberOfChildren(); i++) {
            JMLNode child = listnode.getChildNode(i);
            if (child.getNodeType().equalsIgnoreCase("title")) {
                title = child.getNodeValue().trim();
            } else if (child.getNodeType().equalsIgnoreCase("line")) {
                JMLAttributes attr = child.getAttributes();

                String display = attr.getAttribute("display");
                if ((display==null) || display.equals(""))
                    display = " ";		// JList won't display empty strings
                String animprefs = attr.getAttribute("animprefs");
                String notation = attr.getAttribute("notation");
                String pattern = attr.getAttribute("pattern");

                addPattern(display, animprefs, notation, pattern);
            } else
                throw new JuggleExceptionUser(errorstrings.getString("Error_illegal_tag"));
        }
    }

    public void writeJML(Writer wr) throws IOException {
        PrintWriter write = new PrintWriter(wr);
        for (int i = 0; i < JMLDefs.jmlprefix.length; i++)
            write.println(JMLDefs.jmlprefix[i]);
        String vers = this.loadingversion;
        if (vers == null)
            vers = JMLDefs.jmlversion;
        write.println("<jml version=\""+vers+"\">");
        write.println("<patternlist>");
        write.println("<title>"+this.title+"</title>");

        for (int i = 0; i < patterns.size(); i++) {
            PatternRecord rec = (PatternRecord)patterns.elementAt(i);
            String line = "<line display=\"" + rec.display + "\"";

            if (rec.animprefs != null)
                line += " animprefs=\"" + rec.animprefs + "\"";
            if (rec.notation != null)
                line += " notation=\"" + rec.notation.toLowerCase() + "\"";
            if ((rec.notation != null) && rec.notation.equalsIgnoreCase("JML")) {
                line += " pattern=\'";
                write.println(line);
                write.println(rec.anim);
                write.println("\'/>");
            } else if (rec.anim != null) {
                line += " pattern=\"" + rec.anim + "\"/>";
                write.println(line);
            } else {
                line += "/>";
                write.println(line);
            }
        }
        write.println("</patternlist>");
        write.println("</jml>");
        for (int i = 0; i < JMLDefs.jmlsuffix.length; i++)
            write.println(JMLDefs.jmlsuffix[i]);
        write.flush();
        //		write.close();
    }
    
    public void writeJMLPatternList(Writer wr) throws IOException {
        PrintWriter write = new PrintWriter(wr);
        for (int i = 0; i < JMLDefs.jmlprefix.length; i++)
            write.println(JMLDefs.jmlprefix[i]);
        String vers = this.loadingversion;
        if (vers == null)
            vers = JMLDefs.jmlversion;
        write.println("<jml version=\""+vers+"\">");
        write.println("<patternlist>");
        write.println("<title>"+this.title+"</title>");

        for (int i = 0; i < patterns.size(); i++) {
            PatternRecord rec = (PatternRecord)patterns.elementAt(i);
            String line = "<line display=\"" + rec.display + "\"";

            if (rec.animprefs != null)
                line += " animprefs=\"" + rec.animprefs + "\"";
            if (rec.notation != null)
                line += " notation=\"" + rec.notation.toLowerCase() + "\"";
            if ((rec.notation != null) && rec.notation.equalsIgnoreCase("JML")) {
                line += " pattern=\'";
                write.println(line);
                write.println(rec.anim);
                write.println("\'/>");
            } else if (rec.anim != null) {
                line += " pattern=\"" + rec.anim + "\"/>";
                write.println(line);
            } else {
                line += "/>";
                write.println(line);
            }
        }
        write.println("</patternlist>");
        write.println("</jml>");
        for (int i = 0; i < JMLDefs.jmlsuffix.length; i++)
            write.println(JMLDefs.jmlsuffix[i]);
        write.flush();    	
    }

    public void writeText(Writer wr) throws IOException {
        PrintWriter write = new PrintWriter(wr);

        for (int i = 0; i < patterns.size(); i++) {
            PatternRecord rec = (PatternRecord)patterns.elementAt(i);
            write.println(rec.display);
        }
        write.flush();
        //		write.close();
    }


    class PatternRecord {
        public String display;
        public String animprefs;
        public String notation;
        public String anim;

        public PatternRecord(String dis, String ap, String not, String ani) {
            this.display = dis;
            this.animprefs = ap;
            this.notation = not;
            this.anim = ani;
        }
    }
}
