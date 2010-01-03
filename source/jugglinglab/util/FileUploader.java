// FileUploader.java
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

// Note:  Most of this code is derived from HTTPClient V0.3-3, copyright
// (C) 1996-2001 Ronald Tschalär and released under the GNU LGPL.  Many thanks
// to Ronald.

package jugglinglab.util;

import java.io.*;
import java.net.*;
import java.util.*;


public class FileUploader {
	private URL u;
	
    private final static String ContDisp = "\r\nContent-Disposition: form-data; name=\"";
    private final static String FileName = "\"; filename=\"";
    private final static String ContType = "\r\nContent-Type: ";
    private final static String Boundary = "\r\n----------ieoau._._+2_8_GoodLuck8.3-dskdfJwSJKl234324jfLdsjfdAuaoei-----";
	private static BitSet  BoundChar;

    // Class Initializer
    static
    {
		// rfc-2046 & rfc-2045: (bcharsnospace & token)
		// used for multipart codings
		BoundChar = new BitSet(256);
		for (int ch='0'; ch <= '9'; ch++)  BoundChar.set(ch);
		for (int ch='A'; ch <= 'Z'; ch++)  BoundChar.set(ch);
		for (int ch='a'; ch <= 'z'; ch++)  BoundChar.set(ch);
		BoundChar.set('+');
		BoundChar.set('_');
		BoundChar.set('-');
		BoundChar.set('.');
    }


    
	public FileUploader(URL url) {
		u = url;
	}
	
	
	public String upload(String filename, InputStream in, int flength) throws IOException {
		HttpURLConnection huc = (HttpURLConnection)u.openConnection();
		huc.setDoInput(true);
		huc.setDoOutput(true);
		huc.setUseCaches(false);
		huc.setRequestMethod("POST");

		NVPair[] opts = { new NVPair("note", "none") };
		// NVPair[] file = { new NVPair("upfile", filename) };
		NVPair[] hdrs = new NVPair[1];
		byte[] data = mpFormDataEncode(opts, filename, in, flength, hdrs);
		
		huc.setRequestProperty(hdrs[0].getName(), hdrs[0].getValue());

		huc.connect();
		DataOutputStream dos = new DataOutputStream(huc.getOutputStream());
		dos.write(data);
		dos.flush();
		dos.close();
		
		InputStream resp = huc.getInputStream();
		StringBuffer sb = new StringBuffer();
		int c;
		while ((c = resp.read()) != -1)
			sb.append((char)c);
		resp.close();
		huc.disconnect();
		return sb.toString();
	}

	public String upload(String filename, String contents) throws IOException {
		byte[] b = contents.getBytes("ISO-8859-1");
		return upload(filename, new ByteArrayInputStream(b), b.length);
	}
	
	public String upload(String filename) throws IOException {
		File f = new File(filename);
		return upload(filename, new FileInputStream(f), (int)f.length());
	}
	

    protected NVPair[] dummy = new NVPair[0];

