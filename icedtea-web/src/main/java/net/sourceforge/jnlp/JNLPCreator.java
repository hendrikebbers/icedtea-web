/*
 * Copyright 2012 Red Hat, Inc.
 * This file is part of IcedTea, http://icedtea.classpath.org
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package net.sourceforge.jnlp;

import net.sourceforge.jnlp.cache.ResourceTracker;
import net.sourceforge.jnlp.cache.UpdatePolicy;
import net.sourceforge.jnlp.parser.Parser;
import net.sourceforge.jnlp.parser.ParserSettings;
import net.sourceforge.jnlp.runtime.JNLPRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;

import static net.sourceforge.jnlp.runtime.Translator.R;

public class JNLPCreator {
    private final static Logger LOG = LoggerFactory.getLogger(JNLPCreator.class);

    public JNLPFile create(URL location) throws IOException, ParseException {
        return create(location, new ParserSettings());
    }

    public JNLPFile create(URL location, ParserSettings settings) throws IOException, ParseException {
        return create(location, (Version) null, settings);
    }

    public JNLPFile create(URL location, Version version, ParserSettings settings) throws IOException, ParseException {
        return create(location, version, settings, JNLPRuntime.getDefaultUpdatePolicy());
    }

    public JNLPFile create(URL location, Version version, ParserSettings settings, UpdatePolicy policy) throws IOException, ParseException {
        return create(location, version, settings, policy, null);
    }

    public JNLPFile create(URL location, Version version, ParserSettings parserSettings, UpdatePolicy policy, URL forceCodebase) throws IOException, ParseException {
        InputStream input = JNLPCreator.openURL(location, version, policy);

        final JNLPFile jnlpFile = doCreate(input, location, forceCodebase, parserSettings);

        //Downloads the original jnlp file into the cache if possible
        //(i.e. If the jnlp file being launched exist locally, but it
        //originated from a website, then download the one from the website
        //into the cache).
        if (jnlpFile.getSourceLocation() != null && "file".equals(location.getProtocol())) {
            JNLPCreator.openURL(jnlpFile.getSourceLocation(), version, policy);
        }

        jnlpFile.setFileLocation(location);

        jnlpFile.setUniqueKey(Calendar.getInstance().getTimeInMillis() + "-" +
                ((int)(Math.random()*Integer.MAX_VALUE)) + "-" +
                location);

        LOG.error("UNIQUEKEY=" + jnlpFile.getUniqueKey());

        return jnlpFile;
    }

    public JNLPFile create(URL location, String uniqueKey, Version version, ParserSettings settings, UpdatePolicy policy) throws IOException, ParseException {
        final JNLPFile jnlpFile = create(location, version, settings, policy, null);
        jnlpFile.setUniqueKey(uniqueKey);

        LOG.error("UNIQUEKEY (override) =" + jnlpFile.getUniqueKey());

        return jnlpFile;
    }

    public JNLPFile create(InputStream input, ParserSettings settings) throws ParseException {
        return doCreate(input, null, null, settings);
    }

    public JNLPFile create(InputStream input, URL codebase, ParserSettings settings) throws ParseException {
        return doCreate(input, null, codebase, settings);
    }

    private JNLPFile doCreate(InputStream input, URL location, URL forceCodebase, ParserSettings parserSettings)
            throws ParseException {
        try {
            JNLPFile jnlpFile = new JNLPFile();

            Node root = Parser.getRootNode(input, parserSettings);
            Parser parser = new Parser(jnlpFile, location, root, parserSettings, forceCodebase); // true == allow extensions

            jnlpFile.setSpecVersion(parser.getSpecVersion());
            jnlpFile.setFileVersion(parser.getFileVersion());
            jnlpFile.setCodeBase(parser.getCodeBase());
            jnlpFile.setFileLocation(parser.getFileLocation() != null ? parser.getFileLocation() : location);
            jnlpFile.setInfo(parser.getInfo(root));
            jnlpFile.setUpdate(parser.getUpdate(root));
            jnlpFile.setResources(parser.getResources(root, false));
            jnlpFile.setLaunchType(parser.getLauncher(root));
            jnlpFile.setComponent(parser.getComponent(root));
            jnlpFile.setSecurity(parser.getSecurity(root));

            parser.checkForInformation();

            return jnlpFile;

        } catch (ParseException ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.error("ERROR", ex);
            throw new RuntimeException(ex.toString());
        }
    }



    /**
     * Open the jnlp file URL from the cache if there, otherwise
     * download to the cache.
     * Unless file is find in cache, this method blocks until it is downloaded.
     * This is the best way in itw how to download and cache file
     * @param location of resource to open
     * @param version of resource
     * @param policy update policy of resource
     * @return  opened streamfrom given url
     * @throws IOException  if something goes wrong
     */
    public static InputStream openURL(URL location, Version version, UpdatePolicy policy) throws IOException {
        if (location == null || policy == null)
            throw new IllegalArgumentException(R("NullParameter"));

        try {
            ResourceTracker tracker = new ResourceTracker(false); // no prefetch
            tracker.addResource(location, version, null, policy);
            File f = tracker.getCacheFile(location);
            return new FileInputStream(f);
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }
}
