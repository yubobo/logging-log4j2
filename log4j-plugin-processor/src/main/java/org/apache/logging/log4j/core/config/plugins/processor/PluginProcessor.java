/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.logging.log4j.core.config.plugins.processor;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.SimpleElementVisitor6;
import javax.tools.FileObject;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;

import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.StandardLocation.CLASS_OUTPUT;

/**
 * Annotation processor for pre-scanning Log4j 2 plugins.
 */
@SupportedAnnotationTypes(PluginProcessor.PLUGINS_PACKAGE_NAME + ".*")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class PluginProcessor extends AbstractProcessor {

    /**
     * Destination package for saving cache file.
     */
    public static final String PLUGINS_PACKAGE_NAME = "org.apache.logging.log4j.core.config.plugins";

    /**
     * Name of cache file.
     */
    public static final String FILENAME = "Log4j2Plugins.dat";

    private final ConcurrentMap<String, ConcurrentMap<String, PluginEntry>> pluginCategories =
            new ConcurrentHashMap<String, ConcurrentMap<String, PluginEntry>>();
    private final ElementVisitor<PluginEntry, Plugin> pluginVisitor =
            new PluginElementVisitor();
    private final ElementVisitor<Collection<PluginEntry>, Plugin> pluginAliasesVisitor =
            new PluginAliasesElementVisitor();

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        try {
            final Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Plugin.class);
            if (elements.isEmpty()) {
                return false;
            }
            collectPlugins(elements);
            writeCacheFile(elements.toArray(new Element[elements.size()]));
            return true;
        } catch (final IOException e) {
            error(e.getMessage());
            return false;
        }
    }

    private void error(final CharSequence message) {
        processingEnv.getMessager().printMessage(ERROR, message);
    }

    private void collectPlugins(final Iterable<? extends Element> elements) {
        for (final Element element : elements) {
            final Plugin plugin = element.getAnnotation(Plugin.class);
            final PluginEntry entry = element.accept(pluginVisitor, plugin);
            pluginCategories.putIfAbsent(entry.getCategory(), new ConcurrentHashMap<String, PluginEntry>());
            final ConcurrentMap<String, PluginEntry> category = pluginCategories.get(entry.getCategory());
            category.put(entry.getKey(), entry);
            final Collection<PluginEntry> entries = element.accept(pluginAliasesVisitor, plugin);
            for (final PluginEntry pluginEntry : entries) {
                category.put(pluginEntry.getKey(), pluginEntry);
            }
        }
    }

    private void writeCacheFile(final Element... elements) throws IOException {
        final FileObject fo = processingEnv.getFiler().createResource(CLASS_OUTPUT, PLUGINS_PACKAGE_NAME, FILENAME, elements);
        final DataOutputStream out = new DataOutputStream(new BufferedOutputStream(fo.openOutputStream()));
        try {
            out.writeInt(pluginCategories.size());
            for (final Map.Entry<String, ConcurrentMap<String, PluginEntry>> category : pluginCategories.entrySet()) {
                out.writeUTF(category.getKey());
                final Map<String, PluginEntry> m = category.getValue();
                out.writeInt(m.size());
                for (final Map.Entry<String, PluginEntry> entry : m.entrySet()) {
                    final PluginEntry plugin = entry.getValue();
                    out.writeUTF(plugin.getKey());
                    out.writeUTF(plugin.getClassName());
                    out.writeUTF(plugin.getName());
                    out.writeBoolean(plugin.isPrintable());
                    out.writeBoolean(plugin.isDefer());
                }
            }
        } finally {
            out.close();
        }
    }

    /**
     * ElementVisitor to scan the Plugin annotation.
     */
    private static class PluginElementVisitor extends SimpleElementVisitor6<PluginEntry, Plugin> {
        @Override
        public PluginEntry visitType(final TypeElement e, final Plugin plugin) {
            if (plugin == null) {
                throw new NullPointerException("Plugin annotation is null.");
            }
            final PluginEntry entry = new PluginEntry();
            entry.setKey(plugin.name().toLowerCase());
            entry.setClassName(e.getQualifiedName().toString());
            entry.setName(Plugin.EMPTY.equals(plugin.elementType()) ? plugin.name() : plugin.elementType());
            entry.setPrintable(plugin.printObject());
            entry.setDefer(plugin.deferChildren());
            entry.setCategory(plugin.category());
            return entry;
        }
    }

    /**
     * ElementVisitor to scan the PluginAliases annotation.
     */
    private static class PluginAliasesElementVisitor extends SimpleElementVisitor6<Collection<PluginEntry>, Plugin> {
        protected PluginAliasesElementVisitor() {
            super(Collections.<PluginEntry>emptyList());
        }

        @Override
        public Collection<PluginEntry> visitType(final TypeElement e, final Plugin plugin) {
            final PluginAliases aliases = e.getAnnotation(PluginAliases.class);
            if (aliases == null) {
                return DEFAULT_VALUE;
            }
            final Collection<PluginEntry> entries = new ArrayList<PluginEntry>(aliases.value().length);
            for (final String alias : aliases.value()) {
                final PluginEntry entry = new PluginEntry();
                entry.setKey(alias.toLowerCase());
                entry.setClassName(e.getQualifiedName().toString());
                entry.setName(Plugin.EMPTY.equals(plugin.elementType()) ? alias : plugin.elementType());
                entry.setPrintable(plugin.printObject());
                entry.setDefer(plugin.deferChildren());
                entry.setCategory(plugin.category());
                entries.add(entry);
            }
            return entries;
        }
    }
}