    protected byte[] mpFormDataEncode(NVPair[] opts, String fname, InputStream in, int flength, NVPair[] ct_hdr) throws IOException
    {
		byte[] boundary  = Boundary.getBytes("8859_1"),
			   cont_disp = ContDisp.getBytes("8859_1"),
			   cont_type = ContType.getBytes("8859_1"),
			   filename  = FileName.getBytes("8859_1");
		int len = 0,
			hdr_len = boundary.length + cont_disp.length+1 + 2 +  2;
			//        \r\n --  bnd      \r\n C-D: ..; n=".." \r\n \r\n

		if (opts == null)   opts  = dummy;
//		if (files == null)  files = dummy;

		String upname = "upfile";
		
		// Calculate the length of the data

		for (int idx=0; idx<opts.length; idx++)
		{
			if (opts[idx] == null)  continue;

			len += hdr_len + opts[idx].getName().length() +
			   opts[idx].getValue().length();
		}

//		for (int idx=0; idx<files.length; idx++)
//		{
//			if (files[idx] == null)  continue;

//			File file = new File(files[idx].getValue());
//			String fname = file.getName();
//			if (fname != null)
//			{
			len += hdr_len + upname.length() + filename.length;
			len += fname.length() + flength;

			String ct = CT.getContentType(fname);
			if (ct != null)
				len += cont_type.length + ct.length();
//			}
//		}

		if (len == 0)
		{
			ct_hdr[0] = new NVPair("Content-Type", "application/octet-stream");
			return new byte[0];
		}

		len -= 2;			// first CR LF is not written
		len += boundary.length + 2 + 2;	// \r\n -- bnd -- \r\n


		// Now fill array

		byte[] res = new byte[len];
		int    pos = 0;

		NewBound: for (int new_c=0x30303030; new_c!=0x7A7A7A7A; new_c++)
		{
			pos = 0;

			// modify boundary in hopes that it will be unique
			while (!BoundChar.get(new_c     & 0xff)) new_c += 0x00000001;
			while (!BoundChar.get(new_c>>8  & 0xff)) new_c += 0x00000100;
			while (!BoundChar.get(new_c>>16 & 0xff)) new_c += 0x00010000;
			while (!BoundChar.get(new_c>>24 & 0xff)) new_c += 0x01000000;
			boundary[40] = (byte) (new_c     & 0xff);
			boundary[42] = (byte) (new_c>>8  & 0xff);
			boundary[44] = (byte) (new_c>>16 & 0xff);
			boundary[46] = (byte) (new_c>>24 & 0xff);

			int off = 2;
			int[] bnd_cmp = compile_search(boundary);

			for (int idx=0; idx<opts.length; idx++)
			{
			if (opts[idx] == null)  continue;

			System.arraycopy(boundary, off, res, pos, boundary.length-off);
			pos += boundary.length - off;
			off  = 0;
			int  start = pos;

			System.arraycopy(cont_disp, 0, res, pos, cont_disp.length);
			pos += cont_disp.length;

			int nlen = opts[idx].getName().length();
			System.arraycopy(opts[idx].getName().getBytes("8859_1"), 0, res, pos, nlen);
			pos += nlen;

			res[pos++] = (byte) '"';
			res[pos++] = (byte) '\r';
			res[pos++] = (byte) '\n';
			res[pos++] = (byte) '\r';
			res[pos++] = (byte) '\n';

			int vlen = opts[idx].getValue().length();
			System.arraycopy(opts[idx].getValue().getBytes("8859_1"), 0, res, pos, vlen);
			pos += vlen;

			if ((pos-start) >= boundary.length  &&
				findStr(boundary, bnd_cmp, res, start, pos) != -1)
				continue NewBound;
			}

//			for (int idx=0; idx<files.length; idx++)
//			{
//			if (files[idx] == null)  continue;

//			File file = new File(files[idx].getValue());
//			String fname = file.getName();
//			if (fname == null)  continue;

			System.arraycopy(boundary, off, res, pos, boundary.length-off);
			pos += boundary.length - off;
			off  = 0;
			int start = pos;

			System.arraycopy(cont_disp, 0, res, pos, cont_disp.length);
			pos += cont_disp.length;


			int nlen = upname.length();
			System.arraycopy(upname.getBytes("8859_1"), 0, res, pos, nlen);
			pos += nlen;

			System.arraycopy(filename, 0, res, pos, filename.length);
			pos += filename.length;

			nlen = fname.length();
			System.arraycopy(fname.getBytes("8859_1"), 0, res, pos, nlen);
			pos += nlen;

			res[pos++] = (byte) '"';

			String ct2 = CT.getContentType(fname);
			if (ct2 != null)
			{
				System.arraycopy(cont_type, 0, res, pos, cont_type.length);
				pos += cont_type.length;
				System.arraycopy(ct2.getBytes("8859_1"), 0, res, pos, ct2.length());
				pos += ct2.length();
			}

			res[pos++] = (byte) '\r';
			res[pos++] = (byte) '\n';
			res[pos++] = (byte) '\r';
			res[pos++] = (byte) '\n';

			nlen = (int) flength;
//			FileInputStream fin = new FileInputStream(file);
			while (nlen > 0)
			{
				int got = in.read(res, pos, nlen);
				nlen -= got;
				pos += got;
			}
			in.close();

			if ((pos-start) >= boundary.length  &&
				findStr(boundary, bnd_cmp, res, start, pos) != -1)
				continue NewBound;
//			}

			break NewBound;
		}

		System.arraycopy(boundary, 0, res, pos, boundary.length);
		pos += boundary.length;
		res[pos++] = (byte) '-';
		res[pos++] = (byte) '-';
		res[pos++] = (byte) '\r';
		res[pos++] = (byte) '\n';

		if (pos != len)
			throw new Error("Calculated "+len+" bytes but wrote "+pos+" bytes!");

		/* the boundary parameter should be quoted (rfc-2046, section 5.1.1)
		 * but too many script authors are not capable of reading specs...
		 * So, I give up and don't quote it.
		 */
		ct_hdr[0] = new NVPair("Content-Type",
					   "multipart/form-data; boundary=" +
					   new String(boundary, 4, boundary.length-4, "8859_1"));

		return res;
    }


