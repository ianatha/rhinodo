/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.rhinodo.impl;

import org.mule.tools.rhinodo.api.NodeModule;
import org.mule.tools.rhinodo.api.NodeModuleFactory;
import org.mule.tools.rhinodo.tools.JarURIHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

public class PrimitiveNodeModuleFactory implements NodeModuleFactory {

    private NodeModuleFactory nodeModuleFactory;
    private List<NodeModuleImpl> nodeModuleList;

    public PrimitiveNodeModuleFactory(URI env, NodeModuleFactory nodeModuleFactory, String destDir) {
        this.nodeModuleFactory = nodeModuleFactory;
        this.nodeModuleList = new ArrayList<NodeModuleImpl>();

        if (env == null) {
            throw new IllegalArgumentException("env cannot be null");
        }

        if ("file".equals(env.getScheme())) {
            addFileModules(env);
        } else if ("jar".equals(env.getScheme())) {
            try {
                new JarURIHelper(env).copyToFolder(destDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            addFileModules(new File(destDir, "META-INF/env").toURI());

        } else {
            throw new IllegalArgumentException(String.format("Error creating PrimitiveNodeModuleFactory: " +
                    "[%s] scheme not recognized.", env.getScheme()));
        }
    }

    private void addFileModules(URI env) {
        String path = env.getPath();
        File file1 = new File(path);
        File[] files = file1.listFiles();
        if ( files == null) {
            throw new IllegalArgumentException();
        }
        for (File file : files) {
            String fileName = file.getName();
            if ( fileName.endsWith(".js") ) {
                String moduleName = fileName.substring(0,fileName.lastIndexOf(".js"));
                nodeModuleList.add(new NodeModuleImpl(moduleName, file.toURI()));
            }
        }
    }



    private void addJarModules(URI env) {
        JarURIHelper jarHelper = new JarURIHelper(env);

        URL jarURL = jarHelper.getJarURL();
        String insideJarRelativePath = jarHelper.getInsideJarRelativePath();
        JarInputStream jarInputStream;
        try {

            jarInputStream = new JarInputStream(jarURL.openStream());

            JarEntry jarEntry = null;
            while( (jarEntry = jarInputStream.getNextJarEntry() ) != null ) {
                if ( jarEntry.getName().startsWith(insideJarRelativePath) ) {
                    if ( jarEntry.getName().endsWith(".js") ) {
                        String moduleName = jarEntry.getName().substring(insideJarRelativePath.length() + 1,jarEntry.getName().lastIndexOf(".js"));
                        nodeModuleList.add(new NodeModuleImpl(moduleName, URI.create("jar:" + jarURL.toString() + "!/" + jarEntry.getName())));
                    }
                }
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public Collection<? extends NodeModule> getModules() {
        ArrayList<NodeModule> nodeModules = new ArrayList<NodeModule>(nodeModuleList);
        nodeModules.addAll(nodeModuleFactory.getModules());
        return nodeModules;
    }
}
