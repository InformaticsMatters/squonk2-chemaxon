/*
 *  Copyright (c) 2023  Informatics Matters Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package squonk.jobs.chemaxon.util;


import chemaxon.formats.MolExporter;
import chemaxon.formats.MolImporter;
import chemaxon.struc.Molecule;
import chemaxon.util.MolHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

public class MoleculeUtils {

    private static final Logger LOG = Logger.getLogger(MoleculeUtils.class.getName());
    private static final DMLogger DMLOG = new DMLogger();

    public static Molecule createRepresentation(Molecule mol, MoleculeObject.Representation key) {

        switch (key) {
            case Original:
                return mol.clone();
            case ExplicitH:
                return moleculeWithExplicitHydrogens(mol);
            case ImplicitH:
                return moleculeWithImplicitHydrogens(mol);
            default:
                throw new UnsupportedOperationException("Can't handle representation " + key);
        }
    }

    public static Molecule moleculeWithExplicitHydrogens(Molecule mol) {
        MolHandler h = new MolHandler(mol.clone());
        h.addHydrogens();
        return h.getMolecule();
    }

    public static Molecule moleculeWithImplicitHydrogens(Molecule mol) {
        MolHandler h = new MolHandler(mol.clone());
        h.removeHydrogens();
        return h.getMolecule();
    }

    public static Stream<MoleculeObject> readMoleculesAsStream(String path) throws IOException {
        return readMoleculesAsStream(path, null);
    }

    /** Generate a Stream of MoleculeObjects.
     * The logic is quite complex so that it can handle bad molecules gracefully.
     * If a bad molecule is encountered the MoleculeObject returned from the Stream is null.
     *
     * @param path
     * @return
     * @throws IOException
     */
    public static Stream<MoleculeObject> readMoleculesAsStream(String path, String opts) throws IOException {
        File file = new File(path);
        MolImporter importer = new MolImporter(file, opts);
        final AtomicInteger count = new AtomicInteger(0);

        Iterator<MoleculeObject> iter = new Iterator<>() {

            Molecule currentMol = null;
            final Molecule errorMol = new Molecule();

            private Molecule readNextMol() {
                count.incrementAndGet();
                try {
                    return importer.read();
                } catch (IOException ioe) {
                    String msg = "Failed to read molecule " + count.get();
                    DMLOG.logEvent(DMLogger.Level.WARNING, msg + " " + ioe.getMessage());
                    LOG.log(Level.WARNING, msg, ioe);
                    return errorMol;
                }
            }

            @Override
            public boolean hasNext() {
                if (currentMol != null) {
                    return true;
                } else {
                    currentMol = readNextMol();
                    return currentMol != null;
                }
            }

            @Override
            public MoleculeObject next() {
                Molecule theMol = null;
                if (currentMol != null) {
                    theMol = currentMol;
                    currentMol = null;
                } else {
                    theMol = readNextMol();
                }

                if (theMol == null) {
                    return null;
                } else if (theMol == errorMol) {
                    return null;
                } else {
                    return new MoleculeObject(theMol);
                }
            }
        };

        Spliterator<MoleculeObject> spliterator = Spliterators.spliteratorUnknownSize(iter, Spliterator.IMMUTABLE);
        Stream<MoleculeObject> mols = StreamSupport.stream(spliterator, false);

        return mols;
    }

    public static Stream<MoleculeObject> addFileWriter(Stream<MoleculeObject> mols, String outputFile, boolean includeHeader)
            throws IOException {
        Path path = Paths.get(outputFile);
        Path dir = path.getParent();
        if (dir != null) {
            Files.createDirectories(dir);
        }

        String opts = null;
        if (outputFile.endsWith(".sdf")) {
            opts = "sdf";
        } else if (outputFile.endsWith(".smi")) {
            if (includeHeader) {
                opts = "smiles:T*";
            } else {
                opts = "smiles:-T*";
            }
        } else {
            throw new IllegalArgumentException("Unsupported output file format " + outputFile);
        }
        // In principle this should work for gzipping, but you end up with empty files. ChemAxon bug?
//        if (outputFile.endsWith(".gz")) {
//            opts = "gzip:" + opts;
//        }

        OutputStream out = new FileOutputStream(outputFile);

        return addFileWriter(mols, out, opts, includeHeader);
    }

    public static Stream<MoleculeObject> addFileWriter(
            Stream<MoleculeObject> mols, OutputStream output, String options, boolean includeHeader)
            throws IOException {

        LOG.info("Options: " + options);
        final MolExporter exporter = new MolExporter(output, options);
        final AtomicInteger i = new AtomicInteger(0);
        Stream<MoleculeObject> stream = mols.peek(mo -> {
            if (mo != null) {
                try {
                    i.incrementAndGet();
                    exporter.write(mo.getMol());
                } catch (IOException e) {
                    DMLOG.logEvent(DMLogger.Level.WARNING, "Failed to export molecule " + i);
                }
            }
        }).onClose(() -> {
            try {
                exporter.close();
            } catch (IOException e) {
                DMLOG.logEvent(DMLogger.Level.WARNING, "Failed to close MolExporter " + e.getMessage());
            }
        });

        return stream;
    }
}