    protected final static int[] compile_search(byte[] search) {
		int[] cmp = {0, 1, 0, 1, 0, 1};
		int   end;

		for (int idx=0; idx<search.length; idx++)
		{
			for (end=idx+1; end<search.length; end++)
			{
			if (search[idx] == search[end])  break;
			}
			if (end < search.length)
			{
			if ((end-idx) > cmp[1])
			{
				cmp[4] = cmp[2];
				cmp[5] = cmp[3];
				cmp[2] = cmp[0];
				cmp[3] = cmp[1];
				cmp[0] = idx;
				cmp[1] = end - idx;
			}
			else if ((end-idx) > cmp[3])
			{
				cmp[4] = cmp[2];
				cmp[5] = cmp[3];
				cmp[2] = idx;
				cmp[3] = end - idx;
			}
			else if ((end-idx) > cmp[3])
			{
				cmp[4] = idx;
				cmp[5] = end - idx;
			}
			}
		}

		cmp[1] += cmp[0];
		cmp[3] += cmp[2];
		cmp[5] += cmp[4];
		return cmp;
    }

    /**
     * Search for a string. Use compile_search() to first generate the second
     * argument. This uses a Knuth-Morris-Pratt like algorithm.
     *
     * @param search  the string to search for.
     * @param cmp     the the array returned by compile_search.
     * @param str     the string in which to look for <var>search</var>.
     * @param beg     the position at which to start the search in
     *                <var>str</var>.
     * @param end     the position at which to end the search in <var>str</var>,
     *                noninclusive.
     * @return the position in <var>str</var> where <var>search</var> was
     *         found, or -1 if not found.
     */
    protected final static int findStr(byte[] search, int[] cmp, byte[] str, int beg, int end) {
		int c1f  = cmp[0],
			c1l  = cmp[1],
			d1   = c1l - c1f,
			c2f  = cmp[2],
			c2l  = cmp[3],
			d2   = c2l - c2f,
			c3f  = cmp[4],
			c3l  = cmp[5],
			d3   = c3l - c3f;

		Find: while (beg+search.length <= end)
		{
			if (search[c1l] == str[beg+c1l])
			{
			/* This is correct, but Visual J++ can't cope with it...
			Comp: if (search[c1f] == str[beg+c1f])
			{
				for (int idx=0; idx<search.length; idx++)
				if (search[idx] != str[beg+idx])  break Comp;

				break Find;		// we found it
			}
			*  so here is the replacement: */
			if (search[c1f] == str[beg+c1f])
			{
				boolean same = true;

				for (int idx=0; idx<search.length; idx++)
				if (search[idx] != str[beg+idx])
				{
					same = false;
					break;
				}

				if (same)
				break Find;         // we found it
			}

			beg += d1;
			}
			else if (search[c2l] == str[beg+c2l])
			beg += d2;
			else if (search[c3l] == str[beg+c3l])
			beg += d3;
			else
			beg++;
		}

		if (beg+search.length > end)
			return -1;
		else
			return beg;
    }

    private static class CT extends URLConnection {
		private CT() { super(null); }

		protected static final String getContentType(String fname) {
			return guessContentTypeFromName(fname);
		}

		public void connect() { }
    }


	private final class NVPair {
		private String name;
		private String value;

		public NVPair(String name, String value) {
			this.name  = name;
			this.value = value;
		}

		public NVPair(NVPair p) {
			this(p.name, p.value);
		}

		public final String getName() { return name; }

		public final String getValue() { return value; }

		public String toString() {
			return getClass().getName() + "[name=" + name + ",value=" + value + "]";
		}
	}

}
